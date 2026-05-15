package com.example.family_tasks_proj.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageHelper {

    private static final int JPEG_QUALITY = 70;
    private static final int MAX_SIZE = 600;

    // אם יחס ההקטנה קטן מ-1, התמונה גדולה מהמותר וצריך להקטין אותה
    private static final float SCALE_THRESHOLD = 1.0f;

    // טוען תמונה מהגלריה ומקטין אותה כדי שיהיה אפשר לשמור אותה ב-Firebase
    public static Bitmap loadCorrectedBitmap(ContentResolver resolver, Uri uri) {
        try {
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                return null;
            }

            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float ratio = Math.min((float) MAX_SIZE / width, (float) MAX_SIZE / height);
            
            if (ratio < SCALE_THRESHOLD) {
                return Bitmap.createScaledBitmap(originalBitmap, Math.round(width * ratio), Math.round(height * ratio), true);
            }
            return originalBitmap;
        } catch (Exception exception) {
            return null;
        }
    }

    // דוחס את התמונה וממיר אותה למחרוזת טקסט (Base64) לשמירה במסד הנתונים
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception exception) {
            return null;
        }
    }

    // ממיר מחרוזת טקסט (Base64) חזרה לתמונה (Bitmap) להצגה במסך
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception exception) {
            return null;
        }
    }
}
