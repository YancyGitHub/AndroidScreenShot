package yancy.github.screenshot.shot;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import yancy.github.screenshot.R;

public class ScreenShot {

    private static final int EVT_SCREEN_SHOT_OVER = 0;
    private static final int EVT_WAIT_SCREEN_DATA = 1;

    private static ScreenShot mInstance;
    private Context mContext;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;

    private int mResultCode;
    private Intent mData;

    private OnCaptureOverListener mListener;

    public interface OnCaptureOverListener {
        public void onCaptureOver();
    };

    public static ScreenShot getInstance(Context ctx) {
        if(mInstance == null) {
            mInstance = new ScreenShot(ctx);
        }
        return mInstance;
    }

    private ScreenShot(Context ctx) {
        mContext = ctx;
        mMediaProjectionManager = (MediaProjectionManager)mContext
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void screenCapture() {
        Intent intent = new Intent(mContext, ScreenShotActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void registerCaptureOverListener(OnCaptureOverListener l) {
        mListener = l;
    }

    @SuppressLint("HandlerLeak")
    private Handler mInternalHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what)
            {
                case EVT_SCREEN_SHOT_OVER:
                    if(mListener != null) {
                        mListener.onCaptureOver();
                    }
                    //mInstance = null;
                    break;
                case EVT_WAIT_SCREEN_DATA:
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getScreenData(mResultCode, mData);
                        }
                    }, 100);
                    break;
            }
        }
    };

    protected void handleResult(final int resultCode, final Intent data) {
        mResultCode = resultCode;
        mData = data;
        mInternalHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                getScreenData(mResultCode, mData);
            }
        }, 150);
    }

    private void getScreenData(int resultCode, Intent data) {
        if(mMediaProjection == null) {
            mInternalHandler.sendEmptyMessage(EVT_WAIT_SCREEN_DATA);
            return;
        }

        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        final int width = dm.widthPixels;
        final int height = dm.heightPixels;

        final ImageReader imageReader = ImageReader.newInstance(width, height,
                PixelFormat.RGBA_8888, 2);
        final VirtualDisplay display = mMediaProjection.createVirtualDisplay("screen-shot", width,
                height,
                DisplayMetrics.DENSITY_MEDIUM, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                imageReader.getSurface(), null, null);

        HandlerThread handlerThread = new HandlerThread("screen-shot");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                image = reader.acquireLatestImage();
                if (image != null) {
                    final Image.Plane[] planes = image.getPlanes();
                    if (planes.length > 0) {
                        final ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;
                        Bitmap bmp = Bitmap.createBitmap(width + rowPadding / pixelStride,
                                height, Bitmap.Config.ARGB_8888);
                        bmp.copyPixelsFromBuffer(buffer);

                        Bitmap croppedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height);
                        saveBitmap(croppedBitmap);

                        if (croppedBitmap != null) {
                            croppedBitmap.recycle();
                        }
                        if (bmp != null) {
                            bmp.recycle();
                        }
                    }
                }
                if (image != null) {
                    image.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (display != null) {
                    display.release();
                }

                reader.setOnImageAvailableListener(null, null);
                mMediaProjection.stop();
            }// end of onImageAvailable
        }, null);
    }

    static File getOutputFoler() {
        File folder = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Screenshots");
        if (folder.exists() == false || folder.isDirectory() == false) {
            folder.mkdirs();
        }
        return folder;
    }

    static String getShoutFileName() {
        Date d = new Date(System.currentTimeMillis());
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        return "ScreenShot-" + sd.format(d) + ".jpg";
    }

    private void saveBitmap(Bitmap bmp) {
        File childFolder = getOutputFoler();
        File imageFile = new File(childFolder.getAbsolutePath() + "/" + getShoutFileName());

        OutputStream fOut = null;
        try {
            fOut = new FileOutputStream(imageFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fOut != null) {
                try {
                    fOut.close();
                } catch (IOException e) {}
            }
        }
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                .fromFile(imageFile)));

        showNotification(imageFile);
    }

    private void showNotification(File file) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

        Bitmap screenshot = BitmapFactory.decodeFile(file.getAbsolutePath());
        int imageWidth = screenshot.getWidth();
        int imageHeight = screenshot.getHeight();
        final int shortSide = imageWidth < imageHeight ? imageWidth : imageHeight;

        Bitmap preview = Bitmap.createBitmap(shortSide, shortSide,
                screenshot.getConfig() == null ? Bitmap.Config.ARGB_8888 : screenshot.getConfig());
        Canvas c = new Canvas(preview);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        Matrix matrix = new Matrix();
        matrix.postTranslate((shortSide - imageWidth) / 2, (shortSide - imageHeight) / 2);
        c.drawBitmap(screenshot, matrix, paint);
        c.drawColor(0x40FFFFFF);
        builder.setPriority(Notification.PRIORITY_DEFAULT);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType("image/*");
        PendingIntent sharePendingIntent = PendingIntent.getActivity(mContext, 0, shareIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentTitle(file.getName())
                .setContentText(file.getAbsolutePath())
                .setContentIntent(sharePendingIntent)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(preview))
                .setAutoCancel(true)
                .setSound(
                        Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.screenshot))
                .setSmallIcon(android.R.drawable.ic_menu_gallery);

        Notification notification = builder.build();
        NotificationManager nm = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, notification);

        mInternalHandler.sendEmptyMessageDelayed(EVT_SCREEN_SHOT_OVER, 500);
    }
}
