![](https://img.shields.io/badge/license-Apache--2.0-blue)
![](https://img.shields.io/badge/android-30%2B-brightgreen?logo=android)
![](https://img.shields.io/badge/jitpack-release-green?logo=jitpack)
[![](https://jitpack.io/v/catchpig/AndroidUVCCamera.svg)](https://jitpack.io/#catchpig/AndroidUVCCamera)
![GitHub forks](https://img.shields.io/github/forks/catchpig/AndroidUVCCamera)
![GitHub Repo stars](https://img.shields.io/github/stars/catchpig/AndroidUVCCamera)

# AndroidUVCCamera

Android UVC USB 摄像头接入库，提供 `CameraView` 自定义控件和 `CameraManager` 设备管理器，支持同时接入多路 UVC 摄像头、实时预览及拍照。

## 环境要求

- Android API 30+（Android 11 及以上）
- 支持 ABI：`armeabi-v7a`、`arm64-v8a`
- 需要设备支持 USB Host

## 添加依赖

**Step 1：** 在项目根目录的 `settings.gradle` 或 `build.gradle` 中添加 JitPack 仓库：

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2：** 在模块的 `build.gradle` 中添加依赖：

```groovy
implementation "com.github.catchpig:AndroidUVCCamera:last_version"
```

## 权限配置

在 `AndroidManifest.xml` 中声明所需权限和 USB Host 特性：

```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-feature android:glEsVersion="0x00020000" android:required="true" />

<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

> `CAMERA` 和 `WRITE_EXTERNAL_STORAGE` 为危险权限，需在运行时动态申请后再调用 `initCamera()`。

## 布局使用

在 XML 布局中添加 `CameraView`，通过 `product_id` 和 `vendor_id` 标识摄像头设备：

```xml
<com.catchpig.widget.CameraView
    android:id="@+id/camera1"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:product_id="45829"
    app:vendor_id="40978" />
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `app:product_id` | int | USB 设备 Product ID |
| `app:vendor_id` | int | USB 设备 Vendor ID |

> `product_id` 和 `vendor_id` 可通过 `adb shell lsusb` 或系统 USB 管理 API 获取。

## 代码接入

### 初始化摄像头

动态申请权限后调用 `initCamera()` 启动预览：

```kotlin
// Kotlin 示例
cameraView.setOnImageCaptureCallback(this)

XXPermissions.with(this)
    .permission(Permission.CAMERA)
    .permission(Permission.WRITE_EXTERNAL_STORAGE)
    .request { _, allGranted ->
        if (allGranted) {
            cameraView.initCamera()
        }
    }
```

### 拍照

```kotlin
// 保存到外部缓存目录：{externalCacheDir}/folderName/pictureName.jpg
cameraView.takePicture("myImages", "photo_001")
```

### 拍照结果回调

Activity 或 Fragment 实现 `IImageCapture.OnImageCaptureCallback`：

```kotlin
class MainActivity : AppCompatActivity(), IImageCapture.OnImageCaptureCallback {

    override fun onImageSaved(result: IImageCapture.OutputFileResults) {
        val path = result.savedUri?.path
        // 照片保存成功，path 为文件路径
    }

    override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
        // 拍照失败
    }
}
```

### 释放资源

在 `onDestroy()` 中释放摄像头资源，避免内存泄漏：

```kotlin
override fun onDestroy() {
    super.onDestroy()
    cameraView.cameraManager.release()
}
```

## 多路摄像头

在布局中添加多个 `CameraView`，每个配置不同的 `product_id` / `vendor_id` 即可同时接入多路摄像头。多路初始化建议错开时间，避免 USB 权限申请冲突：

```xml
<com.catchpig.widget.CameraView
    android:id="@+id/camera1"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    app:product_id="45829"
    app:vendor_id="40978" />

<com.catchpig.widget.CameraView
    android:id="@+id/camera2"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    app:product_id="25451"
    app:vendor_id="3141" />
```

```kotlin
camera1.initCamera()
// 第二路延迟初始化，避免 USB 权限冲突
camera1.postDelayed({
    camera2.initCamera()
}, 5000)
```

## 高级配置

### 预览配置（旋转 / 镜像）

```java
CameraPreviewConfig config = cameraView.getCameraManager()
        .getImageCaptureConfig(); // 通过 CameraHelper 获取

// 设置预览旋转角度（0 / 90 / 180 / 270）
CameraPreviewConfig previewConfig = new CameraPreviewConfig()
        .setRotation(90)
        .setMirror(MirrorMode.MIRROR_HORIZONTAL);

cameraView.getCameraManager().setPreviewConfig(previewConfig);
```

### 拍照配置（质量 / 策略）

```java
ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)  // 高质量模式
        .setJpegCompressionQuality(90);                               // JPEG 压缩质量 1-100

cameraView.getCameraManager().setImageCaptureConfig(imageCaptureConfig);
```

| 配置项 | 可选值 | 默认值 |
|--------|--------|--------|
| `setCaptureMode` | `CAPTURE_MODE_MINIMIZE_LATENCY` / `CAPTURE_MODE_MAXIMIZE_QUALITY` | 低延迟 |
| `setCaptureStrategy` | `CAPTURE_STRATEGY_OPENGL_ES` / `CAPTURE_STRATEGY_IMAGE_READER` | OpenGL ES |
| `setJpegCompressionQuality` | 1 ~ 100 | 低延迟默认值 |
| `setRotation` | 0 / 90 / 180 / 270 | 0 |
| `setMirror` | `MirrorMode.MIRROR_NORMAL` / `MIRROR_HORIZONTAL` / ... | NORMAL |

## License

```
Copyright 2024 catchpig

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
