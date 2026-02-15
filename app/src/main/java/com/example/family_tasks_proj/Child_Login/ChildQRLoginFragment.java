package com.example.family_tasks_proj.Child_Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * מסך התחברות ילד באמצעות סריקת QR.
 *
 * אחריות:
 * - פותח סורק QR (ספריית ZXing).
 * - מפענח את המחרוזת בפורמט "parent:{id}|child:{id}".
 * - מוודא מול Firebase שהילד קיים תחת ההורה.
 * - שומר סשן ב-SharedPreferences ופותח את ChildSelectionActivity (מסך בחירת ילד).
 *
 * Layout: fragment_child_q_r_login.xml
 *
 * ===== תהליך מלא =====
 * 1. ילד לוחץ "סרוק QR" → נפתח סורק מצלמה
 * 2. סורק את ה-QR שההורה הציג ב-GenerateQRActivity
 * 3. מפענח: "parent:XXX|child:YYY" → חילוץ parentId + childId
 * 4. בדיקה ב-Firebase שהילד קיים תחת ההורה
 * 5. שמירת סשן ב-SharedPreferences (כדי שלא יצטרך לסרוק שוב)
 * 6. פתיחת ChildSelectionActivity — מסך בחירת ילד עם Spinner
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף הנפשה/טעינה בזמן בדיקת Firebase (בין סריקה לפתיחת דשבורד).
 * TODO: לטפל בהרשאות מצלמה (CAMERA permission) — כרגע ZXing מטפל בזה.
 */
public class ChildQRLoginFragment extends Fragment {

    private static final String TAG = "ChildQRLogin";

    // מפתחות SharedPreferences לסשן הילד
    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    private Button btnScanQR;

    /**
     * Launcher לסריקת QR — משתמש ב-ActivityResult API (לא deprecated onActivityResult).
     * ה-callback מקבל את תוצאת הסריקה ומעבד אותה.
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                // בדיקה שה-Fragment עדיין מחובר — מונע crash
                if (!isAdded()) return;

                String raw = result.getContents();
                if (raw == null) {
                    // המשתמש ביטל את הסריקה
                    Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                raw = raw.trim();
                Log.d(TAG, "RAW=" + raw);

                // פענוח המחרוזת
                ParsedQr parsed = parseQr(raw);

                Log.d(TAG, "parsed.parentId=" + parsed.parentId + ", parsed.childId=" + parsed.childId);

                // ולידציה — חייבים לפחות parentId
                if (parsed.parentId == null || parsed.parentId.isEmpty()) {
                    Toast.makeText(requireContext(), "QR לא תקין", Toast.LENGTH_SHORT).show();
                    return;
                }

                // אם יש childId — בודקים שהוא קיים, אחרת עוברים לבחירת ילד
                if (parsed.childId != null && !parsed.childId.isEmpty()) {
                    checkChildExists(parsed.parentId, parsed.childId);
                } else {
                    // QR קבוע להורה — בודקים שההורה קיים ועוברים לבחירת ילד
                    checkParentExists(parsed.parentId);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_q_r_login, container, false);

        btnScanQR = view.findViewById(R.id.btnScanQR);
        btnScanQR.setOnClickListener(v -> startQRScan());

        return view;
    }

    /** פותח את מצלמת הסורק עם ZXing. */
    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false); // מאפשר סיבוב מסך
        options.setPrompt("Scan the QR code given by your parent");
        barcodeLauncher.launch(options);
    }

    /**
     * מפענח מחרוזת QR לאובייקט ParsedQr.
     *
     * פורמטים נתמכים:
     * 1. "parent:XXX|child:YYY" — הפורמט הנדרש (תואם ל-GenerateQRActivity)
     * 2. "childId:YYY" — legacy/ישן, ייכשל בוולידציה כי חסר parentId
     * 3. טקסט חופשי — legacy/ישן, ייכשל בוולידציה
     *
     * @return ParsedQr עם parentId ו-childId (אחד או שניהם יכולים להיות null)
     */
    private ParsedQr parseQr(String raw) {
        ParsedQr out = new ParsedQr();

        if (raw == null) return out;
        raw = raw.trim();

        // פורמט מלא: parent:XXX|child:YYY (תאימות אחורה)
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

        // פורמט חדש: parent:XXX (QR קבוע להורה — בלי childId)
        if (raw.startsWith("parent:")) {
            out.parentId = raw.substring("parent:".length()).trim();
            return out;
        }

        // פורמט legacy — לתאימות אחורה
        if (raw.startsWith("childId:")) {
            out.childId = raw.substring("childId:".length()).trim();
            return out;
        }

        out.childId = raw;
        return out;
    }

    /**
     * בודק ב-Firebase שההורה קיים ב-/parents/{parentId}.
     * משמש ל-QR קבוע (פורמט "parent:XXX" בלי childId).
     * אם קיים — שומר סשן ופותח מסך בחירת ילד.
     */
    private void checkParentExists(String parentId) {
        Log.d(TAG, "Checking parent exists: " + parentId);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    // הורה קיים — שומרים סשן ופותחים מסך בחירת ילד
                    saveSession(parentId, null);

                    Intent i = new Intent(requireActivity(), ChildSelectionActivity.class);
                    i.putExtra("parentId", parentId);
                    startActivity(i);
                    requireActivity().finish();
                } else {
                    Toast.makeText(requireContext(), "QR לא תקין — הורה לא נמצא", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Parent NOT FOUND: " + parentId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e(TAG, "DB error: " + error.getMessage());
                Toast.makeText(requireContext(), "שגיאת DB: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * בודק ב-Firebase שהילד קיים תחת /parents/{parentId}/children/{childId}.
     * אם כן — שומר סשן ופותח מסך בחירת ילד (ChildSelectionActivity).
     * אם לא — מציג הודעת שגיאה.
     */
    private void checkChildExists(String parentId, String childId) {
        String path = "parents/" + parentId + "/children/" + childId;
        Log.d(TAG, "Checking path=" + path);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .child(childId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Fragment לא מחובר — לא עושים כלום

                Log.d(TAG, "snapshot.exists=" + snapshot.exists());

                if (snapshot.exists()) {
                    // הילד קיים — שומרים סשן ופותחים מסך בחירת ילד (Spinner)
                    // ה-childId מועבר כ-preselection — ייבחר אוטומטית ב-Spinner
                    saveSession(parentId, childId);

                    Intent i = new Intent(requireActivity(), ChildSelectionActivity.class);
                    i.putExtra("parentId", parentId);
                    i.putExtra("childId", childId);
                    startActivity(i);
                    requireActivity().finish(); // סוגר את MainActivity
                } else {
                    // הילד לא נמצא — QR לא תקין
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

    /**
     * שומר parentId + childId ב-SharedPreferences לסשן עתידי.
     * כך הילד יכול לחזור לדשבורד בלי לסרוק QR שוב.
     */
    private void saveSession(String parentId, String childId) {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit()
                .putString(KEY_PARENT, parentId);
        // שומר childId רק אם קיים — בפורמט QR חדש הוא null
        if (childId != null) {
            editor.putString(KEY_CHILD, childId);
        }
        editor.apply();
        Log.d(TAG, "Session saved: parentId=" + parentId + " childId=" + childId);
    }

    /** מחלקה פנימית — תוצאת פענוח QR. מחזיקה parentId ו-childId. */
    private static class ParsedQr {
        String parentId;
        String childId;
    }
}
