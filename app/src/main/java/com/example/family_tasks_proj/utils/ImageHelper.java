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

    // איכות הדחיסה של התמונה לפני שמירה
    private static final int JPEG_QUALITY = 70;

    // הגודל המקסימלי של תמונה שנשמרת במסד
    private static final int MAX_SIZE = 600;

    // אם יחס ההקטנה קטן מ-1, התמונה גדולה מהמותר וצריך להקטין אותה
    private static final float SCALE_THRESHOLD = 1.0f;

    // טוען תמונה מהגלריה ומקטין אותה לפני שמירה
    public static Bitmap loadResizedBitmap(ContentResolver resolver, Uri uri) {
        try {
            // פתיחת התמונה שנבחרה מהגלריה
            InputStream imageStream = resolver.openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);

            if (originalBitmap == null) {
                return null;
            }

            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            // חישוב יחס הקטנה כדי שהתמונה לא תהיה גדולה מדי
            float resizeRatio = Math.min((float) MAX_SIZE / width, (float) MAX_SIZE / height);

            // אם התמונה גדולה מהמותר, מקטינים אותה
            if (resizeRatio < SCALE_THRESHOLD) {
                int newWidth = Math.round(width * resizeRatio);
                int newHeight = Math.round(height * resizeRatio);
                return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            }

            return originalBitmap;
        } catch (Exception exception) {
            return null;
        }
    }

    // ממיר Bitmap לטקסט Base64 כדי לשמור תמונה ב-Firebase
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // דחיסת התמונה ל-JPEG והפיכתה למערך bytes
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // המרת ה-bytes לטקסט Base64
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            return null;
        }
    }

    // ממיר Base64 בחזרה ל-Bitmap כדי להציג את התמונה במסך
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        try {
            // פענוח הטקסט חזרה ל-bytes
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);

            // יצירת Bitmap מתוך ה-bytes
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception exception) {
            return null;
        }
    }
}
