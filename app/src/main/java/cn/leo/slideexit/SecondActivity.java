package cn.leo.slideexit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

public class SecondActivity extends Activity {

    private ViewPager mVp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //SlideExit.bind(this, SlideExit.SLIDE_RIGHT_EXIT);
        init();
    }

    private void init() {
        mVp = (ViewPager) findViewById(R.id.vp);
        mVp.setAdapter(new MyVpAdapter());
        findViewById(R.id.tvTitle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SecondActivity.this, LoginActivity.class));
            }
        });

    }

}
