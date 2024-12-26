package com.catchpig.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.IImageCapture;
import com.herohan.uvcapp.ImageCaptureConfig;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioTextureView;

import java.io.File;
import java.util.List;

public class CameraView extends AspectRatioTextureView implements TextureView.SurfaceTextureListener, ICameraHelper.StateCallback {
    private static final String TAG = "CameraView";

    private CameraHelper mCameraHelper;
    private UsbDevice mUsbDevice;
    private OnUsbCameraSelectedListener mOnUsbCameraSelectedListener;
    private IImageCapture.OnImageCaptureCallback mOnImageCaptureCallback;

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnUsbCameraSelectedListener(OnUsbCameraSelectedListener onUsbCameraSelectedListener) {
        mOnUsbCameraSelectedListener = onUsbCameraSelectedListener;
    }

    public void setOnImageCaptureCallback(IImageCapture.OnImageCaptureCallback onImageCaptureCallback) {
        mOnImageCaptureCallback = onImageCaptureCallback;
    }

    /**
     * 拍照
     *
     * @param folderName  文件夹名称
     * @param pictureName 照片名称(不带后缀)
     */
    public void takePicture(String folderName, String pictureName) {
        if (mOnImageCaptureCallback == null) {
            throw new IllegalArgumentException("请先设置照片回调的监听器(setOnImageCaptureCallback)");
        }
        CameraHelper cameraHelper = getCameraHelper();
        File outputDirectory = new File(getContext().getExternalCacheDir(), folderName);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File outputFile = new File(outputDirectory, String.format("%s.jpg", pictureName));
        IImageCapture.OutputFileOptions options = new IImageCapture.OutputFileOptions.Builder(outputFile).build();
        cameraHelper.takePicture(options, mOnImageCaptureCallback);
    }

    /**
     * 获取USB设备列表
     *
     * @return
     */
    public List<UsbDevice> getDeviceList() {
        return getCameraHelper().getDeviceList();
    }

    private CameraHelper getCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(this);
            ImageCaptureConfig imageCaptureConfig = mCameraHelper.getImageCaptureConfig();
            imageCaptureConfig.setJpegCompressionQuality(90);
            mCameraHelper.setImageCaptureConfig(imageCaptureConfig);
        }
        return mCameraHelper;
    }

    public void setupCamera() {
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        getCameraHelper().addSurface(surface, false);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        getCameraHelper().removeSurface(surface);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    public void selectDevice(UsbDevice device) {
        mUsbDevice = device;
        getCameraHelper().selectDevice(device);
    }

    @Override
    public void onAttach(UsbDevice device) {
        if (mOnUsbCameraSelectedListener != null) {
            boolean result = mOnUsbCameraSelectedListener.onSelected(device);
            if (result && mUsbDevice == null) {
                selectDevice(device);
            }
        }
    }

    @Override
    public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
        getCameraHelper().openCamera();
    }

    @Override
    public void onCameraOpen(UsbDevice device) {
        CameraHelper cameraHelper = getCameraHelper();
        cameraHelper.startPreview();
        Size size = cameraHelper.getPreviewSize();
        if (size != null) {
            setAspectRatio(size.width, size.height);
        }
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            cameraHelper.addSurface(surfaceTexture, false);
        }
    }

    @Override
    public void onCameraClose(UsbDevice device) {
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            getCameraHelper().addSurface(surfaceTexture, false);
        }
    }

    @Override
    public void onDeviceClose(UsbDevice device) {
        Log.d(TAG, "onDeviceClose");
    }

    @Override
    public void onDetach(UsbDevice device) {
        Log.d(TAG, "onDetach");
        mUsbDevice = null;
    }

    @Override
    public void onCancel(UsbDevice device) {
        mUsbDevice = null;
    }

    public void release() {
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    public interface OnUsbCameraSelectedListener {
        /**
         * 选择摄像头的回调
         *
         * @param device
         * @return true:打开此摄像头显示画面 false:不显示此摄像头画面
         */
        boolean onSelected(UsbDevice device);
    }
}
