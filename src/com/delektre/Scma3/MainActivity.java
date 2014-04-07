package com.delektre.Scma3;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.code.microlog4android.Level;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import jjil.android.CrosshairOverlay;
import jjil.android.Preview;

import java.io.File;
import java.io.IOException;

import static com.delektre.Scma3.R.id;
import static com.delektre.Scma3.R.layout;


public class MainActivity extends Activity {

    private int mNumberOfCameras = -1, mCameraCurrentlyLocked = -1;
    Camera mCamera;
    int mDefaultCameraId = -1;

    ScmSeqCreator mSeqHandler;
    private Preview mPreview;
    private int nCameras;
    private ProgressBar mProgress;

    private Button clickButton, zoomButton;

    protected static final Logger logger = LoggerFactory.getLogger();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide titlebar and set Fullscreen mode
        setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // set main layout
        setContentView(layout.main);


        initUI();
        testSystem();
        initCamera();
    }

    private void initCamera() {
        mNumberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mDefaultCameraId = i;
            }
        }

        if (mDefaultCameraId == -1) {
            // don't we have any camera?
            if (nCameras > 0) {
                mDefaultCameraId = 0;
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.err_no_cameras,
                        Toast.LENGTH_LONG);
                toast.show();
                finish();
            }
        }
        mCameraCurrentlyLocked = mDefaultCameraId;
    }


    /**
     * Called when application is suspended, ie another Intent takes over or such
     */
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    protected void onResume() {
        super.onResume();
        if ( mCameraCurrentlyLocked != 1 ) {
            try {
                mCamera = Camera.open(mCameraCurrentlyLocked);
                mPreview.setCamera(mCamera);

                // TODO: kirjoita callback
                mCamera.setPreviewCallback(mSeqHandler);

                String focusMode = mCamera.getParameters().getFocusMode();
                if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
                        || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                    //original code from the book
//            mCamera.autoFocus(mReadBarcode);
                    //delayed focusing works on more devices
                    mSeqHandler.autoFocusLater(mCamera);
                } else {
                }
            } catch (RuntimeException re) {
                logger.error(re.getMessage());
            }
        }
    }

    private void initUI() {
        mProgress = (ProgressBar) findViewById(R.id.ui_main_progress);
        mSeqHandler = new ScmSeqCreator(mProgress);

        clickButton = (Button) findViewById(id.ui_main_click_button);
        zoomButton = (Button) findViewById(id.ui_main_focus_button);

        CrosshairOverlay co = (CrosshairOverlay) findViewById(id.crosshairoverlay1);

        nCameras = Camera.getNumberOfCameras();
        mPreview = (Preview) findViewById(id.ui_main_preview);

        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgress.setProgress(0);
                File tmp = new File(Environment.getExternalStorageDirectory(), "/SCM");
                for (int i = 0; i < 6; i++) {
                    mProgress.setProgress(i * 12);
                    mPreview.savePicture(saveDir +  "test_" + i);
                }
            }
        });
        zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logger.debug("preview.doFocus()");
                mCamera.autoFocus(null);
            }
        });
    }

    private String saveDir;

    private void testSystem() {
        try {
            File tmp = new File(Environment.getExternalStorageDirectory(), "/SCM");
            if (!tmp.exists()) {
                tmp.mkdir();
                saveDir = tmp.getCanonicalPath();
                logger.info("Creating missing data directory: " + tmp.getCanonicalPath());
            }
            saveDir = tmp.getCanonicalPath() + File.pathSeparator;
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        String tmp = "";
        try {
            tmp = Environment.getExternalStorageDirectory().getCanonicalPath();
        } catch (Exception e) {

        }
        String mStorageDir;
        try {

            //Level loglevel = Level.valueOf(sharedPref.getString(KEY_PREF_LOGLEVEL, "DEBUG"));
            Level loglevel = Level.valueOf("DEBUG");

            logger.setLevel(loglevel);
            logger.debug("Set loglevel to: " + logger.getLevel().toString());
        } catch (Exception e) {
            logger.error("Exception while setting loglevel: " + e.toString());

        }
        try {
            mStorageDir = new File(tmp + "/SCM").getCanonicalPath();

        } catch (IOException e1) {
            String msg = "";
            if (e1.getMessage() != null) {
                msg = e1.getMessage();
            }
            logger.error(e1.toString() + " : " + msg);
            //logger.error(e1.toString(), msg);
        } catch (Exception e) {
            String msg = "";
            if (e.getMessage() != null) {
                msg = e.getMessage();
            }
            logger.error(e.toString());
            logger.error(msg);
        }

        try {
            File saveDir = new File(mStorageDir);
            if (!saveDir.isDirectory()) {
                saveDir.mkdir();
            }
        } catch (Exception e) {
            logger.error("Error in creation of StorageDIR: " + e.toString());
            //Log.d(TAG, "Error in creation of savedir");
        }
        */

    }
}
