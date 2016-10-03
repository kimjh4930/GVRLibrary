package com.test.gvrtest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.Toast;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

import java.io.IOException;

public class MainActivity extends GvrActivity {

    final String TAG = "MainActivity";
    private GvrView.StereoRenderer mRenderer;

    private int mScreenDentisy;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;

    Button recordBtn;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int REQUEST_PERMISSIONS = 10;
    private static final int REQUEST_CODE = 1000;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordBtn = (Button) findViewById(R.id.recordbtn);
        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);

        //Screen Record 부분
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDentisy = metrics.densityDpi;
        mMediaRecorder = new MediaRecorder();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        //mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        // VR 부분
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        //권한이 없는 경우
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //최초 거부를 선택하면 두번째부터 이벤트 발생 & 권한 획득이 필요한 이유유를 설명
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }

            //요청 팝업 팝업 선택시 onRequestPermissionsResult 이동
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);

        }
        //권한이 있는 경우
        else {
            //불러올 파일 주소
            initGvrView(gvrView);
        }

        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN :

                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {

                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {

                                /*Snackbar.make(findViewById(android.R.id.content),
                                        R.string.label_permissions,
                                        Snackbar.LENGTH_INDEFINITE).setAction("Enable", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
                                    }
                                }).show();*/
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                        REQUEST_PERMISSIONS);
                            }
                        } else {
                            onScreenShare(view);
                        }

                        break;
                    case MotionEvent.ACTION_MOVE :
                        break;
                    case MotionEvent.ACTION_UP :

                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {

                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {

                                /*Snackbar.make(findViewById(android.R.id.content),
                                        R.string.label_permissions,
                                        Snackbar.LENGTH_INDEFINITE).setAction("Enable", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
                                    }
                                }).show();*/
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                        REQUEST_PERMISSIONS);
                            }
                        } else {
                            offScreenShare(view);
                        }

                        break;

                    default:
                        break;
                }
                return false;
            }
        });


    }

    public void initGvrView(GvrView gvrView){
        String address = "/storage/emulated/0/DCIM/friendsCameraSample/20160820_173510.mp4";
        Uri uri = Uri.parse(address);
        String type = "video";

        if (type.contains("image")) {
            type = "image";
            //mRenderer = new ImageRenderer(this, uri);
        } else if (type.contains("video")) {
            type = "video";
            mRenderer = new VideoRenderer(this, uri);
        } else {
            finish();
        }

        gvrView.setRenderer(mRenderer);
        gvrView.setTransitionViewEnabled(false);

            /*
            * true : VR mode, false : not VR mode
            */
        gvrView.setStereoModeEnabled(false);

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        setGvrView(gvrView);
    }

    public void playGvr(GvrView view){
        view.onResume();
    }

    public void pauseGvr(GvrView view){
        view.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.i(TAG, "Unknown request code : " + requestCode);
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();

    }

    public void onScreenShare(View view) {
        initRecorder();
        shareScreen();
    }

    public void offScreenShare(View view) {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            } catch (RuntimeException e) {

            }
        }
        mMediaRecorder = null;
        stopScreenSharing();
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {

        if (mMediaRecorder.getSurface() == null) {
            Log.i(TAG, "getSurface() is null");
        }

        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDentisy, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);

        //마지막에서 바로 앞 null : virtualdisplay.callback
        //VirtualDisplay.callback
        //onPaused(), 시스템이나 surface가 detached 되었을 경우 호출
        //onResumed(), 시작되었을 경우
        //onStopped(), 시스템에서 정지되었을 경우(완전 종료에 해당)
        //마지막 null : handler handler;
    }

    private void initRecorder() {
        try {

            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
            }

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video.mp4");

            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);

            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {

            if (mMediaRecorder != null) {
                try {
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {

                }
                mMediaRecorder.reset();
            }
            mMediaProjection = null;

            stopScreenSharing();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }

        mVirtualDisplay.release();

        destroyMediaProjection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        Log.i(TAG, "MediaProjection Stopped");
    }

}

