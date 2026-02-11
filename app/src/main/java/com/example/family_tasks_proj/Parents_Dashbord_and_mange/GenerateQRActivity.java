package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateQRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        Log.d("QR", "GenerateQRActivity opened");

        ImageView imageViewQrCode = findViewById(R.id.imageViewQrCode);
        if (imageViewQrCode == null) {
            Toast.makeText(this, "ImageView not found (check id)", Toast.LENGTH_SHORT).show();
            Log.e("QR", "imageViewQrCode is null");
            finish();
            return;
        }

        String childId = getIntent().getStringExtra("childId");
        Log.d("QR", "childId = " + childId);

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, "childId missing", Toast.LENGTH_SHORT).show();
            Log.e("QR", "childId missing from Intent");
            finish();
            return;
        }

        // payload ברור כדי שתוכל לפרסר אחר כך בסריקה
        String payload = "childId:" + childId;

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 800, 800);
            imageViewQrCode.setImageBitmap(bitmap);
            Log.d("QR", "QR bitmap set");
        } catch (WriterException e) {
            Toast.makeText(this, "Failed generating QR", Toast.LENGTH_SHORT).show();
            Log.e("QR", "WriterException", e);
        }
    }
}
