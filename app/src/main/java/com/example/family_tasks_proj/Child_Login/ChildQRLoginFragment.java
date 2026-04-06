package com.example.family_tasks_proj.Child_Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginFragment extends Fragment {

    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    private Button btnScanQR;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
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

                if (isBlank(parsed.childId)) {
                    checkParentExists(parsed.parentId);
                } else {
                    checkChildExists(parsed.parentId, parsed.childId);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_q_r_login, container, false);
        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> startQrScan());
        return view;
    }

    private void startQrScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt(getString(R.string.child_qr_scan_prompt));
        barcodeLauncher.launch(options);
    }

    private ParsedQr parseQr(String raw) {
        ParsedQr parsedQr = new ParsedQr();
        if (isBlank(raw)) {
            return parsedQr;
        }

        if (raw.contains("|")) {
            String[] parts = raw.split("\\|");
            for (String part : parts) {
                if (part == null) {
                    continue;
                }

                String trimmed = part.trim();
                if (trimmed.startsWith("parent:")) {
                    parsedQr.parentId = trimmed.substring("parent:".length()).trim();
                } else if (trimmed.startsWith("child:")) {
                    parsedQr.childId = trimmed.substring("child:".length()).trim();
                }
            }
            return parsedQr;
        }

        if (raw.startsWith("parent:")) {
            parsedQr.parentId = raw.substring("parent:".length()).trim();
            return parsedQr;
        }

        if (raw.startsWith("childId:")) {
            parsedQr.childId = raw.substring("childId:".length()).trim();
            return parsedQr;
        }

        parsedQr.childId = raw;
        return parsedQr;
    }

    private void checkParentExists(String parentId) {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            Toast.makeText(requireContext(), R.string.child_qr_parent_not_found, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveSession(parentId, null);
                        openChildSelection(parentId, null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.child_qr_db_error, error.getMessage()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void checkChildExists(String parentId, String childId) {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) {
                            return;
                        }

                        if (!snapshot.exists()) {
                            Toast.makeText(requireContext(), R.string.child_qr_child_not_found, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        saveSession(parentId, childId);
                        openChildSelection(parentId, childId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.child_qr_connection_error, error.getMessage()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openChildSelection(String parentId, String childId) {
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);
        intent.putExtra(KEY_PARENT, parentId);
        if (!isBlank(childId)) {
            intent.putExtra(KEY_CHILD, childId);
        }
        startActivity(intent);
        requireActivity().finish();
    }

    private void saveSession(String parentId, String childId) {
        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit().putString(KEY_PARENT, parentId);

        if (isBlank(childId)) {
            editor.remove(KEY_CHILD);
        } else {
            editor.putString(KEY_CHILD, childId);
        }

        editor.apply();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ParsedQr {
        private String parentId;
        private String childId;
    }
}
