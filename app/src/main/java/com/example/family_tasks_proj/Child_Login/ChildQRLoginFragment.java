// =======================
// ChildQRLoginFragment.java  (FULL + DEBUG + WORKING PARSE)
// package: com.example.family_tasks_proj.Child_Login
// QR payload expected: parent:<PARENT_UID>|child:<CHILD_ID>
// DB path checked: parents/<parentId>/children/<childId>
// =======================
package com.example.family_tasks_proj.Child_Login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginFragment extends Fragment {

    private static final String TAG = "ChildQRLogin";

    private Button btnScanQR;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (!isAdded()) return;

                String raw = result.getContents();
                if (raw == null) {
                    Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                raw = raw.trim();
                Log.d(TAG, "RAW=" + raw);
                Toast.makeText(requireContext(), "RAW=" + raw, Toast.LENGTH_LONG).show();

                ParsedQr parsed = parseQr(raw);

                Log.d(TAG, "parsed.parentId=" + parsed.parentId + ", parsed.childId=" + parsed.childId);

                if (parsed.parentId == null || parsed.parentId.isEmpty()
                        || parsed.childId == null || parsed.childId.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalid QR format", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkChildExists(parsed.parentId, parsed.childId);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_q_r_login, container, false);

        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> startQRScan());

        return view;
    }

    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt("Scan the QR code given by your parent");
        // options.setDesiredBarcodeFormats(ScanOptions.QR_CODE); // optional
        barcodeLauncher.launch(options);
    }

    // ======== QR PARSER ========
    // Supports:
    // 1) parent:XXX|child:YYY  (recommended)
    // 2) childId:YYY           (legacy) -> NOT enough to find parent in new model, returns childId only
    // 3) YYY                   (raw childId) -> returns childId only
    private ParsedQr parseQr(String raw) {
        ParsedQr out = new ParsedQr();

        if (raw == null) return out;
        raw = raw.trim();

        // Recommended format: parent:XXX|child:YYY
        if (raw.contains("|")) {
            String[] parts = raw.split("\\|");
            for (String p : parts) {
                if (p == null) continue;
                p = p.trim();
                if (p.startsWith("parent:")) out.parentId = p.substring("parent:".length()).trim();
                else if (p.startsWith("child:")) out.childId = p.substring("child:".length()).trim();
            }
            return out;
        }

        // Legacy: childId:YYY
        if (raw.startsWith("childId:")) {
            out.childId = raw.substring("childId:".length()).trim();
            return out;
        }

        // Raw id
        out.childId = raw;
        return out;
    }

    private void checkChildExists(String parentId, String childId) {
        String path = "parents/" + parentId + "/children/" + childId;
        Log.d(TAG, "Checking path=" + path);
        Toast.makeText(requireContext(), "CHECK: " + path, Toast.LENGTH_LONG).show();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .child(childId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Log.d(TAG, "snapshot.exists=" + snapshot.exists());

                if (snapshot.exists()) {
                    Intent i = new Intent(requireActivity(), ChildDashboardActivity.class);
                    i.putExtra("parentId", parentId);
                    i.putExtra("childId", childId);
                    startActivity(i);
                    requireActivity().finish();
                } else {
                    Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "NOT FOUND at " + path);
                }



          
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "DB error: " + error.getMessage());
                Toast.makeText(requireContext(), "DB error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // small helper class
    private static class ParsedQr
    {
        String parentId;
        String childId;
    }
}
