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

    /**
     * 从 XML 属性中读取 {@code product_id} 和 {@code vendor_id}。
     *
     * @param context 上下文
     * @param attrs   XML 属性集
     */
    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraView);
        try {
            mProductId = typedArray.getInt(R.styleable.CameraView_product_id, mProductId);
            mVendorId = typedArray.getInt(R.styleable.CameraView_vendor_id, mVendorId);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * 初始化摄像头，将当前 View 注册到对应的 {@link CameraManager}。
     * 需在 View 附加到窗口后调用。
     */
    public void initCamera() {
        getCameraManager().addCameraView(this);
    }

    /**
     * 获取与当前 View 绑定的 {@link CameraManager} 单例。
     *
     * @return 对应 productId/vendorId 的 CameraManager 实例
     */
    public CameraManager getCameraManager() {
        return CameraManager.getInstance(mProductId, mVendorId);
    }

    /**
     * 设置拍照结果回调，委托给对应的 {@link CameraManager}。
     *
     * @param captureCallback 拍照结果回调监听器
     */
    public void setOnImageCaptureCallback(IImageCapture.OnImageCaptureCallback captureCallback) {
        getCameraManager().setOnImageCaptureCallback(captureCallback);
    }

    /**
     * 拍照并将结果保存到外部缓存目录下的指定路径。
     *
     * @param folderName  保存照片的子文件夹名称
     * @param pictureName 照片文件名（不含扩展名），最终保存为 {@code pictureName.jpg}
     */
    public void takePicture(String folderName, String pictureName) {
        File outputDirectory = new File(getContext().getExternalCacheDir(), folderName);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File outputFile = new File(outputDirectory, String.format("%s.jpg", pictureName));
        getCameraManager().takePicture(outputFile);
    }

    /**
     * 获取 USB 设备的 Product ID。
     *
     * @return USB Product ID
     */
    public int getProductId() {
        return mProductId;
    }

    /**
     * 获取 USB 设备的 Vendor ID。
     *
     * @return USB Vendor ID
     */
    public int getVendorId() {
        return mVendorId;
    }

    /**
     * View 从窗口分离时自动调用，将当前 View 从 {@link CameraManager} 的预览列表中注销。
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getCameraManager().removeCameraView(this);
    }
}
