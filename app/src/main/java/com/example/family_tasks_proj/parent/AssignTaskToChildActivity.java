package com.example.family_tasks_proj.parent;

import android.app.DatePickerDialog;
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

import com.example.family_tasks_proj.models.TaskTemplate;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.utils.ImageHelper;
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

// מסך הקצאת משימה לילד מתוך תבנית
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

    private void loadTemplatesFromFirebase() {
        // תבניות משימה נשמרות אצל ההורה, כדי שיוכל להקצות שוב משימות דומות בלי להקליד הכל מחדש.
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

    private void loadChildrenFromFirebase() {
        // טוענים את ילדי המשפחה ל-Spinner, אבל שומרים בצד גם את ה-id האמיתי של כל ילד.
        // השם מוצג למשתמש, וה-id משמש לכתיבה לנתיב הנכון ב-Firebase.
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

        // בדיקת קלט לפני כתיבה למסד הנתונים: חייבים שם משימה, תאריך וילד נבחר.
        if (title.isEmpty() || date.isEmpty() || childPosition < 0 || childPosition >= childUserIdList.size()) {
            Toast.makeText(this, R.string.error_assign_missing_details, Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת משימה חדשה תחת הילד הנבחר
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

        // המשימה נשמרת תחת הילד שנבחר: parents/{parent}/children/{child}/tasks/{task}.
        newTaskRef.child("title").setValue(title);
        newTaskRef.child("dueAt").setValue(date);
        newTaskRef.child("isDone").setValue(false);
        newTaskRef.child("createdAt").setValue(System.currentTimeMillis());
        // אם נבחרה תבנית - לוקחים ממנה את הכוכבים, אחרת ערך ברירת מחדל 10
        int starsWorth;
        if (selectedTemplate != null) {
            starsWorth = selectedTemplate.safeStarsWorth();
        } else {
            starsWorth = TaskTemplate.DEFAULT_STARS_WORTH;
        }
        newTaskRef.child("starsWorth").setValue(starsWorth);

        String img;
        if (selectedTemplate != null) {
            img = selectedTemplate.getImageBase64();
        } else {
            img = "";
        }
        newTaskRef.child("imageBase64").setValue(img).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(AssignTaskToChildActivity.this, R.string.success_task_assigned, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateSelectedTemplateData(int position) {
        if (position >= 0 && position < taskTemplateList.size()) {
            TaskTemplate template = taskTemplateList.get(position);
            etTaskTitle.setText(template.getTitle());
            imageTaskPreview.setImageBitmap(ImageHelper.base64ToBitmap(template.getImageBase64()));
        }
    }

    private void fillSpinner(Spinner spinner, List<String> items) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        // DatePicker נותן בחירת תאריך נוחה ומונע מהמשתמש להקליד תאריך בפורמט שגוי.
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
