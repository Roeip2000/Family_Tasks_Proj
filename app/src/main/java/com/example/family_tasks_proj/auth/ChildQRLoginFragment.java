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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginFragment extends Fragment {

    private Button btnScanQR;

    // מאזין שמחכה לתשובה - האם המשתמש אישר את הרשאת המצלמה?
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

    // מאזין שמחכה לטקסט שחוזר מסורק ה-QR לאחר הסריקה
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), new ActivityResultCallback<ScanIntentResult>() {
                @Override
                public void onActivityResult(ScanIntentResult result
                ) {
                    handleQrScanResult(result);
                }
            });


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
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

    private void startQrScan()
    {
        int status = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);

        if (status == PackageManager.PERMISSION_GRANTED)
        {
            launchScanner();
        } else
        {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner()
    {
        ScanOptions options = new ScanOptions();
        barcodeLauncher.launch(options);
    }

    private void handleQrScanResult(ScanIntentResult result)
    {
        String raw = result.getContents();

        if (raw == null)
        {
            return;
        }

        String parentId = parseParentId(raw.trim());


        if (parentId.isEmpty())
        {
            Toast.makeText(requireContext(), "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
            return;
        }

        openChildSelection(parentId);
    }

    // מחלץ את מזהה ההורה מהפורמט parent:{uid}
    private String parseParentId(String raw)
    {
        if (raw.isEmpty()) {
            return "";
        }
        if (raw.startsWith("parent:"))
        {
            return raw.substring("parent:".length()).trim();
        }
        return "";
    }

    private void openChildSelection(String parentId)
    {
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);

        intent.putExtra("parentId", parentId);
        startActivity(intent);
        requireActivity().finish();
    }
}
