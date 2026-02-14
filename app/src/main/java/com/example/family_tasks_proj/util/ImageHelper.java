package com.example.family_tasks_proj.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * עזר לטיפול בתמונות — פותר שלוש בעיות מרכזיות:
 *
 * 1. EXIF: תמונות מהמצלמה מגיעות עם תגית סיבוב שלא נקראת ע"י BitmapFactory.
 *    בלי תיקון — התמונה נראית מסובבת/הפוכה.
 * 2. יחס גובה-רוחב: הקטנה ל-400×400 מעוותת. במקום זה — מקטינים
 *    לפי הצלע הגדולה (MAX_DIMENSION) ושומרים על היחס.
 * 3. עקביות: אותו Bitmap משמש לתצוגה מקדימה וגם לשמירה כ-Base64,
 *    כך שמה שרואים = מה שנשמר = מה שמוצג אחר כך.
 */
public class ImageHelper {

    /** גודל מקסימלי (פיקסלים) לצלע הגדולה — שומר על איכות סבירה בלי לפוצץ את Firebase */
    private static final int MAX_DIMENSION = 800;
    private static final int JPEG_QUALITY = 75;

    /**
     * טוען תמונה מ-Uri, מתקן סיבוב EXIF, ומקטין תוך שמירה על יחס גובה-רוחב.
     *
     * @param resolver ContentResolver לגישה ל-Uri
     * @param uri      ה-Uri של התמונה שנבחרה מהגלריה/מצלמה
     * @return Bitmap מתוקן ומוקטן, או null בכישלון
     */
    public static Bitmap loadCorrectedBitmap(ContentResolver resolver, Uri uri) {
        try {
            // שלב 1: פענוח ה-Bitmap מה-stream
            Bitmap bitmap;
            try (InputStream is = resolver.openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is);
            }
            if (bitmap == null) return null;

            // שלב 2: קריאת זווית EXIF — חייבים stream חדש כי הראשון נצרך
            int rotation = getExifRotation(resolver, uri);

            // שלב 3: הפעלת סיבוב אם צריך
            if (rotation != 0) {
                bitmap = rotateBitmap(bitmap, rotation);
            }

            // שלב 4: הקטנה לפי הצלע הגדולה תוך שמירה על יחס
            bitmap = scaleDown(bitmap, MAX_DIMENSION);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ממיר Bitmap למחרוזת Base64 (JPEG).
     *
     * @param bitmap ה-Bitmap שכבר עבר תיקון EXIF + הקטנה
     * @return מחרוזת Base64, או null בכישלון
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * מפענח מחרוזת Base64 ל-Bitmap.
     * משמש להצגת תמונות שנשמרו ב-Firebase.
     *
     * @param base64 מחרוזת Base64 של תמונת JPEG
     * @return Bitmap, או null אם הקלט ריק/פגום
     */
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ========== מתודות פנימיות ==========

    /**
     * קורא את תגית הסיבוב מ-EXIF metadata של התמונה.
     * חובה לפתוח InputStream חדש — ExifInterface צורך את כולו.
     *
     * @return זווית סיבוב במעלות (0, 90, 180, 270)
     */
    private static int getExifRotation(ContentResolver resolver, Uri uri) {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (Exception e) {
            // אם אין EXIF (למשל PNG) — פשוט לא מסובבים
            return 0;
        }
    }

    /** מסובב Bitmap בזווית נתונה. */
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

    /**
     * מקטין Bitmap כך שהצלע הגדולה לא תעלה על maxDim.
     * שומר על יחס גובה-רוחב — לא חותך, לא מעוות.
     * אם התמונה כבר קטנה מספיק — מחזיר אותה כמות שהיא.
     */
    private static Bitmap scaleDown(Bitmap source, int maxDim) {
        int w = source.getWidth();
        int h = source.getHeight();

        if (w <= maxDim && h <= maxDim) {
            return source;
        }

        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
        int newW = Math.round(w * ratio);
        int newH = Math.round(h * ratio);

        Bitmap scaled = Bitmap.createScaledBitmap(source, newW, newH, true);
        if (scaled != source) {
            source.recycle();
        }
        return scaled;
    }
}
