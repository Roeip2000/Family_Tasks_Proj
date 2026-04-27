package com.example.family_tasks_proj.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

// תיקון, הקטנה ושמירת תמונות כ-Base64
public class ImageHelper {

    // צלע מקסימלית בפיקסלים — מאזן בין איכות לבין גודל ב-Firebase
    private static final int MAX_DIMENSION = 800;
    private static final int JPEG_QUALITY = 75;

    // טוען תמונה מ-Uri, מתקן סיבוב EXIF ומקטין
    public static Bitmap loadCorrectedBitmap(ContentResolver resolver, Uri uri) {
        try {
            Bitmap bitmap;
            try (InputStream is = resolver.openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is);
            }
            if (bitmap == null) {
                return null;
            }

            // פותחים זרם חדש כי הראשון כבר נקרא
            int rotation = getExifRotation(resolver, uri);

            if (rotation != 0) {
                bitmap = rotateBitmap(bitmap, rotation);
            }

            bitmap = scaleDown(bitmap, MAX_DIMENSION);

            return bitmap;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // ממיר Bitmap למחרוזת Base64 בפורמט JPEG
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // מפענח Base64 ל-Bitmap להצגת תמונות מ-Firebase
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // חותך Bitmap לעיגול לתצוגת אווטאר בלי לעוות
    public static Bitmap getCircularBitmap(Bitmap src) {
        if (src == null) {
            return null;
        }
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int left = (src.getWidth() - size) / 2;
        int top = (src.getHeight() - size) / 2;
        Rect srcRect = new Rect(left, top, left + size, top + size);
        Rect dstRect = new Rect(0, 0, size, size);
        // שלב 1: מציירים עיגול כמסכה
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        // שלב 2: מציירים את התמונה רק בתוך המסכה
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, srcRect, dstRect, paint);
        return output;
    }

    // ========== מתודות פנימיות ==========

    // קורא זווית סיבוב מנתוני EXIF של תמונה מהמצלמה
    private static int getExifRotation(ContentResolver resolver, Uri uri) {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception exception) {
            // אם אין EXIF (למשל PNG) — לא מסובבים
            return 0;
        }
    }

    // מסובב Bitmap בזווית נתונה
    private static Bitmap rotateBitmap(Bitmap source, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
        if (rotated != source) {
            source.recycle();
        }
        return rotated;
    }

    // מקטין Bitmap לפי הצלע הגדולה תוך שמירה על יחס
    private static Bitmap scaleDown(Bitmap source, int maxDim) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= maxDim && height <= maxDim) {
            return source;
        }

        float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        Bitmap scaled = Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
        if (scaled != source) {
            source.recycle();
        }
        return scaled;
    }
}
