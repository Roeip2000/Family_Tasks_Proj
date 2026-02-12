package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * מציג קוד QR שהילד סורק כדי להתחבר.
 *
 * מקבל ב-Intent:
 * - parentId  (חובה, fallback: FirebaseAuth currentUser)
 * - childId   (חובה)
 * - childName (אופציונלי — מוצג ככותרת מעל ה-QR)
 *
 * פורמט ה-QR: "parent:{parentId}|child:{childId}"
 * פורמט זהה לזה שה-ChildQRLoginFragment מפענח.
 *
 * המסך נשאר פתוח עד שההורה לוחץ "חזרה" — אין finish() אוטומטי.
 * ה-payload נשמר ב-onSaveInstanceState כדי לשרוד סיבוב מסך.
 */
public class GenerateQRActivity extends AppCompatActivity {

    private static final String TAG = "QR";

    /** מפתח לשמירת ה-payload ב-Bundle (לשרידת סיבוב מסך) */
    private static final String KEY_PAYLOAD = "qr_payload";
    private static final String KEY_CHILD_NAME = "child_name";

    private TextView tvChildLabel;
    private ProgressBar progressBar;
    private ImageView imageViewQrCode;

    /** ה-payload שמקודד ב-QR — נשמר כדי לייצר מחדש אחרי סיבוב */
    private String payload;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        tvChildLabel = findViewById(R.id.tvChildLabel);
        progressBar = findViewById(R.id.progressBar);
        imageViewQrCode = findViewById(R.id.imageViewQrCode);
        Button btnBack = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> finish());

        // שחזור אחרי סיבוב מסך
        if (savedInstanceState != null) {
            payload = savedInstanceState.getString(KEY_PAYLOAD);
            childName = savedInstanceState.getString(KEY_CHILD_NAME);
        }

        if (payload == null) {
            // טעינה ראשונה — בניית ה-payload מה-Intent
            payload = buildPayloadFromIntent();
        }

        if (childName == null) {
            childName = getIntent().getStringExtra("childName");
        }

        setChildLabel(childName);

        if (payload == null) {
            // חסרים נתונים — אין מה להציג
            Toast.makeText(this, "Missing parentId/childId", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        generateAndShowQR(payload);
    }

    /**
     * בונה את ה-payload מה-Intent extras.
     *
     * @return מחרוזת בפורמט "parent:XXX|child:YYY", או null אם חסרים נתונים
     */
    private String buildPayloadFromIntent() {
        String childId = getIntent().getStringExtra("childId");
        String parentId = getIntent().getStringExtra("parentId");

        // fallback — אם לא הועבר parentId, לוקחים מההורה המחובר
        if (parentId == null || parentId.trim().isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) parentId = user.getUid();
        }

        Log.d(TAG, "parentId=" + parentId + " childId=" + childId);

        if (parentId == null || parentId.trim().isEmpty()
                || childId == null || childId.trim().isEmpty()) {
            return null;
        }

        return "parent:" + parentId.trim() + "|child:" + childId.trim();
    }

    /**
     * מציג את שם הילד ב-TextView מעל ה-QR.
     * אם השם לא הועבר — מציג כותרת כללית.
     */
    private void setChildLabel(String name) {
        if (name != null && !name.trim().isEmpty()) {
            tvChildLabel.setText("QR עבור " + name.trim());
        } else {
            tvChildLabel.setText("QR Code");
        }
    }

    /**
     * מייצר Bitmap של QR ומציג אותו.
     * מחביא את ה-ProgressBar ומראה את ה-ImageView.
     *
     * @param qrPayload המחרוזת שתקודד ב-QR
     */
    private void generateAndShowQR(String qrPayload) {
        Log.d(TAG, "payload=" + qrPayload);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrPayload, BarcodeFormat.QR_CODE, 800, 800);

            // הצלחה — מחביאים ספינר, מציגים QR
            progressBar.setVisibility(View.GONE);
            imageViewQrCode.setVisibility(View.VISIBLE);
            imageViewQrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed generating QR", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "WriterException", e);
        }
    }

    /** שומר payload + childName כדי לשרוד סיבוב מסך בלי Intent מחדש. */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (payload != null) {
            outState.putString(KEY_PAYLOAD, payload);
        }
        if (childName != null) {
            outState.putString(KEY_CHILD_NAME, childName);
        }
    }
}
