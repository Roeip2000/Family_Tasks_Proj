package com.example.family_tasks_proj.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginFragment extends Fragment {

    // בקשת הרשאת מצלמה לפני פתיחת סורק ה-QR
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean granted) {
                    if (granted)
                    {
                        launchScanner();
                    } else
                    {
                        Toast.makeText(requireContext(), R.string.child_qr_camera_denied, Toast.LENGTH_LONG).show();
                    }
                }
            });

    // הפעלת סורק QR וקבלת תוצאת הסריקה
    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), new ActivityResultCallback<ScanIntentResult>() {
                @Override
                public void onActivityResult(ScanIntentResult result) {
                    handleQrScanResult(result);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        // יצירת מסך ה-QR וחיבור כפתור הסריקה
        View view = inflater.inflate(R.layout.fragment_child_q_r_login, container, false);
        Button btnScanQR = view.findViewById(R.id.btnScanQR);

        // לחיצה על הכפתור מתחילה את תהליך הסריקה
        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQrScan();
            }
        });

        return view;
    }

    private void startQrScan() {
        // בודקים אם כבר יש הרשאת מצלמה
        int status = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);

        if (status == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner()
    {
        // פתיחת מסך סריקת ה-QR
        ScanOptions options = new ScanOptions();
        qrScannerLauncher.launch(options);
    }

    private void handleQrScanResult(ScanIntentResult result) {
        String qrText = result.getContents();


        // אם המשתמש ביטל את הסריקה לא ממשיכים
        if (qrText == null) {
            return;
        }

        // הטקסט שנסרק מה-QR הוא מזהה ההורה
        String parentId = qrText.trim();
        if (parentId.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_action_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        checkParentExists(parentId);
    }

    private void checkParentExists(String parentId) {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            openChildSelection(parentId);
                        } else {
                            Toast.makeText(requireContext(), R.string.error_action_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), R.string.error_action_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openChildSelection(String parentId) {
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);
        // מעבירים את מזהה ההורה למסך בחירת הילד
        intent.putExtra("parentId", parentId);
        startActivity(intent);
        requireActivity().finish();
    }
}
