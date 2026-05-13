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

/** מסך סריקת QR עבור הילד להתחברות מהירה. */
public class ChildQRLoginFragment extends Fragment {

    private Button btnScanQR;

    // אובייקט לניהול פתיחת המצלמה וקבלת תוצאת הסריקה
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

    // אובייקט לבקשת הרשאת המצלמה מהמשתמש
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

    // פותח את מסך הסריקה רק אם הרשאת המצלמה ניתנה
    private void startQrScan() {
        int status = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
        if (status == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // פותח את הסורק עצמו
    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt(getString(R.string.child_qr_scan_prompt));
        barcodeLauncher.launch(options);
    }

    // מטפל בתוצאה שהתקבלה מהמצלמה
    private void handleQrScanResult(ScanIntentResult result) {
        if (!isAdded()) {
            return;
        }

        String raw = result.getContents();
        if (raw == null) {
            Toast.makeText(requireContext(), R.string.child_qr_scan_cancelled, Toast.LENGTH_SHORT).show();
            return;
        }
        ParsedQr parsed = parseQr(raw.trim());
        if (isBlank(parsed.parentId)) {
            Toast.makeText(requireContext(), R.string.child_qr_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        checkQrTarget(parsed);
    }

    // בודק לאן להפנות את המשתמש לפי תוכן ה-QR
    private void checkQrTarget(ParsedQr parsed) {
        checkParentExists(parsed.parentId);
    }

    // מפרק את הטקסט של ה-QR לנתונים
    private ParsedQr parseQr(String raw) {
        ParsedQr parsedQr = new ParsedQr();
        if (isBlank(raw)) {
            return parsedQr;
        }

        if (raw.startsWith("parent:")) {
            parsedQr.parentId = raw.substring("parent:".length()).trim();
        }

        return parsedQr;
    }

    // בודק מול Firebase שההורה קיים במערכת
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
                // מעבר ל־ChildSelectionActivity
                openChildSelection(parentId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), getString(R.string.child_qr_db_error, error.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    // פותח את מסך בחירת הילד כשנסרק QR של הורה בלבד
    private void openChildSelection(String parentId) {
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);
        intent.putExtra(ChildSelectionActivity.EXTRA_PARENT_ID, parentId);
        startActivity(intent);
        requireActivity().finish();
    }

    // בודק אם מחרוזת היא ריקה
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // אובייקט עזר לשמירת נתוני ה-QR שפוענחו בזמן הסריקה בלבד
    private static class ParsedQr {
        private String parentId;
    }
}
