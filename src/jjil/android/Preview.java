package jjil.android;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
public class Preview extends ViewGroup implements SurfaceHolder.Callback {

    private final String TAG = "Preview";

    protected static final Logger logger = LoggerFactory.getLogger();
    private boolean isPreview_ = false;

    public Camera getCamera() {
        return mCamera;
    }

    public interface PreviewSizeChangedCallback {
        void previewSizeChanged();
    }


    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Size mPreviewSize;
    private PreviewSizeChangedCallback mPreviewSizeChangedCallback = null;
    private List<Size> mSupportedPreviewSizes;
    private SurfaceView mSurfaceView;

    public Preview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mSurfaceView = new SurfaceView(context, attributeSet);
        addView(mSurfaceView);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes,
                    getMeasuredWidth(), getMeasuredHeight());
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        //Camera.Parameters parameters = mCamera.getParameters();
        parameters.set("rawsave-mode", "1");
        //parameters.set("rawfname",  filename + ".raw");
        // mCamera.setParameters(parameters);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        if (parameters.isAutoWhiteBalanceLockSupported()) {
            parameters.setAutoWhiteBalanceLock(true);
        } else {
            Toast.makeText(getContext(), "Unable to lock AutoWhiteBalance", Toast.LENGTH_LONG).show();
            logger.debug("preview.surfaceChanged(): unable to lock autoWhiteBalance");
        }

        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(true);
        } else {
            Toast.makeText(getContext(), "Unable to lock AutoExposure", Toast.LENGTH_LONG).show();
            logger.debug("unable to lock AutoExposure");
        }
        try {
            camera.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(TAG, "error setting parameters", e);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width == 0 ? 1 : width;
            int previewHeight = height == 0 ? 1 : height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height
                        / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight;
                scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void setPreviewSizeChangedCallback(PreviewSizeChangedCallback callback) {
        mPreviewSizeChangedCallback = callback;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera != null) {
            // Now that the size is known, set up the camera parameters and
            // begin
            // the preview.
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            requestLayout();

            try {
                mCamera.setParameters(parameters);
            } catch (RuntimeException e) {
                Log.e(TAG, "error setting parameters", e);
            }

            mCamera.startPreview();
            if (mPreviewSizeChangedCallback != null) {
                mPreviewSizeChangedCallback.previewSizeChanged();
            }
        }
    }

    private boolean writeImageToDisc(String filename, byte[] data) {
        logger.debug("preview.writeImageToDisc(" + filename + ", " + data.length + " bytes)");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(filename);
            outStream.write(data);
            outStream.close();
            //Log.d(TAG, "writeImageToDisc - wrote bytes: " + data.length);
            Toast.makeText(getContext(), filename + " - wrote bytes: " + data.length, Toast.LENGTH_SHORT).show();
            logger.debug("preview.writeImageToDisc(" + filename + ")");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.debug("preview.writeImageToDisc(): FileNotFoundExeption: " + e.toString());
            return false;
        } catch (IOException e) {
            logger.error("preview.writeImageToDisc(): " + e.toString());
            e.printStackTrace();
            return false;
        } finally {
        }
        logger.debug("writeImageToDisc - complete");
        return true;

    }

    /**
     * take picture and store JPEG and RAW images
     *
     * @param filename the full filename for image, without suffix (.jpg, .raw)
     */
    public void savePicture(final String filename) {
        Camera.PictureCallback jpegCallback = null;
        Camera.PictureCallback rawCallback = null;

        //if ( doJPEG ) {
            /*
                if ( doRAW ) {
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.set("rawsave-mode",  "1");
                        parameters.set("rawfname",  filename + ".raw");
                        mCamera.setParameters(parameters);
                }
                */
        //startPreview();
        jpegCallback = new Camera.PictureCallback() {

            private String mJpegFilename = filename;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    writeImageToDisc(mJpegFilename + ".JPG", data);

                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Toast.makeText(getContext(), "Error while saving JPEG file: " + e, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Other Exception occured: " + e.toString(), Toast.LENGTH_LONG).show();
                }
                startPreview();
            }

        };
        //}
                /*
                if ( doRAW ) {
                        if ( doJPEG) { startPreview(); }
                        rawCallback = new PictureCallback() {
                                private String mRawFilename = filename;
                                @Override public void onPictureTaken(byte[] data, Camera camera) {
                                        try {
                                                if ( data != null && data.length > 0 ) {
                                                if (!writeImageToDisc(mRawFilename + ".RAW", data)) {
                                                        Toast.makeText(getContext(), "Error while saving RAW file", Toast.LENGTH_LONG).show();
                                                }

                                                Thread.sleep(1000);
                                                } else {
                                                        Toast.makeText(getContext(), "Got ZERO data for RAW image: " + mRawFilename, Toast.LENGTH_LONG).show();
                                                }
                                        } catch (InterruptedException e) {
                                                Toast.makeText(getContext(), "Error while saving RAW file: " + e, Toast.LENGTH_LONG).show();
                        }
                                }
                        };
                }
                */
        try {
            mCamera.takePicture(null, rawCallback, jpegCallback);
        } catch (RuntimeException re) {
            logger.error(re.toString());
            Toast.makeText(getContext(), re.toString(), Toast.LENGTH_LONG).show();
        }
        // isPreview_ = false;
    }


    /**
     * Activate camera preview
     */
    public void startPreview() {
        logger.debug("startPreview");
        if (mCamera != null && !isPreview_) {
            logger.debug("preview.startPreview(): do stuff");
            mCamera.startPreview();
            isPreview_ = true;
        }
    }

    /**
     * Stop camera preview
     */
    public void stopPreview() {
        logger.debug("stopPreview");
        if (mCamera != null) {
            logger.debug("preview.stopPreview(): do stuff");
            mCamera.stopPreview();
            isPreview_ = false;
        }
    }

    public void releaseCamera() {
        stopPreview();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void reclaimCamera() {
        if (mCamera == null) {
            mCamera = getCameraInstance();
            if (mCamera == null) {
                Toast.makeText(getContext(), "Unable to obtain camera", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static Camera getCameraInstance() {
        logger.debug("getCameraInstance()");
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {

        }
        return c; // return null if no camera!
    }
}
