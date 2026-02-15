package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * מסך QR קבוע להורה.
 *
 * אחריות:
 * - מציג QR אחד קבוע עבור ההורה המחובר.
 * - כל ילדי ההורה סורקים את אותו QR.
 * - אחרי הסריקה הילד מגיע למסך בחירת שם (ChildSelectionActivity).
 *
 * פורמט ה-QR: "parent:{parentId}"
 * פורמט זהה לזה שה-ChildQRLoginFragment מפענח.
 *
 * Layout: activity_generate_qr.xml
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף כפתור "שתף QR" — לשמור/לשלוח את תמונת ה-QR.
 * TODO: לשמור את ה-Bitmap ב-onSaveInstanceState כדי לשרוד סיבוב מסך.
 */
public class GenerateQRActivity extends AppCompatActivity {

    private static final String TAG = "QR";

    private ImageView imageViewQrCode;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // חיבור views מה-layout
        imageViewQrCode = findViewById(R.id.imageViewQrCode);
        tvError = findViewById(R.id.tvError);
        Button btnBack = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> finish());

        // זיהוי ההורה המחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Parent not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // יצירת QR קבוע להורה — כל הילדים סורקים אותו
        generateParentQR(user.getUid());
    }

    /**
     * מייצר ומציג QR עבור ההורה.
     * הפורמט: "parent:{parentId}" — הילד סורק, מגיע למסך בחירת שם.
     *
     * @param parentId UID של ההורה המחובר
     */
    private void generateParentQR(String parentId) {
        String payload = "parent:" + parentId;
        Log.d(TAG, "QR payload=" + payload);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    payload, BarcodeFormat.QR_CODE, 800, 800);

            imageViewQrCode.setVisibility(View.VISIBLE);
            imageViewQrCode.setImageBitmap(bitmap);
            tvError.setVisibility(View.GONE);
        } catch (WriterException e) {
            imageViewQrCode.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            Log.e(TAG, "WriterException", e);
        }
    }
}
