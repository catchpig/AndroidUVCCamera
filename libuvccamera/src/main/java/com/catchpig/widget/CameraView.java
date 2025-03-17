package com.catchpig.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.catchpig.manager.CameraManager;
import com.catchpig.uvccamera.R;
import com.herohan.uvcapp.IImageCapture;
import com.serenegiant.widget.AspectRatioTextureView;

import java.io.File;

public class CameraView extends AspectRatioTextureView {
    private static final String TAG = "CameraView";
    private int mProductId;
    private int mVendorId;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttr(context, attrs);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraView);
        try {
            mProductId = typedArray.getInt(R.styleable.CameraView_product_id, mProductId);
            mVendorId = typedArray.getInt(R.styleable.CameraView_vendor_id, mVendorId);
        } finally {
            typedArray.recycle();
        }
    }

    public void initCamera() {
        getCameraManager().addCameraView(this);
    }

    public CameraManager getCameraManager() {
        return CameraManager.getInstance(mProductId, mVendorId);
    }

    public void setOnImageCaptureCallback(IImageCapture.OnImageCaptureCallback captureCallback) {
        getCameraManager().setOnImageCaptureCallback(captureCallback);
    }

    /**
     * 拍照
     *
     * @param folderName  文件夹名称
     * @param pictureName 照片名称(不带后缀)
     */
    public void takePicture(String folderName, String pictureName) {
        File outputDirectory = new File(getContext().getExternalCacheDir(), folderName);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File outputFile = new File(outputDirectory, String.format("%s.jpg", pictureName));
        getCameraManager().takePicture(outputFile);
    }

    public int getProductId() {
        return mProductId;
    }

    public int getVendorId() {
        return mVendorId;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getCameraManager().removeCameraView(this);
    }
}
