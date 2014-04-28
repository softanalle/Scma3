package com.delektre.Scma3;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import org.androidannotations.annotations.EBean;
//import com.delektre.Scma3.CrosshairOverlay;
//import com.delektre.Scma3.Preview;

/**
 * Created by t2r on 4/6/14.
 */

@EBean
public class ScmSeqCreator implements android.hardware.Camera.PreviewCallback,
        android.hardware.Camera.AutoFocusCallback{

    protected static final Logger logger = LoggerFactory.getLogger();

    private static final String TAG = "ScmSeqCreator";

    //private ProgressBar mProgress;
    private Handler handler = new Handler();
    private Preview mPreview;
    Context context_;

    public ScmSeqCreator(Context max) { //double dPerpPos, TextView tv, CheckBox ck, CrosshairOverlay co) {
        context_ = max;
        /*
        me13b = new Ean13Barcode1D();
        mdBarcodePerpPos = dPerpPos;
        mTextViewResult = tv;
        mCheckBoxResult = ck;
        mCrosshairOverlay = co;
        */
        //mProgress = pProgress;
    }

    public void setPreview(Preview preview) {
        logger.debug(TAG + ".setPreview()");
        mPreview = preview;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        logger.debug(TAG + ".onAutoFocus( " + success + ", Camera)");
        if (!success) {
            // try again

            //code in the book was:
//            camera.autoFocus(this);
            autoFocusLater(camera);
        } else {
            logger.debug(TAG + ".onAutoFocus(): reset mnFocus");
            // mnFocused = 15;
            // mHasFocus = false;
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
        logger.debug(TAG + ".autoFocusLater()");
        final ScmSeqCreator finalContext = this;
        mHasFocus = true;
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
                    logger.debug(TAG + ".autoFocusLater(): error focusing, camera may be closing");
                }
            }
        }, 100);
    }


    /**
     * mbFocused is true when the camera has successfully autofocused
     */

    boolean mHasFocus = false;


//    private CrosshairOverlay mCrosshairOverlay;


}
