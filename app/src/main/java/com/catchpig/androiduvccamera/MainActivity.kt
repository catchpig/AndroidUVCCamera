package com.catchpig.androiduvccamera

import android.hardware.usb.UsbDevice
import android.os.Bundle
import com.catchpig.androiduvccamera.databinding.ActivityMainBinding
import com.catchpig.mvvm.base.activity.BaseActivity
import com.catchpig.utils.ext.logi
import com.catchpig.widget.CameraView.OnUsbCameraSelectedListener
import com.herohan.uvcapp.IImageCapture
import com.herohan.uvcapp.IImageCapture.OnImageCaptureCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : BaseActivity<ActivityMainBinding>(), OnUsbCameraSelectedListener,
    OnImageCaptureCallback {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bodyBinding {
            camera.setOnUsbCameraSelectedListener(this@MainActivity)
            camera.setOnImageCaptureCallback(this@MainActivity)
            XXPermissions.with(this@MainActivity)
                .permission(Permission.CAMERA)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .request { permissions, allGranted ->
                    if (allGranted) {
                        camera.setupCamera()
                    }
                }
            takePicture.setOnClickListener {
                takePicture()
            }
        }

    }

    private fun takePicture() {
        val name = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            .format(System.currentTimeMillis())
        bodyBinding.camera.takePicture("aiImages", name)
    }


    override fun onSelected(device: UsbDevice): Boolean {
//        device.productName?.let {
//            if (it.contains("PC")) {
//                return true
//            }
//        }
        return true
    }

    override fun onImageSaved(result: IImageCapture.OutputFileResults) {
        result.savedUri?.path?.let {
            "onImageSaved->${it}".logi(TAG)

        }
    }

    override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
        "onError->${message}".logi(TAG)
    }

    override fun onDestroy() {
        super.onDestroy()
        bodyBinding.camera.release()
    }
}
