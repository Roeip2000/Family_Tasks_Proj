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

/** מחלקת עזר לטיפול בתמונות: המרה ל-Base64, שינוי גודל וחיתוך לעיגול. */
public class ImageHelper {

    private static final int MAX_DIM = 800;
    private static final int QUALITY = 75;

    // טוען תמונה מהגלריה ומתקן את הכיוון שלה
    public static Bitmap loadCorrectedBitmap(ContentResolver resolver, Uri uri) {
        try {
            Bitmap bmp;
            try (InputStream is = resolver.openInputStream(uri)) { bmp = BitmapFactory.decodeStream(is); }
            if (bmp == null) return null;

            int rotation = getRotation(resolver, uri);
            if (rotation != 0) bmp = rotate(bmp, rotation);
            return scale(bmp, MAX_DIM);
        } catch (Exception e) { return null; }
    }

    // הופך Bitmap למחרוזת טקסט לשמירה ב-Firebase
    public static String bitmapToBase64(Bitmap bmp) {
        if (bmp == null) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, out);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    // הופך מחרוזת Base64 חזרה לתמונה
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    // חותך תמונה לצורת עיגול עבור פרופיל
    public static Bitmap getCircularBitmap(Bitmap src) {
        if (src == null) return null;
        int s = Math.min(src.getWidth(), src.getHeight());
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int l = (src.getWidth() - s) / 2, t = (src.getHeight() - s) / 2;
        canvas.drawCircle(s / 2f, s / 2f, s / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, new Rect(l, t, l + s, t + s), new Rect(0, 0, s, s), paint);
        return out;
    }

    private static int getRotation(ContentResolver res, Uri uri) {
        try (InputStream is = res.openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(is);
            int orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orient == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orient == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orient == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception e) {}
        return 0;
    }

    private static Bitmap rotate(Bitmap src, int deg) {
        Matrix m = new Matrix(); m.postRotate(deg);
        Bitmap res = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (res != src) src.recycle();
        return res;
    }

    private static Bitmap scale(Bitmap src, int max) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= max && h <= max) return src;
        float r = Math.min((float) max / w, (float) max / h);
        Bitmap res = Bitmap.createScaledBitmap(src, Math.round(w * r), Math.round(h * r), true);
        if (res != src) src.recycle();
        return res;
    }
}
