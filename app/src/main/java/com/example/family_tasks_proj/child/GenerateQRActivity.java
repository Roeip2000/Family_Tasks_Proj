package com.example.family_tasks_proj.child;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateQRActivity extends AppCompatActivity {

    private static final String TAG = "GenerateQRActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        String childId = getIntent().getStringExtra("childId");

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Error: Child ID is missing.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Child ID is null or empty.");
            finish(); // Close the activity if no ID is provided
            return;
        }

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(childId, BarcodeFormat.QR_CODE, 600, 600);
            ImageView imageViewQrCode = findViewById(R.id.imageViewQrCode);
            imageViewQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code", e);
            Toast.makeText(this, "Could not generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }
}
