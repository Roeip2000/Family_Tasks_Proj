package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.family_tasks_proj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignTaskToChildActivity extends AppCompatActivity {

    private EditText etTitle, etDueDate;
    private Spinner spTemplates, spAssignee;
    private ImageView imgTaskPreview;
    private Button btnAssign;

    private List<String> childrenIds = new ArrayList<>();
    private List<Map<String, String>> templatesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_assign_task_to_child);

        // הגדרת Insets כפי שיצרת
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->

        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // אתחול רכיבים (וודא שה-IDs תואמים ל-XML)
        etTitle = findViewById(R.id.etTitle);

        etDueDate = findViewById(R.id.etDueDate);

        spTemplates = findViewById(R.id.spTemplates);

        spAssignee = findViewById(R.id.spAssignee);

        imgTaskPreview = findViewById(R.id.imgTaskPreview);

        btnAssign = findViewById(R.id.btnAssign);

        // טעינת נתונים מ-Firebase
        loadTemplates();
        loadChildren();

        // מאזין לבחירת תבנית - מעדכן כותרת ותמונה אוטומטית
        spTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()


        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Map<String, String> selected = templatesList.get(position);
                etTitle.setText(selected.get("title"));
                displayBase64Image(selected.get("imageBase64"));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // בחירת תאריך
        etDueDate.setOnClickListener(v -> showDatePicker());

        // כפתור ביצוע הקצאה
        btnAssign.setOnClickListener(v -> assignTask());
    }

    private void loadTemplates()
    {
        FirebaseDatabase.getInstance().getReference("task_templates")


                .addListenerForSingleValueEvent(new ValueEventListener()
                {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        List<String> titles = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Map<String, String> temp = new HashMap<>();
                            temp.put("title", ds.child("title").getValue(String.class));
                            temp.put("imageBase64", ds.child("imageBase64").getValue(String.class));
                            templatesList.add(temp);
                            titles.add(temp.get("title"));
                        }


                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AssignTaskToChildActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, titles);

                        spTemplates.setAdapter(adapter);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}

                });
    }

    private void loadChildren()
    {
        FirebaseDatabase.getInstance().getReference("users").get().addOnSuccessListener(snapshot ->
        {

            List<String> names = new ArrayList<>();


            for (DataSnapshot userSnap : snapshot.getChildren())
            {

                if (userSnap.hasChild("qrCode"))

                {
                    // זיהוי ילד לפי ה-QR כפי שמופיע בחוברת
                    names.add(userSnap.child("firstName").getValue(String.class));
                    childrenIds.add(userSnap.getKey());
                }
            }
            spAssignee.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        });
    }

    private void assignTask()
    {
        int childPos = spAssignee.getSelectedItemPosition();
        String date = etDueDate.getText().toString();

        if (childPos == -1 || date.isEmpty()) {

            Toast.makeText(this, "יש לבחור ילד ותאריך", Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = childrenIds.get(childPos);

        Map<String, Object> task = new HashMap<>();

        task.put("title", etTitle.getText().toString());

        task.put("dueDate", date);

        task.put("completed", false); // משימה חדשה תמיד מתחילה כלא הושלמה

        task.put("imageBase64", templatesList.get(spTemplates.getSelectedItemPosition()).get("imageBase64"));

        task.put("starReward", 10); // ניקוד בסיסי

        // שמירה תחת המשתמש של הילד ב-Firebase
        FirebaseDatabase.getInstance().getReference("users")
                .child(childId).child("tasks").push().setValue(task)
                .addOnSuccessListener(aVoid ->
                {

                    Toast.makeText(this, "המשימה הוקצתה בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();

                });
    }


    private void displayBase64Image(String base64)
    {
        if (base64 == null || base64.isEmpty()) return;
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        imgTaskPreview.setImageBitmap(bitmap);
    }

    private void showDatePicker()
    {

        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, y, m, d) -> etDueDate.setText(d + "/" + (m + 1) + "/" + y),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}