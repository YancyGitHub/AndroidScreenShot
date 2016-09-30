package yancy.github.screenshot.floatw;


import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import yancy.github.screenshot.MainService;
import yancy.github.screenshot.R;
import yancy.github.screenshot.shot.ScreenShot;

public class FloatWindow {
    private Context mContext;
    private WindowManager mWm;
    private View mFloatView;

    public FloatWindow(Context ctx) {
        mContext = ctx;
        mWm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);

        mFloatView = LayoutInflater.from(ctx).inflate(R.layout.float_window, null);

        ScreenShot.getInstance(mContext).registerCaptureOverListener(
                new ScreenShot.OnCaptureOverListener() {
            @Override
            public void onCaptureOver() {
                show();
            }
        });

        mFloatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScreenShot.getInstance(mContext).screenCapture();
                hide();
            }
        });
        mFloatView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                hide();
                mContext.stopService(new Intent(mContext, MainService.class));
                return true;
            }
        });

        create();
    }

    private void create() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        lp.format = PixelFormat.RGBA_8888;

        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        DisplayMetrics dm = new DisplayMetrics();
        mWm.getDefaultDisplay().getMetrics(dm);

        lp.width = dm.widthPixels / 7;
        lp.height = dm.widthPixels / 7;
        lp.gravity = Gravity.CENTER;

        mWm.addView(mFloatView, lp);

        hide();
    }

    protected void hide() {
        mFloatView.setVisibility(View.GONE);
    }

    protected void show() {
        mFloatView.setVisibility(View.VISIBLE);
    }
}
