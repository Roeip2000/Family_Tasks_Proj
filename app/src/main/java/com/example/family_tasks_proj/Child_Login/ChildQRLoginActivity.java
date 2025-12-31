package com.example.family_tasks_proj.Child_Login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.Child_Dashbord_mange.ChildDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginActivity extends AppCompatActivity {

    Button btnScanQR;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(ChildQRLoginActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    checkChildExists(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btnScanQR = findViewById(R.id.btnScanQR);

        btnScanQR.setOnClickListener(v -> startQRScan());
    }

    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt("Scan the QR code given by your parent");
        barcodeLauncher.launch(options);
    }

    private void checkChildExists(String childId) {
        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("child_to_parent");

        ref.child(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Intent i = new Intent(
                            ChildQRLoginActivity.this,
                            ChildDashboardActivity.class
                    );
                    i.putExtra("childId", childId);
                    i.putExtra("parentId", snapshot.getValue(String.class));
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(ChildQRLoginActivity.this,
                            "Invalid QR code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChildQRLogin", "Database error: " + error.getMessage());
                Toast.makeText(ChildQRLoginActivity.this, "Database error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
