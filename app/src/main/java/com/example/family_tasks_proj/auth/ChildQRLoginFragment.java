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

    private Button btnScanQR;

    // מאזין לתוצאה מחלון סריקת ה-QR
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    new ActivityResultCallback<ScanIntentResult>() {
                        @Override
                        public void onActivityResult(ScanIntentResult result) {
                            handleQrScanResult(result);
                        }
                    }
            );

    // מאזין לבקשת הרשאת גישה למצלמה
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean granted) {
                            if (granted != null && granted) {
                                launchScanner();
                            } else {
                                Toast.makeText(requireContext(), R.string.child_qr_camera_denied, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_q_r_login, container, false);
        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQrScan();
            }
        });
        return view;
    }

    private void startQrScan() {
        int status = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
        if (status == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt(getString(R.string.child_qr_scan_prompt));
        barcodeLauncher.launch(options);
    }

    private void handleQrScanResult(ScanIntentResult result) {
        if (!isAdded()) {
            return;
        }

        String qrContent = result.getContents();
        if (qrContent == null) {
            Toast.makeText(requireContext(), R.string.child_qr_scan_cancelled, Toast.LENGTH_SHORT).show();
            return;
        }

        // מחלץ את מזהה ההורה (UID) מה-QR לפי הפורמט שקבענו: parent:UID
        String parentId = parseParentId(qrContent.trim());
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(requireContext(), R.string.child_qr_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        checkParentExists(parentId);
    }

    private String parseParentId(String qrContent) {
        if (qrContent == null || qrContent.isEmpty()) {
            return "";
        }
        if (qrContent.startsWith("parent:")) {
            return qrContent.substring("parent:".length()).trim();
        }
        return "";
    }

    // מוודא שההורה מה-QR אכן קיים ב-Firebase לפני שעוברים למסך הבחירה
    private void checkParentExists(final String parentId) {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), R.string.child_qr_parent_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(requireContext(), "סריקת QR תקינה", Toast.LENGTH_SHORT).show();
                openChildSelection(parentId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChildSelection(String parentId) {
        // מעביר את ה-UID של ההורה למסך הבא (בחירת ילד) בעזרת Intent
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);
        intent.putExtra(ChildSelectionActivity.EXTRA_PARENT_ID, parentId);
        startActivity(intent);
        requireActivity().finish();
    }
}
