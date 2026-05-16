package com.example.family_tasks_proj.parent;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.models.ChildTask;
import com.example.family_tasks_proj.models.TaskTemplate;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.utils.ImageHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AssignTaskToChildActivity extends AppCompatActivity {

    private EditText etTaskTitle, etTaskDueDate;
    private Spinner spinnerTemplates, spinnerChildren;
    private ImageView imageTaskPreview;
    private Button btnAssignTask, btnGoBack;

    private final List<String> childIds = new ArrayList<>();
    private final List<TaskTemplate> taskTemplateList = new ArrayList<>();
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task_to_child);

        parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupEvents();

        loadTemplatesFromFirebase();
        loadChildrenFromFirebase();
    }

    // חיבור רכיבי המסך מה-XML לקוד
    private void initViews() {
        etTaskTitle = findViewById(R.id.etTitle);
        etTaskDueDate = findViewById(R.id.etDueDate);
        spinnerTemplates = findViewById(R.id.spTemplates);
        spinnerChildren = findViewById(R.id.spAssignee);
        imageTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssignTask = findViewById(R.id.btnAssign);
        btnGoBack = findViewById(R.id.btnBackToDashboard);
    }

    // הגדרת הפעולות של הבחירות והכפתורים במסך
    private void setupEvents() {
        spinnerTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSelectedTemplateData(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etTaskDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDatePicker();
            }
        });

        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assignTask();
            }
        });

        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // טעינת תבניות המשימה של ההורה מ-Firebase
    private void loadTemplatesFromFirebase()
    {
        getParentDbReference().child("task_templates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskTemplateList.clear();
                // titles מוצג ב-Spinner, והאובייקטים עצמם נשמרים ב-taskTemplateList
                List<String> titles = new ArrayList<>();
                for (DataSnapshot templateSnapshot : snapshot.getChildren()) {
                    TaskTemplate template = templateSnapshot.getValue(TaskTemplate.class);
                    if (template != null) {
                        template.setId(templateSnapshot.getKey());
                        taskTemplateList.add(template);
                        titles.add(template.getTitle());
                    }
                }
                fillSpinner(spinnerTemplates, titles);
                if (!taskTemplateList.isEmpty()) {
                    updateSelectedTemplateData(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase מחייב מימוש, גם אם ריק
            }
        });
    }

    // טעינת הילדים של ההורה מ-Firebase
    private void loadChildrenFromFirebase() {
        getParentDbReference().child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                // childNames מוצג ב-Spinner, ו-childIds שומר את המזהים האמיתיים ל-Firebase
                List<String> childNames = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    childIds.add(childSnapshot.getKey());
                    String firstName = childSnapshot.child("firstName").getValue(String.class);
                    childNames.add(firstName);
                }
                fillSpinner(spinnerChildren, childNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase מחייב מימוש, גם אם ריק
            }
        });
    }

    // יצירת משימה חדשה ושמירתה תחת הילד שנבחר
    private void assignTask() {
        String title = etTaskTitle.getText().toString().trim();
        String date = etTaskDueDate.getText().toString().trim();
        int childPosition = spinnerChildren.getSelectedItemPosition();

        // בודקים שהוזנו כותרת ותאריך
        if (title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מיקום חדש למשימה תחת הילד שנבחר
        DatabaseReference newTaskRef = getParentDbReference()
                .child("children").child(childIds.get(childPosition))
                .child("tasks").push();

        int templatePosition = spinnerTemplates.getSelectedItemPosition();
        TaskTemplate selectedTemplate = taskTemplateList.get(templatePosition);
        String imageBase64 = selectedTemplate.getImageBase64();

        // בניית אובייקט המשימה שנשמר ב-Firebase
        ChildTask newTask = new ChildTask();
        newTask.setTitle(title);
        newTask.setDueAt(date);
        newTask.setIsDone(false);
        newTask.setImageBase64(imageBase64);

        // אחרי שהמשימה נשמרה ב-Firebase סוגרים את המסך וחוזרים לדשבורד
        newTaskRef.setValue(newTask).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                finish();
            }
        });
    }

    // הצגת פרטי התבנית שנבחרה בטופס
    private void updateSelectedTemplateData(int position) {
        if (position >= 0 && position < taskTemplateList.size()) {
            TaskTemplate template = taskTemplateList.get(position);
            etTaskTitle.setText(template.getTitle());

            // המרת תמונת Base64 חזרה ל-Bitmap כדי להציג אותה במסך
            Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
            if (bitmap != null) {
                imageTaskPreview.setImageBitmap(bitmap);
            } else {
                // אם אין תמונה אמיתית, משאירים את אזור התמונה ריק
                imageTaskPreview.setImageDrawable(null);
            }
        }
    }

    private void fillSpinner(Spinner spinner, List<String> items) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    }

    // פתיחת לוח שנה לבחירת תאריך יעד
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        // פותח לוח שנה, וכשהמשתמש בוחר תאריך הוא נכתב בשדה התאריך
        new DatePickerDialog(this, new android.app.DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                // DatePicker מחזיר חודש מ-0, לכן מוסיפים 1 לפני ההצגה
                etTaskDueDate.setText(getString(R.string.date_slash_format, day, month + 1, year));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // מחזיר את מיקום ההורה המחובר ב-Firebase
    private DatabaseReference getParentDbReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentId);
    }
}
