package com.catchpig.androiduvccamera

import android.os.Bundle
import com.catchpig.androiduvccamera.databinding.ActivityMainBinding
import com.catchpig.mvvm.base.activity.BaseActivity
import com.catchpig.utils.ext.logi
import com.herohan.uvcapp.IImageCapture
import com.herohan.uvcapp.IImageCapture.OnImageCaptureCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : BaseActivity<ActivityMainBinding>(),
    OnImageCaptureCallback {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bodyBinding {
            camera1.setOnImageCaptureCallback(this@MainActivity)
            XXPermissions.with(this@MainActivity)
                .permission(Permission.CAMERA)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .request { permissions, allGranted ->
                    if (allGranted) {
                        camera1.initCamera()
                        camera1.postDelayed({
                            camera2.initCamera()
                        }, 5000)
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
        bodyBinding.camera1.takePicture("aiImages", name)
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
        bodyBinding {
            camera1.cameraManager.release()
        }
    }
}
