package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateQRActivity extends AppCompatActivity {

    private static final String TAG = "QR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        Log.d(TAG, "GenerateQRActivity opened");

        ImageView imageViewQrCode = findViewById(R.id.imageViewQrCode);
        if (imageViewQrCode == null) {
            Toast.makeText(this, "ImageView not found (check id)", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "imageViewQrCode is null");
            finish();
            return;
        }

        String childId = getIntent().getStringExtra("childId");
        String parentId = getIntent().getStringExtra("parentId");

        // fallback: אם לא הועבר parentId ב-Intent, ניקח מההורה המחובר
        if (parentId == null || parentId.trim().isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) parentId = user.getUid();
        }

        Log.d(TAG, "parentId=" + parentId + " childId=" + childId);

        if (parentId == null || parentId.trim().isEmpty() || childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "Missing parentId/childId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ פורמט אחיד בין גנרטור לסורק
        String payload = "parent:" + parentId.trim() + "|child:" + childId.trim();
        Log.d(TAG, "payload=" + payload);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 800, 800);
            imageViewQrCode.setImageBitmap(bitmap);
            Log.d(TAG, "QR bitmap set");
        } catch (WriterException e) {
            Toast.makeText(this, "Failed generating QR", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "WriterException", e);
        }
    }
}
