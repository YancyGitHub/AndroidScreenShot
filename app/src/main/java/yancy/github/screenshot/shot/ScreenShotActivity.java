package yancy.github.screenshot.shot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

public class ScreenShotActivity extends Activity {
    private final int PERMISSION_CODE_MEDIA_PROJECTION = 100;
    private int mResultCode;
    private Intent mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenCapture();
    }

    public void screenCapture() {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, PERMISSION_CODE_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSION_CODE_MEDIA_PROJECTION:
                if (resultCode == RESULT_OK && data != null) {
                    mResultCode = resultCode;
                    mData = data;
                }
                break;
        }
        finish();
    }//end onActivityResult

    @Override
    protected void onDestroy() {
        ScreenShot.getInstance(this).handleResult(mResultCode, mData);
        super.onDestroy();
    }
}
