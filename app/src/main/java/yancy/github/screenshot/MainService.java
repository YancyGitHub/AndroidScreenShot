package yancy.github.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import yancy.github.screenshot.floatw.FloatWindowManager;

public class MainService extends Service {

    private FloatWindowManager mFwm;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFwm = new FloatWindowManager(this);
        mFwm.showFloatWindow();
    }
}
