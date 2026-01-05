package com.example.family_tasks_proj.child;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ChildQRLoginFragment extends Fragment {

    private Button btnScanQR;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toast.makeText(requireContext(),
                            "Cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    checkChildExists(result.getContents());
                }
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_child_q_r_login,
                container,
                false
        );

        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> startQRScan());

        return view;
    }

    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt("Scan the QR code given by your parent");
        barcodeLauncher.launch(options);
    }

    private void checkChildExists(String childId) {
        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("child_to_parent");

        ref.child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Intent i = new Intent(
                                    requireActivity(),
                                    ChildDashboardActivity.class
                            );
                            i.putExtra("childId", childId);
                            i.putExtra("parentId",
                                    snapshot.getValue(String.class));
                            startActivity(i);
                            requireActivity().finish();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Invalid QR code",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChildQRLogin",
                                "Database error: " + error.getMessage());
                        Toast.makeText(requireContext(),
                                "Database error",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
