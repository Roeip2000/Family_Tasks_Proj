package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך הקצאת משימה לילד.
 *
 * אחריות:
 * - טוען תבניות משימה מ-/parents/{uid}/task_templates.
 * - טוען רשימת ילדים מ-/parents/{uid}/children.
 * - ההורה בוחר תבנית, ילד, ותאריך יעד — ולוחץ "הקצה".
 * - שומר משימה חדשה ב-/parents/{uid}/children/{childId}/tasks/{taskId}.
 *
 * Layout: activity_assign_task_to_child.xml
 *
 * ===== באגים / הערות =====
 * BUG: onCancelled ריק (שורות 163, 209) — שגיאות Firebase נבלעות בשקט.
 *      צריך להוסיף Toast או Log.e כדי שהמפתח ידע על תקלות.
 * BUG: starsWorth קבוע ל-10 — צריך להוסיף שדה/Spinner בטופס כדי שההורה יבחר ערך.
 * TODO: להשתמש ב-ImageHelper.base64ToBitmap() במקום לפענח Base64 ידנית (שכפול קוד).
 * TODO: להוסיף ProgressBar בזמן טעינת תבניות וילדים.
 * TODO: לטפל במקרה שאין תבניות — להציג הודעה "צור תבנית קודם".
 */
public class AssignTaskToChildActivity extends AppCompatActivity {

    private EditText etTitle, etDueDate;
    private Spinner spTemplates, spAssignee;
    private ImageView imgTaskPreview;
    private Button btnAssign;

    /** מזהי ילדים — האינדקס תואם ל-Spinner של spAssignee */
    private final List<String> childrenIds = new ArrayList<>();
    /** תבניות — כל Map מכיל "title" ו-"imageBase64" */
    private final List<Map<String, String>> templatesList = new ArrayList<>();

    private String parentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assign_task_to_child);

        // התאמה לשוליים של מסך (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // חיבור views מה-layout
        etTitle = findViewById(R.id.etTitle);
        etDueDate = findViewById(R.id.etDueDate);
        spTemplates = findViewById(R.id.spTemplates);
        spAssignee = findViewById(R.id.spAssignee);
        imgTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);

        // בדיקה שההורה מחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
        {
            Toast.makeText(this, "Parent not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentUid = user.getUid();

        // טעינת נתונים מ-Firebase
        loadTemplates();
        loadChildren();

        // בחירת תבנית → מעדכן כותרת + תצוגה מקדימה של תמונה
        spTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (position < 0 || position >= templatesList.size()) return;
                Map<String, String> selected = templatesList.get(position);
                etTitle.setText(selected.get("title"));
                displayBase64Image(selected.get("imageBase64"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // בחירת תאריך עם DatePicker
        etDueDate.setOnClickListener(v -> showDatePicker());
        btnAssign.setOnClickListener(v -> assignTask());
    }

    /**
     * טוען תבניות מ-/parents/{parentUid}/task_templates.
     * ממלא את templatesList ואת ה-Spinner.
     *
     * הערה: משתמש ב-addListenerForSingleValueEvent (לא realtime) —
     *        אם ההורה יוצר תבנית חדשה תוך כדי, היא לא תופיע בלי רענון.
     */
    private void loadTemplates()
    {
        templatesList.clear();

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("task_templates")
                .addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        List<String> titles = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren())
                        {
                            String title = ds.child("title").getValue(String.class);
                            String imageBase64 = ds.child("imageBase64").getValue(String.class);

                            if (title == null) title = "";

                            Map<String, String> temp = new HashMap<>();
                            temp.put("title", title);
                            temp.put("imageBase64", imageBase64);

                            templatesList.add(temp);
                            titles.add(title);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AssignTaskToChildActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, titles);

                        spTemplates.setAdapter(adapter);

                        // Spinner לא תמיד מפעיל onItemSelected בפעם הראשונה — מציגים ידנית
                        if (!templatesList.isEmpty())
                        {
                            displayBase64Image(templatesList.get(0).get("imageBase64"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // BUG: שגיאה נבלעת — כדאי להוסיף Toast כאן
                    }
                });
    }

    /**
     * טוען רשימת ילדים מ-/parents/{parentUid}/children.
     * ממלא את childrenIds ואת ה-Spinner.
     */
    private void loadChildren()
    {
        childrenIds.clear();

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        List<String> names = new ArrayList<>();

                        for (DataSnapshot childSnap : snapshot.getChildren())
                        {
                            String childId = childSnap.getKey();
                            if (childId == null) continue;

                            String firstName = childSnap.child("firstName").getValue(String.class);
                            String lastName = childSnap.child("lastName").getValue(String.class);

                            // בניית שם תצוגה
                            String displayName = (firstName != null ? firstName : "");
                            if (lastName != null && !lastName.trim().isEmpty())
                            {
                                displayName = displayName + " " + lastName;
                            }

                            names.add(displayName.trim().isEmpty() ? childId : displayName);
                            childrenIds.add(childId);
                        }

                        spAssignee.setAdapter(new ArrayAdapter<>(AssignTaskToChildActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, names));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // BUG: שגיאה נבלעת — כדאי להוסיף Toast כאן
                    }
                });
    }

    /**
     * מאמת קלט, בונה אובייקט משימה, ושומר ב-Firebase.
     *
     * נתיב כתיבה: /parents/{parentUid}/children/{childId}/tasks/{taskId}
     * Side-effect: סוגר את ה-Activity בהצלחה.
     *
     * שדות המשימה: title, dueAt, isDone, starsWorth, imageBase64, createdAt
     */
    private void assignTask()
    {
        int childPos = spAssignee.getSelectedItemPosition();
        String date = etDueDate.getText().toString().trim();

        // ולידציה — חייבים ילד ותאריך
        if (childPos == -1 || childPos >= childrenIds.size() || date.isEmpty())
        {
            Toast.makeText(this, "יש לבחור ילד ותאריך", Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = childrenIds.get(childPos);

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty())
        {
            Toast.makeText(this, "יש למלא כותרת", Toast.LENGTH_SHORT).show();
            return;
        }

        // שליפת תמונה מהתבנית שנבחרה (אם יש)
        String imageBase64 = null;
        int templatePos = spTemplates.getSelectedItemPosition();
        if (templatePos >= 0 && templatePos < templatesList.size())
        {
            imageBase64 = templatesList.get(templatePos).get("imageBase64");
        }

        // יצירת מזהה ייחודי למשימה
        String taskId = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .child(childId)
                .child("tasks")
                .push()
                .getKey();

        if (taskId == null)
        {
            Toast.makeText(this, "שגיאה ביצירת מזהה משימה", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית אובייקט המשימה כ-HashMap
        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("dueAt", date);
        task.put("isDone", false);
        task.put("starsWorth", 10); // TODO: לאפשר להורה לקבוע ערך
        task.put("imageBase64", imageBase64);
        task.put("createdAt", System.currentTimeMillis());

        // שמירה ב-Firebase
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .child(childId)
                .child("tasks")
                .child(taskId)
                .setValue(task)
                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(this, "המשימה הוקצתה בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish(); // חוזר למסך הקודם
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * מפענח מחרוזת Base64 ומציג כ-Bitmap ב-imgTaskPreview.
     *
     * TODO: להשתמש ב-ImageHelper.base64ToBitmap() במקום — מונע שכפול קוד.
     */
    private void displayBase64Image(String base64)
    {
        if (base64 == null || base64.isEmpty()) return;
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        imgTaskPreview.setImageBitmap(bitmap);
    }

    /** פותח DatePickerDialog וכותב את התאריך הנבחר ל-etDueDate בפורמט d/M/yyyy. */
    private void showDatePicker()
    {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (DatePicker view, int y, int m, int d) ->
                etDueDate.setText(d + "/" + (m + 1) + "/" + y),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
