package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ניהול QR להורה — עם Spinner לבחירת ילד.
 *
 * אחריות:
 * - טוען את כל ילדי ההורה המחובר מ-Firebase (/parents/{uid}/children).
 * - מציג אותם ב-Spinner (רשימה נפתחת).
 * - כשההורה בוחר ילד — מייצר ומציג QR מיידית.
 * - ה-Spinner מתעדכן אוטומטית אם ילדים נוספים/נמחקים (ValueEventListener).
 *
 * פורמט ה-QR: "parent:{parentId}|child:{childId}"
 * פורמט זהה לזה שה-ChildQRLoginFragment מפענח.
 *
 * תומך גם בפתיחה ישירה עם Intent extras (childId, childName)
 * מ-ManageChildrenActivity — במקרה הזה בוחר אוטומטית את הילד ב-Spinner.
 *
 * Layout: activity_generate_qr.xml
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף כפתור "שתף QR" — לשמור/לשלוח את תמונת ה-QR.
 * TODO: לשמור את הבחירה ב-onSaveInstanceState כדי לשרוד סיבוב מסך.
 */
public class GenerateQRActivity extends AppCompatActivity {

    private static final String TAG = "QR";

    private Spinner spChildren;
    private TextView tvChildLabel, tvNoChildren;
    private ProgressBar progressBar;
    private ImageView imageViewQrCode;

    /** UID של ההורה המחובר */
    private String parentId;

    /** מזהי ילדים — האינדקס תואם ל-Spinner */
    private final List<String> childIds = new ArrayList<>();
    /** שמות ילדים לתצוגה ב-Spinner */
    private final List<String> childNames = new ArrayList<>();

    /** Listener שמתעדכן בזמן אמת כשילדים נוספים/נמחקים */
    private ValueEventListener childrenListener;
    private DatabaseReference childrenRef;

    /** childId שהתקבל מ-Intent — כדי לבחור אוטומטית ב-Spinner */
    private String intentChildId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // חיבור views מה-layout
        spChildren = findViewById(R.id.spChildren);
        tvChildLabel = findViewById(R.id.tvChildLabel);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        progressBar = findViewById(R.id.progressBar);
        imageViewQrCode = findViewById(R.id.imageViewQrCode);
        Button btnBack = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> finish());

        // זיהוי ההורה המחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Parent not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentId = user.getUid();

        // אם הגענו מ-ManageChildrenActivity עם childId ספציפי
        intentChildId = getIntent().getStringExtra("childId");

        // הגדרת Spinner listener — בכל בחירת ילד מייצרים QR חדש
        spChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= childIds.size()) return;
                String selectedChildId = childIds.get(position);
                String selectedChildName = childNames.get(position);

                showQrForChild(selectedChildId, selectedChildName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                imageViewQrCode.setVisibility(View.GONE);
                tvChildLabel.setText("QR Code");
            }
        });

        // טעינת ילדים עם listener בזמן אמת — מתעדכן אוטומטית
        loadChildrenRealtime();
    }

    /**
     * טוען את רשימת הילדים מ-Firebase עם ValueEventListener.
     * ה-listener נרשם כ-addValueEventListener (לא SingleValue) —
     * כך כל שינוי ברשימת הילדים (הוספה/מחיקה) מעדכן אוטומטית את ה-Spinner.
     */
    private void loadChildrenRealtime() {
        childrenRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children");

        progressBar.setVisibility(View.VISIBLE);

        childrenListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ניקוי רשימות ישנות
                childIds.clear();
                childNames.clear();

                // מעבר על כל ילד — שליפת שם ומזהה
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String cId = childSnap.getKey();
                    if (cId == null) continue;

                    String firstName = childSnap.child("firstName").getValue(String.class);
                    String lastName = childSnap.child("lastName").getValue(String.class);

                    // בניית שם תצוגה — fallback למזהה אם אין שם
                    String displayName = (firstName != null ? firstName : "");
                    if (lastName != null && !lastName.trim().isEmpty()) {
                        displayName = displayName + " " + lastName;
                    }
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) displayName = cId;

                    childIds.add(cId);
                    childNames.add(displayName);
                }

                progressBar.setVisibility(View.GONE);

                if (childIds.isEmpty()) {
                    // אין ילדים — מציג הודעה, מחביא Spinner ו-QR
                    tvNoChildren.setVisibility(View.VISIBLE);
                    imageViewQrCode.setVisibility(View.GONE);
                    spChildren.setVisibility(View.GONE);
                    tvChildLabel.setText("QR Code");
                    return;
                }

                tvNoChildren.setVisibility(View.GONE);
                spChildren.setVisibility(View.VISIBLE);

                // עדכון ה-Spinner עם שמות הילדים
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        GenerateQRActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        new ArrayList<>(childNames));
                spChildren.setAdapter(adapter);

                // אם הגענו עם childId מ-Intent — בוחרים אותו אוטומטית ב-Spinner
                if (intentChildId != null && !intentChildId.isEmpty()) {
                    int idx = childIds.indexOf(intentChildId);
                    if (idx >= 0) {
                        spChildren.setSelection(idx);
                    }
                    // ננקה כדי שעדכונים עתידיים לא יאלצו בחירה מחדש
                    intentChildId = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GenerateQRActivity.this,
                        "Failed loading children: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "loadChildren error: " + error.getMessage());
            }
        };

        childrenRef.addValueEventListener(childrenListener);
    }

    /**
     * מייצר ומציג QR עבור הילד שנבחר.
     *
     * @param childId   מזהה הילד ב-Firebase
     * @param childName שם הילד לתצוגה מעל ה-QR
     */
    private void showQrForChild(String childId, String childName) {
        // בניית payload בפורמט שה-ChildQRLoginFragment מצפה לו
        String payload = "parent:" + parentId + "|child:" + childId;
        tvChildLabel.setText("QR עבור " + childName);

        Log.d(TAG, "payload=" + payload);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 800, 800);

            imageViewQrCode.setVisibility(View.VISIBLE);
            imageViewQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            imageViewQrCode.setVisibility(View.GONE);
            Toast.makeText(this, "Failed generating QR", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "WriterException", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ניקוי listener כדי למנוע דליפת זיכרון (memory leak)
        if (childrenRef != null && childrenListener != null) {
            childrenRef.removeEventListener(childrenListener);
        }
    }
}
