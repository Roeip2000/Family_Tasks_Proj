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

import androidx.activity.result.ActivityResultCallback;
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
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * מסך סריקת QR לילד.
 * תומך ב-QR של הורה בלבד: parent:{parentId},
 * וגם ב-QR מלא: parent:{parentId}|child:{childId}.
 * כל בדיקה מול Firebase נשארת תחת /parents/{parentId}.
 */
public class ChildQRLoginFragment extends Fragment {

    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    private Button btnScanQR;

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

    // יוצר את ה-layout ומחבר את כפתור הסריקה
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

    // מפעיל את סורק ה-QR של ZXing
    private void startQrScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setPrompt(getString(R.string.child_qr_scan_prompt));
        barcodeLauncher.launch(options);
    }

    // מטפל בתוצאה שחזרה מהסורק
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

    // מחליט אם ה-QR מוביל לבחירת ילד או לדשבורד ילד ישיר
    private void checkQrTarget(ParsedQr parsed) {
        if (isBlank(parsed.childId)) {
            checkParentExists(parsed.parentId);
        } else {
            checkChildExists(parsed.parentId, parsed.childId);
        }
    }

    // מפענח את הטקסט מה-QR למזהה הורה ומזהה ילד אם קיים
    private ParsedQr parseQr(String raw) {
        ParsedQr parsedQr = new ParsedQr();
        if (isBlank(raw)) {
            return parsedQr;
        }

        if (raw.contains("|")) {
            fillParsedQrFromParts(parsedQr, raw);
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

    // מפענח פורמט מלא: parent:{id}|child:{id}
    private void fillParsedQrFromParts(ParsedQr parsedQr, String raw) {
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
    }

    // בודק שענף ההורה קיים ב-Firebase: /parents/{parentId}
    private void checkParentExists(final String parentId) {
        DatabaseReference parentRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId);

        parentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleParentSnapshot(parentId, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showParentCheckError(error);
            }
        });
    }

    // מטפל בתוצאת בדיקת ההורה מ-Firebase
    private void handleParentSnapshot(String parentId, DataSnapshot snapshot) {
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

    // מציג שגיאת Firebase בזמן בדיקת ההורה
    private void showParentCheckError(DatabaseError error) {
        if (!isAdded()) {
            return;
        }

        Toast.makeText(
                requireContext(),
                getString(R.string.child_qr_db_error, error.getMessage()),
                Toast.LENGTH_LONG
        ).show();
    }

    // בודק שילד ספציפי קיים ב-Firebase: /parents/{parentId}/children/{childId}
    private void checkChildExists(final String parentId, final String childId) {
        DatabaseReference childRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .child(childId);

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleChildSnapshot(parentId, childId, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showChildCheckError(error);
            }
        });
    }

    // מטפל בתוצאת בדיקת הילד מ-Firebase
    private void handleChildSnapshot(String parentId, String childId, DataSnapshot snapshot) {
        if (!isAdded()) {
            return;
        }

        if (!snapshot.exists()) {
            Toast.makeText(requireContext(), R.string.child_qr_child_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        saveSession(parentId, childId);
        openChildDashboard(parentId, childId);
    }

    // מציג שגיאת Firebase בזמן בדיקת הילד
    private void showChildCheckError(DatabaseError error) {
        if (!isAdded()) {
            return;
        }

        Toast.makeText(
                requireContext(),
                getString(R.string.child_qr_connection_error, error.getMessage()),
                Toast.LENGTH_LONG
        ).show();
    }

    // פותח דשבורד ילד עם שני extras: parentId ו-childId
    private void openChildDashboard(String parentId, String childId) {
        Intent intent = new Intent(requireActivity(), ChildDashboardActivity.class);
        // parentId אומר לדשבורד מאיזה הורה לקרוא את ענף הילדים
        intent.putExtra(KEY_PARENT, parentId);
        // childId אומר לדשבורד איזה ילד לפתוח תחת אותו הורה
        intent.putExtra(KEY_CHILD, childId);
        startActivity(intent);
        requireActivity().finish();
    }

    // פותח בחירת ילד כאשר ה-QR זיהה רק את ההורה
    private void openChildSelection(String parentId, String childId) {
        Intent intent = new Intent(requireActivity(), ChildSelectionActivity.class);
        // parentId נחסך מהילד במסך הבא כדי שלא יצטרך לבחור הורה ידנית
        intent.putExtra(KEY_PARENT, parentId);
        if (!isBlank(childId)) {
            intent.putExtra(KEY_CHILD, childId);
        }
        startActivity(intent);
        requireActivity().finish();
    }

    // שומר סשן ילד מקומי ב-SharedPreferences כדי לאפשר כניסה מהירה בפעם הבאה
    private void saveSession(String parentId, String childId) {
        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PARENT, parentId);

        if (isBlank(childId)) {
            editor.remove(KEY_CHILD);
        } else {
            editor.putString(KEY_CHILD, childId);
        }

        editor.apply();
    }

    // בודק null או מחרוזת ריקה אחרי trim
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * מחזיק את תוצאת פענוח ה-QR בצורה פשוטה.
     */
    private static class ParsedQr {
        private String parentId;
        private String childId;
    }
}
