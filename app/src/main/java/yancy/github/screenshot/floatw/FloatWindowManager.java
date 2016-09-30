package yancy.github.screenshot.floatw;


import android.content.Context;

public class FloatWindowManager {
    private Context mContext;
    private FloatWindow mFloatW;

    public FloatWindowManager(Context context) {
        mContext = context;
        mFloatW = new FloatWindow(context);
    }

    public void showFloatWindow() {
        mFloatW.show();
    }

    public void hideFloatWindow() {
        mFloatW.hide();
    }

    public void switchFloatWindow() {

    }
}
