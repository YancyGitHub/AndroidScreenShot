package yancy.github.screenshot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import yancy.github.screenshot.floatw.FloatWindowManager;
import yancy.github.screenshot.shot.ScreenShot;

public class MainActivity extends AppCompatActivity {

    private FloatWindowManager mFwm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void handleButtonClick(View view) {
        startService(new Intent(this, MainService.class));
        finish();
    }

}
