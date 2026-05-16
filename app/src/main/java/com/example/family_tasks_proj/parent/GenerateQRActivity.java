package com.example.family_tasks_proj.parent;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateQRActivity extends AppCompatActivity {

    private ImageView imageViewQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        imageViewQrCode = findViewById(R.id.imageViewQrCode);
        Button btnBack = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        generateParentQR(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private static final int QR_CODE_SIZE = 800;

    // ה-QR מכיל את מזהה ההורה המחובר
    private void generateParentQR(String parentId) {
        String payload = parentId;

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(
                    payload, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            imageViewQrCode.setVisibility(View.VISIBLE);
            imageViewQrCode.setImageBitmap(bitmap);
        } catch (Exception exception) {
            imageViewQrCode.setVisibility(View.GONE);
        }
    }
}
