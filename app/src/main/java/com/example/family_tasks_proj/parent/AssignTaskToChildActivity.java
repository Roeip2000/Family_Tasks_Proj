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

    private EditText etDueDate;
    private Spinner spinnerTemplates, spinnerChildren;
    private ImageView imageTaskPreview;
    private Button btnAssign, btnBack;

    // childIds שומר את מזהי הילדים, taskTemplates שומר את התבניות שניתן לשייך
    private final List<String> childIds = new ArrayList<>();
    private final List<TaskTemplate> taskTemplates = new ArrayList<>();
    
    private String parentId;
    private DatabaseReference parentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task_to_child);

        // מזהה ההורה המחובר
        parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // הנתיב של ההורה המחובר ב-Firebase
        parentReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId);

        etDueDate = findViewById(R.id.etDueDate);
        spinnerTemplates = findViewById(R.id.spTemplates);
        spinnerChildren = findViewById(R.id.spAssignee);
        imageTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);
        btnBack = findViewById(R.id.btnBackToDashboard);

        spinnerTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Android מחייב את כל הפרמטרים, אבל כאן מספיק לנו position
                showSelectedTemplateImage(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
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

        loadTemplatesFromFirebase();
        loadChildrenFromFirebase();
    }

    // טעינת תבניות מה-Firebase
    private void loadTemplatesFromFirebase()
    {
        parentReference.child("task_templates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskTemplates.clear();
                List<String> templateTitles = new ArrayList<>();
                templateTitles.add("בחר תבנית");

                for (DataSnapshot templateSnapshot : snapshot.getChildren())
                {
                    TaskTemplate template = templateSnapshot.getValue(TaskTemplate.class);
                    if (template != null) {
                        taskTemplates.add(template);
                        templateTitles.add(template.getTitle());
                    }
                }
                
                spinnerTemplates.setAdapter(new ArrayAdapter<>(
                        AssignTaskToChildActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        templateTitles
                ));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // טעינת רשימת הילדים מה-Firebase
    private void loadChildrenFromFirebase() {
        parentReference.child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                List<String> childNames = new ArrayList<>();
                childNames.add("בחר ילד");

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    childIds.add(childSnapshot.getKey());
                    String firstName = childSnapshot.child("firstName").getValue(String.class);
                    childNames.add(firstName);
                }
                
                // עדכון ה-Spinner ישירות
                spinnerChildren.setAdapter(new ArrayAdapter<>(
                        AssignTaskToChildActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        childNames
                ));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // שיוך המשימה לילד ב-Firebase
    private void assignTask()
    {
        String date = etDueDate.getText().toString().trim();
        int childPosition = spinnerChildren.getSelectedItemPosition();
        int templatePosition = spinnerTemplates.getSelectedItemPosition();

        if (childPosition == 0 || templatePosition == 0 || date.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        int childIndex = childPosition - 1;
        int templateIndex = templatePosition - 1;

        DatabaseReference newTaskReference = parentReference
                .child("children")
                .child(childIds.get(childIndex))
                .child("tasks")
                .push();

        TaskTemplate selectedTemplate = taskTemplates.get(templateIndex);

        ChildTask newTask = new ChildTask();
        newTask.setTitle(selectedTemplate.getTitle());
        newTask.setDueAt(date);
        newTask.setImageBase64(selectedTemplate.getImageBase64());
        newTask.setIsDone(false);

        newTaskReference.setValue(newTask).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                finish();
            }
        });
    }

    // הצגת תמונת התבנית שנבחרה
    private void showSelectedTemplateImage(int position) {
        if (position == 0) {
            imageTaskPreview.setImageDrawable(null);
            return;
        }

        int templateIndex = position - 1;

        if (templateIndex >= 0 && templateIndex < taskTemplates.size()) {
            TaskTemplate template = taskTemplates.get(templateIndex);

            Bitmap templateBitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
            imageTaskPreview.setImageBitmap(templateBitmap);
        }
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
}
