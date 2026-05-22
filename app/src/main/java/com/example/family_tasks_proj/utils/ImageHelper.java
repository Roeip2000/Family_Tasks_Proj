package com.example.family_tasks_proj.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

// מחלקת עזר לטיפול בתמונות בפרויקט
public class ImageHelper {

    private static final int IMAGE_QUALITY = 70;
    private static final int MAX_IMAGE_SIZE = 600;

    // טוען תמונה מהגלריה ומקטין אותה אם היא גדולה מדי
    public static Bitmap loadResizedBitmap(ContentResolver resolver, Uri uri) {
        try {
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                return null;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            float scale = Math.min(
                    (float) MAX_IMAGE_SIZE / width,
                    (float) MAX_IMAGE_SIZE / height
            );

            if (scale < 1) {
                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            return bitmap;
        } catch (Exception exception) {
            return null;
        }
    }

    // ממיר תמונה לטקסט כדי לשמור אותה ב-Firebase
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            return null;
        }
    }

    // ממיר טקסט Base64 בחזרה לתמונה
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception exception) {
            return null;
        }
    }
}
