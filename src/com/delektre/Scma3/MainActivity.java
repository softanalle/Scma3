package com.delektre.Scma3;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;
import jjil.android.CrosshairOverlay;
import jjil.android.Preview;

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

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.main);

        mProgress = (ProgressBar) findViewById(R.id.ui_main_progress);
        mSeqHandler = new ScmSeqCreator(mProgress);

        CrosshairOverlay co = (CrosshairOverlay) findViewById(id.crosshairoverlay1);

        nCameras = Camera.getNumberOfCameras();
        mPreview = (Preview) findViewById(id.ui_main_preview);

    }

    private void initCamera() {
        mNumberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i=0; i < mNumberOfCameras; i++)
        {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mDefaultCameraId = i;
            }
        }

        if (mDefaultCameraId == -1)
        {
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
        mCamera = Camera.open(mCameraCurrentlyLocked);
        mPreview.setCamera( mCamera );

        // TODO: kirjoita callback
        mCamera.setPreviewCallback(mSeqHandler);

        String focusMode = mCamera.getParameters().getFocusMode();
        if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
                || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO))
        {
            //original code from the book
//            mCamera.autoFocus(mReadBarcode);
            //delayed focusing works on more devices
            mSeqHandler.autoFocusLater(mCamera);
        }
        else
        {
        }

    }

}
