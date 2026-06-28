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
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CameraManager implements ICameraHelper.StateCallback {
    private static final String TAG = "CameraManager";
    private static final Map<String, CameraManager> sCameraManagers = new ConcurrentHashMap<>();
    private final CameraHelper mCameraHelper = new CameraHelper();
    private final List<CameraView> mCameraViews = new ArrayList<>();
    private boolean mCameraOpened = false;
    private UsbDevice mUsbDevice;
    private IImageCapture.OnImageCaptureCallback mOnImageCaptureCallback;
    private final int mProductId;
    private final int mVendorId;

    /**
     * 获取指定 USB 设备的 CameraManager 单例，不存在则自动创建。
     *
     * @param productId USB 设备的 Product ID
     * @param vendorId  USB 设备的 Vendor ID
     * @return 对应设备的 CameraManager 实例
     */
    public static CameraManager getInstance(int productId, int vendorId) {
        return sCameraManagers.computeIfAbsent(
                createKey(productId, vendorId),
                key -> new CameraManager(productId, vendorId));
    }

    /**
     * 根据 productId 和 vendorId 生成 Map 唯一键。
     *
     * @param productId USB 设备的 Product ID
     * @param vendorId  USB 设备的 Vendor ID
     * @return 格式为 "productId/vendorId" 的字符串键
     */
    private static String createKey(int productId, int vendorId) {
        return String.format(Locale.US, "%d/%d", productId, vendorId);
    }

    private CameraManager(int productId, int vendorId) {
        mProductId = productId;
        mVendorId = vendorId;
        init();
    }

    /**
     * 初始化，向 CameraHelper 注册设备状态回调。
     */
    public void init() {
        mCameraHelper.setStateCallback(this);
    }

    /**
     * 设置拍照结果回调监听器，需在调用 {@link #takePicture} 前设置。
     *
     * @param onImageCaptureCallback 拍照结果回调
     */
    public void setOnImageCaptureCallback(IImageCapture.OnImageCaptureCallback onImageCaptureCallback) {
        mOnImageCaptureCallback = onImageCaptureCallback;
    }

    /**
     * 拍照并将结果保存到指定文件。
     *
     * @param outFile 照片输出文件
     * @throws IllegalArgumentException 若未事先调用 {@link #setOnImageCaptureCallback} 设置回调
     */
    public void takePicture(File outFile) {
        if (mOnImageCaptureCallback == null) {
            throw new IllegalArgumentException("请先设置照片回调的监听器(setOnImageCaptureCallback)");
        }
        IImageCapture.OutputFileOptions options = new IImageCapture.OutputFileOptions.Builder(outFile).build();
        mCameraHelper.takePicture(options, mOnImageCaptureCallback);
    }

    /**
     * 获取当前拍照配置。
     *
     * @return 当前的 {@link ImageCaptureConfig} 配置对象
     */
    public ImageCaptureConfig getImageCaptureConfig() {
        return mCameraHelper.getImageCaptureConfig();
    }

    /**
     * 设置拍照配置，如分辨率、质量等参数。
     *
     * @param imageCaptureConfig 拍照配置对象
     */
    public void setImageCaptureConfig(ImageCaptureConfig imageCaptureConfig) {
        mCameraHelper.setImageCaptureConfig(imageCaptureConfig);
    }


    /**
     * 注册摄像头预览 View。若相机已开启，则立即将该 View 的 Surface 绑定到预览流。
     *
     * @param cameraView 需要显示预览画面的 {@link CameraView}
     */
    public void addCameraView(CameraView cameraView) {
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

    /**
     * 释放所有相机资源。依次执行：移除各 View 的 Surface 绑定和监听器、
     * 释放 CameraHelper、重置状态字段、并从静态实例池中注销当前实例。
     * 调用后此实例不可再使用。
     */
    public void release() {
        for (CameraView cameraView : mCameraViews) {
            cameraView.setSurfaceTextureListener(null);
            SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
            if (surfaceTexture != null) {
                mCameraHelper.removeSurface(surfaceTexture);
            }
        }
        mCameraViews.clear();

        mCameraHelper.release();

        mCameraOpened = false;
        mUsbDevice = null;
        mOnImageCaptureCallback = null;

        sCameraManagers.remove(createKey(mProductId, mVendorId));
    }

    /**
     * USB 设备连接回调。匹配到目标设备时自动发起选择连接。
     *
     * @param device 已连接的 USB 设备
     */
    @Override
    public void onAttach(UsbDevice device) {
        Log.i(TAG, "onAttach->productId:" + device.getProductId() + ",vendorId:" + device.getVendorId());
        if (mProductId == device.getProductId() &&
                mVendorId == device.getVendorId()) {
            selectDevice(device);
        }
    }

    /**
     * 手动选择指定 USB 设备并发起连接，重置相机开启状态。
     *
     * @param device 要连接的 USB 设备
     */
    public void selectDevice(UsbDevice device) {
        mUsbDevice = device;
        mCameraOpened = false;
        mCameraHelper.selectDevice(device);
    }

    /**
     * USB 设备打开回调，在此触发摄像头开启。
     *
     * @param device      已打开的 USB 设备
     * @param isFirstOpen 是否为首次打开
     */
    @Override
    public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
        mCameraHelper.openCamera();
    }

    /**
     * 摄像头开启回调，启动预览并将所有已注册的 View Surface 绑定到预览流。
     *
     * @param device 已开启摄像头的 USB 设备
     */
    @Override
    public void onCameraOpen(UsbDevice device) {
        mCameraHelper.startPreview();
        for (CameraView textureView : mCameraViews) {
            addSurface(textureView);
        }
        mCameraOpened = true;
    }

    /**
     * 将指定 View 的 SurfaceTexture 绑定到预览流，并根据预览尺寸设置宽高比。
     *
     * @param textureView 需要绑定的 {@link AspectRatioTextureView}
     */
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

    /**
     * 摄像头关闭回调，移除所有已注册 View 的 Surface 绑定并重置开启状态。
     *
     * @param device 已关闭摄像头的 USB 设备
     */
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

    /**
     * USB 设备关闭回调。
     *
     * @param device 已关闭的 USB 设备
     */
    @Override
    public void onDeviceClose(UsbDevice device) {
        Log.d(TAG, "onDeviceClose");
    }

    /**
     * USB 设备断开回调，清空设备引用。
     *
     * @param device 已断开的 USB 设备
     */
    @Override
    public void onDetach(UsbDevice device) {
        Log.d(TAG, "onDetach");
        mUsbDevice = null;
    }

    /**
     * USB 权限授权被取消回调，清空设备引用。
     *
     * @param device 授权被取消的 USB 设备
     */
    @Override
    public void onCancel(UsbDevice device) {
        Log.d(TAG, "onCancel");
        mUsbDevice = null;
    }

    /**
     * 从预览列表中移除指定 View，不会自动解绑 Surface（Surface 销毁时由监听器处理）。
     *
     * @param cameraView 需要移除的 {@link CameraView}
     */
    public void removeCameraView(CameraView cameraView) {
        mCameraViews.remove(cameraView);
    }
}
