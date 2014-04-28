package com.delektre.Scma3;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.config.PropertyConfigurator;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
// import jjil.android.CrosshairOverlay;
import com.delektre.Scma3.Preview;
import org.androidannotations.annotations.*;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.delektre.Scma3.R.id;
import static com.delektre.Scma3.R.layout;

@WindowFeature({Window.FEATURE_NO_TITLE, Window.FEATURE_INDETERMINATE_PROGRESS})
@Fullscreen
@EActivity(layout.main)
@OptionsMenu(R.menu.options_menu)
public class MainActivity extends Activity {

    @OptionsItem
    void optMenuResetSelected() {
        scmaPrefs.clear();
    }

    @OptionsItem
    void optMenuQuitSelected() {
        super.onStop();
        System.exit(0);
    }

    @Pref
    ScmaPrefs_ scmaPrefs;

    protected static final Logger logger = LoggerFactory.getLogger();

    private int mNumberOfCameras = -1, mCameraCurrentlyLocked = -1;
    Camera mCamera = null;
    int mDefaultCameraId = -1;

    @ViewById
    Preview uiMainPreview;

    @ViewById
    ProgressBar uiMainProgress;

    @ViewById
    CrosshairOverlay uiCrossHairOverlay; //  = (CrosshairOverlay) findViewById(id.crosshairoverlay1);


    private int nCameras;

    private boolean stillLoading = true;

    private final int mLedCount = 6;
    private long mWaitAfterDelay = 500;
    private boolean[] mLedState;
    private int mProgressValue;

    private String TAG = "MainActivity";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        setContentView(layout.main);

        PropertyConfigurator.getConfigurator(this).configure();


        // variable initializations
        mLedState = new boolean[mLedCount];

        updateProgress(0);
    }


    @AfterViews
    void initApplication() {
        updateProgress(10);
        testSystem();
        updateProgress(20);
        initUI();
        updateProgress(40);
        initHardware();
        updateProgress(60);
        initCamera();
        updateProgress(90);
        stillLoading = false;
        updateProgress(100);
    }

    /**
     * Check if this device has a camera
     *
     * @param context The application context to be used
     */

    private boolean checkCameraHardware(Context context) {
        logger.debug(TAG + ".checkCameraHardware()");
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    void initCamera() {
        logger.debug(TAG + ".initCamera()");

        logger.debug("  mCameraCurrentlyLocked=" + mCameraCurrentlyLocked);
        if (mCamera != null) {
            mCamera.release();
        }
//        if (mCameraCurrentlyLocked == -1) {
        mCamera = null;

        if (checkCameraHardware(getApplicationContext())) {
            logger.info("  Ok, we can use camera");
        } else {
            logger.error(TAG + ".onCreate: unable to find camera feature");
            Toast.makeText(getApplicationContext(), "Camera is missing!", Toast.LENGTH_LONG).show();
        }

        mNumberOfCameras = Camera.getNumberOfCameras();

        logger.info("  Found " + mNumberOfCameras + " cameras");

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int i = 0; i < mNumberOfCameras; i++) {
            logger.info(TAG + " checking camera #" + i);
            Camera.getCameraInfo(i, cameraInfo);
            logger.debug("  camerainfo = " + cameraInfo.facing + "; back=" + Camera.CameraInfo.CAMERA_FACING_BACK + "; front=" + Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (cameraInfo.facing == scmaPrefs.cameraToUse().get()) {
                logger.info("  Accepting camera #" + i);
                mDefaultCameraId = i;
            }
        }

        if (mDefaultCameraId == -1) {
            logger.info("  mDefaultCameraId is still -1");
            // don't we have any camera?
            if (mNumberOfCameras > 0) {
                logger.debug("  Set failback to camera #0");
                mDefaultCameraId = 0;
                mCameraCurrentlyLocked = 0;
                mCamera = Camera.open(0);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.err_no_cameras,
                        Toast.LENGTH_LONG);
                toast.show();
                finish();
            }
        } else {
            logger.info("  Selecting camera #" + mDefaultCameraId);
            mCameraCurrentlyLocked = mDefaultCameraId;
            mCamera = Camera.open(mCameraCurrentlyLocked);

        }
        //      }
        try {
            uiMainPreview.switchCamera(mCamera);
            //mCamera.startPreview();
/*
            Camera.Parameters camParams;
            camParams = mCamera.getParameters();


            camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            camParams.set("rawsave-mode", "1");
            // parameters.set("rawfname", filename + ".raw");
            mCamera.setParameters(camParams);
            */
        } catch (Exception e) {
            logger.error(TAG + ".initCamera() - finetuning camera settings: " + e.toString());
        }

        try {
            String focusMode = mCamera.getParameters().getFocusMode();
            logger.debug(TAG + ".initCamera() - focus mode set to: " + focusMode);
        } catch (Exception e) {
            logger.error("initCamera(): " + e.toString());
        }
    }


    /**
     * Called when application is suspended, ie another Intent takes over or such
     */

    protected void onPause() {
        super.onPause();
        logger.debug(TAG + ".onPause()");
        if (mCamera != null) {
            //uiMainPreview.setCamera(null);
            //mCamera.release();
            uiMainPreview.releaseCamera();
            // mSeqHandler = null;
            mCamera = null;
            mCameraCurrentlyLocked = -1;

            stillLoading = true;
        }

    }


    protected void onResume() {
        super.onResume();
        logger.debug(TAG + ".onResume()");
        //if (!stillLoading) {

        try {
            initCamera();
            //uiMainPreview.reclaimCamera();
            mCamera = uiMainPreview.getCamera();

            // mCamera = Camera.open(mCameraCurrentlyLocked);
            // mCamera = uiMainPreview.getCameraInstance();
            // uiMainPreview.setCamera(mCamera);
        } catch (Exception e) {
            logger.error(TAG + ".onResume() stage 1: " + e.toString());
        }

        if (mCamera == null) {
            logger.error(TAG + ".onResume() stage 1.5, CAMERA is NULL");
        }

  /*      try {

            String focusMode = mCamera.getParameters().getFocusMode();
            logger.debug("  focusMode = " + focusMode);

            if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
                    || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                logger.debug("  Enabling FOCUS_MODE_AUTO or FOCUS_MODE_MACRO for camera");

                //original code from the book
                //            mCamera.autoFocus(mReadBarcode);
                //delayed focusing works on more devices
                //mSeqHandler.autoFocusLater(mCamera);
            } else if (focusMode.equals(Camera.Parameters.FOCUS_MODE_FIXED)) {
                logger.debug("  Camera has FIXED focus mode, not setting autoFocus");
            } else {
                logger.debug("  Camera does not support FOCUS_MODE_AUTO or FOCUS_MODE_MACRO");
            }
            */
/*
        } catch (RuntimeException re) {
            logger.error(TAG + ".onResume() - stage 2: we got RuntimeException: " + re.toString());
        } catch (Exception e) {
            logger.error(TAG + ".onResume() - stage 3: we got Exceiption: " + e.toString());
        }
*/
        try {
            // TODO: kirjoita callback
            /*
            mSeqHandler = new ScmSeqCreator(getApplicationContext());

            mSeqHandler.setPreview(uiMainPreview);

            mCamera.setPreviewCallback(mSeqHandler);
            */
            // mCamera.startPreview();
            mCamera.setPreviewDisplay(uiMainPreview.getSurfaceHolder());
            uiMainPreview.startPreview();
        } catch (Exception e) {
            logger.error(TAG + " mSeqHandler state got exception: " + e.toString());
        }

        try {
            uiMainPreview.startPreview();
        } catch (Exception e) {
            logger.debug(TAG + ".onResume() - some error when trying to start preview");
        }

    }


    void initUI() {

        // mSeqHandler = new ScmSeqCreator(getApplicationContext());
        // mSeqHandler.setPreview( uiMainPreview );

    }

    @UiThread
    void beep() {
        logger.debug(TAG + ".beep() called");
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Click
    void uiMainButtonFocus() {
        beep();
        uiMainProgress.setProgress(0);
        logger.debug(TAG + ".uiMainButtonFocus()");
        doFocus();
    }

    @Background
    void doFocus() {
        logger.debug(TAG + ".doFocus()");
        try {
            mService.setLedMode(6, true);
            Thread.sleep(200);
            if (mCamera != null)
                mCamera.autoFocus(null);
            Thread.sleep(200);
            mService.setLedMode(6, false);
        } catch (InterruptedException ie) {
            logger.error(TAG + ".doFocus(ie): " + ie.toString());
        } catch (Exception e) {
            logger.error(TAG + ".doFocus(e): " + e.toString());
        }
    }

    @Click
    void uiMainButtonPicture() {
        logger.debug(TAG + ".uiMainButtonPicture()");
        uiMainProgress.setProgress(0);
        if (mBound) {
            takeColorSeries();
        } else {
            Toast.makeText(getApplicationContext(), "IOIO not ready", Toast.LENGTH_LONG).show();
        }
    }


    // @Background(serial = "picture", delay = 200)
    void takePictureWithLed(int led_, String namePrefix) {
        logger.debug(TAG + ".takePictureWithLed(" + led_ + ", " + namePrefix + ")");

        if (uiMainPreview == null) {
            logger.error("  Preview object is null");
        }

        try {
            // mLedState[led_] = true;
            mService.setLedMode(led_, true);
            Thread.sleep(200);
            uiMainPreview.savePicture(saveDir.toString() + namePrefix);
            Thread.sleep(750);
            mCamera.startPreview();
            //mLedState[led_] = false;
            mService.setLedMode(led_, true);

        } catch (NullPointerException npe) {
            logger.error("  Got: " + npe.toString());
        } catch (Exception e) {
            logger.error("  Got: " + e.toString());
        }
        logger.debug(TAG + ".takePictureWithLed() completed");
    }


    @UiThread
    void updateProgress(int mProgressValue) {
        logger.debug(TAG + ".updateProgress(" + mProgressValue + ")");
        uiMainProgress.setProgress(mProgressValue);
    }

    @UiThread
    void takeColorSeries() {
        logger.debug(TAG + ".takeColorSeries() start");

        logger.debug("  storing images to: " + saveDir.toString());

        int i = -1;


        for (i = 0; i < 6; i++) {
            takePictureWithLed(i, "test_" + i);
            updateProgress(i * 16);
            beep();
        }

        // initCamera();
        mCamera.startPreview();

        logger.debug(TAG + ".takeColorSeries() completed");
    }

    private String saveDir = null;


    void testSystem() {
        try {
            File tmp = new File(Environment.getExternalStorageDirectory(), File.separator + "SCM");
            if (!tmp.exists()) {
                tmp.mkdir();
                saveDir = tmp.getCanonicalPath();
                logger.info("  Creating missing data directory: " + tmp.getCanonicalPath());
            }
            saveDir = tmp.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (saveDir == null) {
            logger.error("ERROR IN Constructing storage dir");
        }

        // Reading a value and providing a fallback default value
        long now = System.currentTimeMillis();
        long lastUpdated = scmaPrefs.lastUpdated().getOr(now);

    }


    IOIOPalvelu mService;
    boolean mBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, IOIOPalvelu_.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            IOIOPalvelu.LocalBinder binder = (IOIOPalvelu.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    void initHardware() {
        // init the IOIO
        Intent intent = new Intent(this, IOIOPalvelu_.class);
        startService(intent);
    }

}
