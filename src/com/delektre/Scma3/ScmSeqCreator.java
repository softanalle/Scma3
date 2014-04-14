package com.delektre.Scma3;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import jjil.android.CrosshairOverlay;
import jjil.android.Preview;

/**
 * Created by t2r on 4/6/14.
 */
public class ScmSeqCreator implements android.hardware.Camera.PreviewCallback,
        android.hardware.Camera.AutoFocusCallback{

    private static final String TAG = "ScmSeqCreator";

    private ProgressBar mProgress;
    private Handler handler = new Handler();
    private Preview mPreview;

    public ScmSeqCreator(Preview preview) { //double dPerpPos, TextView tv, CheckBox ck, CrosshairOverlay co) {
        /*
        me13b = new Ean13Barcode1D();
        mdBarcodePerpPos = dPerpPos;
        mTextViewResult = tv;
        mCheckBoxResult = ck;
        mCrosshairOverlay = co;
        */
        //mProgress = pProgress;
        mPreview = preview;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "on auto focus " + success);
        if (!success) {
            // try again

            //code in the book was:
//            camera.autoFocus(this);
            autoFocusLater(camera);
        } else {
            Log.d(TAG, "reset mnFocus");
            mnFocused = 15;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        /*
        Camera.Parameters cameraParameters = camera.getParameters();
        String focusMode = camera.getParameters().getFocusMode();
        boolean bUseAutoFocus = focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO);
        if (bUseAutoFocus && (mnFocused == 0 || mbFoundBarcode)) {
            Log.d(TAG, "exit, mnFocused is 0 or mbFoundBarcode is "
                    + mbFoundBarcode + " use auto " + bUseAutoFocus);
            return;
        }
        */
        // TODO: implement our own routine for Camera Preview visualization
        return;
    }
    /**
     * useful method for starting auto focus not too soon
     */
    public void autoFocusLater(final Camera camera)
    {
        final ScmSeqCreator finalContext = this;
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    camera.autoFocus(finalContext);
                }
                catch (RuntimeException e)
                {
                    Log.d(TAG, "error focusing, camera may be closing");
                }
            }
        }, 100);
    }


    /**
     * mbFocused is true when the camera has successfully autofocused
     */
    int mnFocused = 0;



    private CrosshairOverlay mCrosshairOverlay;


}
