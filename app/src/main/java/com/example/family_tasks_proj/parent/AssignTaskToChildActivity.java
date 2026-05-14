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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private final List<String> childUserIdList = new ArrayList<>();
    private final List<TaskTemplate> taskTemplateList = new ArrayList<>();
    private String currentParentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task_to_child);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentParentUid = user.getUid();

        initViews();
        setupEvents();
        
        loadTemplatesFromFirebase();
        loadChildrenFromFirebase();
    }

    private void initViews() {
        etTaskTitle = findViewById(R.id.etTitle);
        etTaskDueDate = findViewById(R.id.etDueDate);
        spinnerTemplates = findViewById(R.id.spTemplates);
        spinnerChildren = findViewById(R.id.spAssignee);
        imageTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssignTask = findViewById(R.id.btnAssign);
        btnGoBack = findViewById(R.id.btnBackToDashboard);
    }

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
                confirmAndAssign();
            }
        });

        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // טוען את תבניות המשימה של ההורה מ-Firebase כדי להציג אותן ב-Spinner
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // טוען ילדים מ-Firebase ושומר את ה-ID של כל אחד לצורך ההקצאה
    private void loadChildrenFromFirebase() {
        getParentDbReference().child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childUserIdList.clear();
                List<String> childNames = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    childUserIdList.add(childSnapshot.getKey());
                    String firstName = childSnapshot.child("firstName").getValue(String.class);
                    if (firstName != null) {
                        childNames.add(firstName);
                    } else {
                        childNames.add(getString(R.string.default_child_name_fallback));
                    }
                }
                fillSpinner(spinnerChildren, childNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void confirmAndAssign() {
        String title = etTaskTitle.getText().toString().trim();
        String date = etTaskDueDate.getText().toString().trim();
        int childPosition = spinnerChildren.getSelectedItemPosition();

        if (title.isEmpty() || date.isEmpty() || childPosition < 0 || childPosition >= childUserIdList.size()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // יוצר מזהה (ID) למשימה חדשה תחת הילד שנבחר
        DatabaseReference newTaskRef = getParentDbReference()
                .child("children").child(childUserIdList.get(childPosition))
                .child("tasks").push();

        TaskTemplate selectedTemplate;
        int templatePosition = spinnerTemplates.getSelectedItemPosition();
        if (templatePosition >= 0 && templatePosition < taskTemplateList.size()) {
            selectedTemplate = taskTemplateList.get(templatePosition);
        } else {
            selectedTemplate = null;
        }

        String img;
        if (selectedTemplate != null) {
            img = selectedTemplate.getImageBase64();
        } else {
            img = "";
        }

        ChildTask newTask = new ChildTask();
        newTask.setTitle(title);
        newTask.setDueAt(date);
        newTask.setIsDone(false);
        newTask.setCreatedAt(System.currentTimeMillis());
        newTask.setImageBase64(img);

        newTaskRef.setValue(newTask).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(AssignTaskToChildActivity.this, R.string.success_task_assigned, Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        });
    }

    private void updateSelectedTemplateData(int position) {
        if (position >= 0 && position < taskTemplateList.size()) {
            TaskTemplate template = taskTemplateList.get(position);
            etTaskTitle.setText(template.getTitle());
            Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
            if (bitmap != null) {
                imageTaskPreview.setImageBitmap(bitmap);
            } else {
                imageTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }

    private void fillSpinner(Spinner spinner, List<String> items) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, new android.app.DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                etTaskDueDate.setText(getString(R.string.date_slash_format, day, month + 1, year));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private DatabaseReference getParentDbReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(currentParentUid);
    }
}
