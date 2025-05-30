package com.herohan.uvcapp;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageSaver2 implements Runnable {
    private static final String TAG = ImageSaver.class.getSimpleName();

    private static final int PENDING = 1;
    private static final int NOT_PENDING = 0;

    // The image that was captured
    private final Bitmap mImage;
    private final int mWidth;
    private final int mHeight;
    // The compression quality level of the output JPEG image
    private final int mJpegQuality;
    // The target location to save the image to.
    @NonNull
    private final ImageCapture.OutputFileOptions mOutputFileOptions;
    // The callback to call on completion
    @NonNull
    private final OnImageSavedCallback mCallback;

    public ImageSaver2(Bitmap image, int width, int height,
                       @IntRange(from = 1, to = 100) int jpegQuality,
                       @NonNull ImageCapture.OutputFileOptions outputFileOptions,
                       @NonNull OnImageSavedCallback callback) {
        this.mImage = image;
        this.mWidth = width;
        this.mHeight = height;
        this.mJpegQuality = jpegQuality;
        this.mOutputFileOptions = outputFileOptions;
        this.mCallback = callback;
    }

    @Override
    public void run() {
        SaveError saveError = null;
        String errorMessage = null;
        Exception exception = null;
        Uri outputUri = null;

        try {
            byte[] data = imageToJpegByteArray(mImage, mJpegQuality);
            if (data == null) {
                saveError = SaveError.ENCODE_FAILED;
                errorMessage = "Failed to encode mImage";
            } else {
                if (isSaveToMediaStore()) {
                    ContentValues values = mOutputFileOptions.getContentValues() != null
                            ? new ContentValues(mOutputFileOptions.getContentValues())
                            : new ContentValues();
                    setContentValuePending(values, PENDING);
                    outputUri = mOutputFileOptions.getContentResolver().insert(
                            mOutputFileOptions.getSaveCollection(),
                            values);
                    if (outputUri == null) {
                        saveError = SaveError.FILE_IO_FAILED;
                        errorMessage = "Failed to insert URI.";
                    } else {
                        if (!copyByteArrayToUri(data, outputUri)) {
                            saveError = SaveError.FILE_IO_FAILED;
                            errorMessage = "Failed to save to URI.";
                        }
                        setUriNotPending(outputUri);
                    }
                } else if (isSaveToOutputStream()) {
                    mOutputFileOptions.getOutputStream().write(data);
                } else if (isSaveToFile()) {
                    File targetFile = mOutputFileOptions.getFile();
                    copyByteArrayToFile(data, targetFile);
                    outputUri = Uri.fromFile(targetFile);
                }
            }
        } catch (IOException e) {
            saveError = SaveError.FILE_IO_FAILED;
            errorMessage = "Failed to write destination file.";
            exception = e;
        }

        if (saveError != null) {
            mCallback.onError(saveError, errorMessage, exception);
        } else {
            mCallback.onImageSaved(new ImageCapture.OutputFileResults(outputUri));
        }
    }

    private byte[] imageToJpegByteArray(Bitmap image, int jpegQuality) {
        Bitmap bmp = null;
        byte[] byteArray = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            bmp = Bitmap.createBitmap(image, 0, 0, mWidth, mHeight);
            bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out);
            byteArray = out.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "failed to save file", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "failed to save file", e);
            }
            if (bmp != null) {
                bmp.recycle();
            }
            if (image != null) {
                image.recycle();
            }
        }
        return byteArray;
    }

    private boolean isSaveToMediaStore() {
        return mOutputFileOptions.getSaveCollection() != null
                && mOutputFileOptions.getContentResolver() != null
                && mOutputFileOptions.getContentValues() != null;
    }

    private boolean isSaveToFile() {
        return mOutputFileOptions.getFile() != null;
    }

    private boolean isSaveToOutputStream() {
        return mOutputFileOptions.getOutputStream() != null;
    }

    /**
     * Set IS_PENDING flag to {@link ContentValues}.
     */
    private void setContentValuePending(@NonNull ContentValues values, int isPending) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, isPending);
        }
    }

    /**
     * Removes IS_PENDING flag during the writing to {@link Uri}.
     */
    private void setUriNotPending(@NonNull Uri outputUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            setContentValuePending(values, NOT_PENDING);
            mOutputFileOptions.getContentResolver().update(outputUri, values, null, null);
        }
    }

    /**
     * Copies  byte array to {@link Uri}.
     *
     * @return false if the {@link Uri} is not writable.
     */
    private boolean copyByteArrayToUri(@NonNull byte[] data, @NonNull Uri uri) throws IOException {
        try (OutputStream outputStream =
                     mOutputFileOptions.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                // The URI is not writable.
                return false;
            }
            outputStream.write(data);
        }
        return true;
    }

    /**
     * Copies  byte array to {@link File}.
     *
     * @return false if the {@link File} is not writable.
     */
    private boolean copyByteArrayToFile(@NonNull byte[] data, @NonNull File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
        }
        return true;
    }

    /**
     * Type of error that occurred during save
     */
    public enum SaveError {
        /**
         * Failed to write to or close the file
         */
        FILE_IO_FAILED,
        /**
         * Failure when attempting to encode image
         */
        ENCODE_FAILED,
        UNKNOWN
    }

    public interface OnImageSavedCallback {

        void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults);

        void onError(@NonNull SaveError saveError, @NonNull String message,
                     @Nullable Throwable cause);
    }
}
