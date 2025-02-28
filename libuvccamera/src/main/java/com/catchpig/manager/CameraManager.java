package com.catchpig.manager;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.catchpig.widget.CameraView;
import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.IImageCapture;
import com.herohan.uvcapp.ImageCaptureConfig;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioTextureView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CameraManager implements ICameraHelper.StateCallback {
    private static final String TAG = "CameraManager";
    private static Map<String, CameraManager> sCameraManagers = new HashMap<>();
    private final CameraHelper mCameraHelper = new CameraHelper();
    private final List<CameraView> mCameraViews = new ArrayList<>();
    private boolean mCameraOpened = false;
    private UsbDevice mUsbDevice;
    private IImageCapture.OnImageCaptureCallback mOnImageCaptureCallback;
    private final int mProductId;
    private final int mVendorId;

    public static CameraManager getInstance(int productId, int vendorId) {
        String key = createKey(productId, vendorId);
        if (sCameraManagers.containsKey(key)) {
            return sCameraManagers.get(key);
        } else {
            CameraManager cameraManager = new CameraManager(productId, vendorId);
            sCameraManagers.put(key, cameraManager);
            return cameraManager;
        }
    }

    private static String createKey(int productId, int vendorId) {
        return String.format(Locale.US, "%d/%d", productId, vendorId);
    }

    private CameraManager(int productId, int vendorId) {
        mProductId = productId;
        mVendorId = vendorId;
        init();
    }

    public void init() {
        mCameraHelper.setStateCallback(this);
    }

    public void setOnImageCaptureCallback(IImageCapture.OnImageCaptureCallback onImageCaptureCallback) {
        mOnImageCaptureCallback = onImageCaptureCallback;
    }

    /**
     * 拍照
     */
    public void takePicture(File outFile) {
        if (mOnImageCaptureCallback == null) {
            throw new IllegalArgumentException("请先设置照片回调的监听器(setOnImageCaptureCallback)");
        }
        IImageCapture.OutputFileOptions options = new IImageCapture.OutputFileOptions.Builder(outFile).build();
        mCameraHelper.takePicture(options, mOnImageCaptureCallback);
    }

    public ImageCaptureConfig getImageCaptureConfig() {
        return mCameraHelper.getImageCaptureConfig();
    }

    public void setImageCaptureConfig(ImageCaptureConfig imageCaptureConfig) {
        mCameraHelper.setImageCaptureConfig(imageCaptureConfig);
    }


    public void addTextureView(CameraView cameraView) {
        mCameraViews.add(cameraView);
        if (mCameraOpened) {
            addSurface(cameraView);
        }
        cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mCameraHelper.addSurface(surface, false);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                mCameraHelper.removeSurface(surface);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    public void release() {
        mCameraHelper.release();
    }

    @Override
    public void onAttach(UsbDevice device) {
        Log.i(TAG, "onAttach->productId:" + device.getProductId() + ",vendorId:" + device.getVendorId());
        if (mProductId == device.getProductId() &&
                mVendorId == device.getVendorId()) {
            selectDevice(device);
        }
    }

    public void selectDevice(UsbDevice device) {
        mUsbDevice = device;
        mCameraOpened = false;
        mCameraHelper.selectDevice(device);
    }

    @Override
    public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
        mCameraHelper.openCamera();
    }

    @Override
    public void onCameraOpen(UsbDevice device) {
        mCameraHelper.startPreview();
        for (CameraView textureView : mCameraViews) {
            addSurface(textureView);
        }
        mCameraOpened = true;
    }

    private void addSurface(AspectRatioTextureView textureView) {
        Size size = mCameraHelper.getPreviewSize();
        if (size != null) {
            textureView.setAspectRatio(size.width, size.height);
        }
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture != null) {
            mCameraHelper.addSurface(surfaceTexture, false);
        }
    }

    @Override
    public void onCameraClose(UsbDevice device) {
        Log.d(TAG, "onCameraClose");
        for (CameraView textureView : mCameraViews) {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                mCameraHelper.removeSurface(surfaceTexture);
            }
        }
        mCameraOpened = false;
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
        Log.d(TAG, "onCancel");
        mUsbDevice = null;
    }
}
