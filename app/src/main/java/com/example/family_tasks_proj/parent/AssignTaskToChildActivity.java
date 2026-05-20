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

    private EditText etTitle, etDueDate;
    private Spinner spinnerTemplates, spinnerChildren;
    private ImageView imageTaskPreview;
    private Button btnAssign, btnBack;

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

    // חיבור רכיבי המסך מה-XML
    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDueDate = findViewById(R.id.etDueDate);
        spinnerTemplates = findViewById(R.id.spTemplates);
        spinnerChildren = findViewById(R.id.spAssignee);
        imageTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);
        btnBack = findViewById(R.id.btnBackToDashboard);

        // הכפתור לא פעיל עד שיש גם ילדים וגם תבניות
        btnAssign.setEnabled(false);
    }

    // הפעלת כפתור ההקצאה רק אם הנתונים מוכנים
    private void refreshAssignButtonState() {
        btnAssign.setEnabled(!childIds.isEmpty() && !taskTemplateList.isEmpty());
    }

    // הגדרת פעולות
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

        etDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDatePicker();
            }
        });

        btnAssign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assignTask();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // טעינת תבניות מה-Firebase
    private void loadTemplatesFromFirebase() {
        getParentDbReference().child("task_templates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskTemplateList.clear();
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
                refreshAssignButtonState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // טעינת רשימת הילדים מה-Firebase
    private void loadChildrenFromFirebase() {
        getParentDbReference().child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                List<String> childNames = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    childIds.add(childSnapshot.getKey());
                    String firstName = childSnapshot.child("firstName").getValue(String.class);
                    childNames.add(firstName);
                }
                fillSpinner(spinnerChildren, childNames);
                refreshAssignButtonState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // שיוך המשימה לילד ב-Firebase
    private void assignTask() {
        String title = etTitle.getText().toString().trim();
        String date = etDueDate.getText().toString().trim();
        int childPosition = spinnerChildren.getSelectedItemPosition();

        if (title.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference newTaskRef = getParentDbReference()
                .child("children").child(childIds.get(childPosition))
                .child("tasks").push();

        TaskTemplate selectedTemplate = taskTemplateList.get(spinnerTemplates.getSelectedItemPosition());
        String imageBase64 = selectedTemplate.getImageBase64();

        ChildTask newTask = new ChildTask();
        newTask.setTitle(title);
        newTask.setDueAt(date);
        newTask.setImageBase64(imageBase64);
        newTask.setIsDone(false);

        newTaskRef.setValue(newTask).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                finish();
            }
        });
    }

    // עדכון פרטי התבנית שנבחרה בטופס
    private void updateSelectedTemplateData(int position) {
        if (position >= 0 && position < taskTemplateList.size()) {
            TaskTemplate template = taskTemplateList.get(position);
            etTitle.setText(template.getTitle());

            Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
            if (bitmap != null) {
                imageTaskPreview.setImageBitmap(bitmap);
            } else {
                imageTaskPreview.setImageDrawable(null);
            }
        }
    }

    private void fillSpinner(Spinner spinner, List<String> items) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    }

    // פתיחת לוח שנה לבחירת תאריך
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, new android.app.DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                etDueDate.setText(getString(R.string.date_slash_format, day, month + 1, year));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // נתיב ההורה ב-Firebase
    private DatabaseReference getParentDbReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentId);
    }
}
