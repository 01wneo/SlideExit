package cn.leo.slideexit;

import android.animation.IntEvaluator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import java.util.LinkedList;

/**
 * @author : Jarry Leo
 * @date : 2019/2/13 9:51
 */
public class SwipeBack extends FrameLayout implements Application.ActivityLifecycleCallbacks {
    private LinkedList<Activity> mActivities = new LinkedList<>();
    /**
     * 当前展示的activity的内容页面
     */
    private View mContentView;
    /**
     * 底下activity的view作为当前activity的背景
     */
    private View mBackView;
    private ViewDragHelper mDragHelper;
    private Paint mPaint;
    private IntEvaluator mEvaluator;

    private SwipeBack(@NonNull Context context) {
        this(context, null);
    }

    private SwipeBack(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private SwipeBack(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mPaint = new Paint();
        mEvaluator = new IntEvaluator();
        mDragHelper = ViewDragHelper.create(this, mCallback);
    }


    public static void init(Application application) {
        SwipeBack swipeBack = new SwipeBack(application);
        application.registerActivityLifecycleCallbacks(swipeBack);
    }

    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContentView && mActivities.size() > 1;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            //滑动边界1/4即关闭Activity
            float xDistance = getMeasuredWidth() / 3;
            //左右超过边界处理
            if (mContentView.getLeft() != 0) {
                if (mContentView.getLeft() < -xDistance) {
                    mDragHelper.smoothSlideViewTo(mContentView, -getMeasuredWidth(), 0);
                } else if (mContentView.getLeft() > xDistance) {
                    mDragHelper.smoothSlideViewTo(mContentView, getMeasuredWidth(), 0);
                } else {
                    //未超过则回弹
                    mDragHelper.smoothSlideViewTo(mContentView, 0, 0);
                }
            }

            //刷新动画
            ViewCompat.postInvalidateOnAnimation(SwipeBack.this);
        }


        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (left <= 0) {
                return 0;
            }
            return left;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            //绘制阴影刷新显示
            ViewCompat.postInvalidateOnAnimation(SwipeBack.this);
        }


        @Override
        public int getViewHorizontalDragRange(View child) {
            return 100;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return 100;
        }
    };

    @Override
    public void computeScroll() {
        //滑动动画处理
        if (mDragHelper.continueSettling(true)) {
            //刷新显示
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            //滑动结束,关闭Activity
            if (Math.abs(mContentView.getLeft()) == getMeasuredWidth()) {
                mActivities.getLast().finish();
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //交给ViewDragHelper处理拦截事件
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //交给ViewDragHelper处理滑动事件
        mDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mContentView.getLeft() > 0) {
            Integer evaluate = mEvaluator.evaluate(mContentView.getLeft() * 1.0f / mContentView.getMeasuredWidth(),
                    0, 100);
            mPaint.setColor(Color.argb(100 - evaluate, 0, 0, 0));
            //左边阴影
            canvas.drawRect(0, 0, mContentView.getLeft(),
                    getMeasuredHeight(), mPaint);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        mActivities.addLast(activity);
        getViewToNewActivity();
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        boolean isTop = activity == mActivities.getLast();
        mActivities.remove(activity);
        if (isTop) {
            //从销毁的页面移除自身
            mContentView = null;
            ViewGroup decorView = getDecorView(activity);
            decorView.removeAllViews();
            //处理底下漏出的新页面
            resetViewToSecondActivity();
        }
    }

    /**
     * 把底部的页面还原回去
     * 并且把它下面的view拿上来
     */
    private void resetViewToSecondActivity() {
        this.removeAllViews();
        Activity lastActivity = mActivities.getLast();
        ViewGroup decorView = getDecorView(lastActivity);
        if (mActivities.size() < 2) {
            //如果底下只剩一个页面，则把它之前的页面还给它
            decorView.addView(mBackView);
            return;
        }
        //如果底下有多个页面则把倒数第二个页面添加到它的背景
        Activity secondLastActivity = mActivities.get(mActivities.size() - 2);
        mContentView = mBackView;
        mBackView = getContentView(secondLastActivity);
        this.addView(mBackView);
        this.addView(mContentView);
        //把本容器添加到Activity的父容器
        decorView.addView(this);
    }

    /**
     * 把底部页面的布局添加到当前展示的activity的底下；
     */
    private void getViewToNewActivity() {
        if (mActivities.size() < 2) {
            return;
        }
        Activity lastActivity = mActivities.getLast();
        Activity secondLastActivity = mActivities.get(mActivities.size() - 2);
        ViewGroup decorView = getDecorView(lastActivity);
        if (decorView.getChildCount() > 0) {
            //拿到Activity的contentView
            mContentView = getContentView(lastActivity);
            mBackView = getContentView(secondLastActivity);
            //把contentView添加到本容器
            this.removeAllViews();
            this.addView(mBackView);
            this.addView(mContentView);
            //把本容器添加到Activity的父容器
            decorView.addView(this);
        }
    }

    private ViewGroup getDecorView(Activity activity) {
        Window window = activity.getWindow();
        return (ViewGroup) window.getDecorView();
    }

    private View getContentView(Activity activity) {
        ViewGroup decorView = getDecorView(activity);
        View contentView = decorView.getChildAt(0);
        decorView.removeAllViews();
        return contentView;
    }
}
