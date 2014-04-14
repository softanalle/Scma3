package com.delektre.Scma3;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.config.PropertyConfigurator;
import jjil.android.CrosshairOverlay;
import jjil.android.Preview;

import static com.delektre.Scma3.R.id;
import static com.delektre.Scma3.R.layout;


public class MainActivity extends Activity {
    protected static final Logger logger = LoggerFactory.getLogger();

    private int mNumberOfCameras = -1, mCameraCurrentlyLocked = -1;
    Camera mCamera;
    int mDefaultCameraId = -1;

    ScmSeqCreator mSeqHandler;
    private Preview mPreview;
    private int nCameras;
    private ProgressBar mProgress;

    private boolean stillLoading = true;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        setContentView(layout.main);

        PropertyConfigurator.getConfigurator(this).configure();

        mProgress = (ProgressBar) findViewById(R.id.ui_main_progress);
        mSeqHandler = new ScmSeqCreator(mProgress);

        CrosshairOverlay co = (CrosshairOverlay) findViewById(id.crosshairoverlay1);

        if (checkCameraHardware(getApplicationContext())) {
            logger.info("Ok, we have a camera");
        } else {
            logger.error("onCreate: unable to find camera");
            Toast.makeText(getApplicationContext(), "Camera is missing!", Toast.LENGTH_LONG).show();
        }

        nCameras = Camera.getNumberOfCameras();
        mPreview = (Preview) findViewById(id.ui_main_preview);

        initCamera();
        stillLoading = false;
    }

    /**
     * Check if this device has a camera
     *
     * @param context The application context to be used
     */
    private boolean checkCameraHardware(Context context) {
        logger.debug("checkCameraHardware()");
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    private void initCamera() {
        logger.debug("initCamera()");
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
        logger.debug("onPause()");
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }

    }

    protected void onResume() {
        super.onResume();
        if ( ! stillLoading) {
            logger.debug("onResume()");
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
            getSettings();
        }
    }

    private void getSettings() {
        logger.debug("getSettings()");
        String dump = "before";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            //saveModeJPEG = sharedPref.getBoolean(KEY_PREF_SAVE_JPEG, true);
            //saveModeRAW = sharedPref.getBoolean(KEY_PREF_SAVE_RAW, false);
/*
            dump = "green";
            mPulseWidth[LED_INDEX_GREEN] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_GREEN_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_GREEN])));
            dump = "blue";
            mPulseWidth[LED_INDEX_BLUE] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_BLUE_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_BLUE])));
            dump = "red";
            mPulseWidth[LED_INDEX_RED] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_RED_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_RED])));
            dump = "yellow";
            mPulseWidth[LED_INDEX_YELLOW] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_YELLOW_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_YELLOW])));
            dump = "white";
            mPulseWidth[LED_INDEX_WHITE] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_WHITE_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_WHITE])));
            dump = "nir";
            mPulseWidth[LED_INDEX_NIR] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_NIR_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_NIR])));
  */
            dump = "focusColor";
    /*
            mFocusLedIndex = Integer.parseInt(sharedPreferences.getString(KEY_PREF_FOCUSCOLOR, "3"));
      */
        } catch (ClassCastException cce) {
            Toast.makeText(getApplicationContext(), "Invalid setting for '" + dump + "': " + cce, Toast.LENGTH_LONG).show();
            logger.error("Invalid setting for '" + dump + "': " + cce);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exeption: " + e, Toast.LENGTH_LONG).show();
            logger.error("Exeption: " + e);
        }

    }

}
