package com.example.family_tasks_proj.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** מחלקת עזר לטיפול בתמונות: טעינה מהגלריה והמרה לפורמט טקסט (Base64). */
public class ImageHelper {

    private static final int JPEG_QUALITY = 70;
    private static final int MAX_SIZE = 600; // הגבלת גודל כדי לא להעמיס על ה-Database

    // טוען תמונה מהגלריה ומקטין אותה כדי שיהיה אפשר לשמור אותה ב-Firebase
    public static Bitmap loadCorrectedBitmap(ContentResolver resolver, Uri uri) {
        try {
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) return null;

            // חישוב יחס הקטנה
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float ratio = Math.min((float) MAX_SIZE / width, (float) MAX_SIZE / height);
            
            if (ratio < 1.0) {
                return Bitmap.createScaledBitmap(originalBitmap, Math.round(width * ratio), Math.round(height * ratio), true);
            }
            return originalBitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    // הופך Bitmap למחרוזת טקסט (Base64) כדי לשמור ב-Database
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // דוחס את התמונה כ-JPEG כדי לחסוך מקום
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception ignored) {
            return null;
        }
    }

    // הופך מחרוזת טקסט (Base64) חזרה לתמונה (Bitmap) להצגה במסך
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.trim().isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }
}
