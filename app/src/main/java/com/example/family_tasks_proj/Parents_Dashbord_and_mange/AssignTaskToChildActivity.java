package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskTemplate;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// === מסך: הקצאת משימה ===
// תפקיד: בוחר תבנית, ילד ותאריך יעד ושומר משימה חדשה לילד
// מחלקות קשורות: TaskTemplate, Child, ImageHelper
// Firebase path: parents/{uid}/children/{childId}/tasks
public class AssignTaskToChildActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etDueDate;
    private Spinner spTemplates;
    private Spinner spAssignee;
    private ImageView imgTaskPreview;
    private Button btnAssign;

    private final List<String> childIds = new ArrayList<>();
    private final List<TaskTemplate> templates = new ArrayList<>();

    private String parentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task_to_child);

        bindViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.assign_task_parent_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentUid = user.getUid();
        bindActions();
        loadTemplates();
        loadChildren();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDueDate = findViewById(R.id.etDueDate);
        spTemplates = findViewById(R.id.spTemplates);
        spAssignee = findViewById(R.id.spAssignee);
        imgTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);
    }

    private void bindActions() {
        spTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                handleTemplateSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        etDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        btnAssign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAssignConfirmDialog();
            }
        });

        findViewById(R.id.btnBackToDashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // מעדכן את הכותרת והתמונה כאשר ההורה בוחר תבנית
    private void handleTemplateSelected(int position) {
        if (position < 0 || position >= templates.size()) {
            return;
        }

        TaskTemplate selectedTemplate = templates.get(position);
        etTitle.setText(selectedTemplate.toDisplayTitle());
        displayBase64Image(selectedTemplate.getImageBase64());
    }

    // טוען תבניות מ-Firebase: /parents/{uid}/task_templates
    private void loadTemplates() {
        templates.clear();

        DatabaseReference templatesRef = parentRef().child("task_templates");
        templatesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleTemplatesSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        AssignTaskToChildActivity.this,
                        getString(R.string.assign_task_error_loading_templates, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // ממיר נתוני תבניות לרשימת כותרות בספינר
    private void handleTemplatesSnapshot(DataSnapshot snapshot) {
        List<String> titles = new ArrayList<>();

        for (DataSnapshot templateSnapshot : snapshot.getChildren()) {
            addTemplateFromSnapshot(templateSnapshot, titles);
        }

        setSpinnerItems(spTemplates, titles);
        showFirstTemplateImageOrPlaceholder();
    }

    private void addTemplateFromSnapshot(DataSnapshot templateSnapshot, List<String> titles) {
        TaskTemplate template = templateSnapshot.getValue(TaskTemplate.class);
        if (template == null) {
            return;
        }
        if (template.getId() == null) {
            template.setId(templateSnapshot.getKey());
        }

        templates.add(template);
        titles.add(template.toDisplayTitle());
    }

    // מציג תמונת תבנית ראשונה או תמונה חלופית אם אין תבניות
    private void showFirstTemplateImageOrPlaceholder() {
        if (!templates.isEmpty()) {
            displayBase64Image(templates.get(0).getImageBase64());
        } else {
            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    // טוען ילדים מ-Firebase: /parents/{uid}/children
    private void loadChildren() {
        childIds.clear();

        DatabaseReference childrenRef = parentRef().child("children");
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleChildrenSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        AssignTaskToChildActivity.this,
                        getString(R.string.assign_task_error_loading_children, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // ממיר נתוני ילדים לשמות בספינר
    private void handleChildrenSnapshot(DataSnapshot snapshot) {
        List<String> childNames = new ArrayList<>();

        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
            addChildFromSnapshot(childSnapshot, childNames);
        }

        setSpinnerItems(spAssignee, childNames);
    }

    private void addChildFromSnapshot(DataSnapshot childSnapshot, List<String> childNames) {
        String childId = childSnapshot.getKey();
        if (childId == null) {
            return;
        }

        String firstName = childSnapshot.child("firstName").getValue(String.class);
        String lastName = childSnapshot.child("lastName").getValue(String.class);
        childIds.add(childId);
        childNames.add(NameUtils.fullNameOrDefault(
                firstName,
                lastName,
                getString(R.string.default_child_name)
        ));
    }

    // מציג דיאלוג אישור לפני יצירת משימה
    private void showAssignConfirmDialog() {
        String title = etTitle.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        int childPosition = spAssignee.getSelectedItemPosition();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty() || childPosition < 0 || childPosition >= childIds.size()) {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String childName = String.valueOf(spAssignee.getSelectedItem());
        new AlertDialog.Builder(this)
                .setTitle(R.string.assign_task_confirm_title)
                .setMessage(getString(R.string.assign_task_confirm_message, title, childName, dueDate))
                .setPositiveButton(R.string.assign_task_confirm_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        assignTask();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // יוצר מפת נתונים של משימה ושומר אותה תחת הילד שנבחר ב-Firebase
    private void assignTask() {
        String title = etTitle.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        int childPosition = spAssignee.getSelectedItemPosition();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty() || childPosition < 0 || childPosition >= childIds.size()) {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return;
        }

        btnAssign.setEnabled(false); // מונע לחיצות כפולות

        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("parents").child(parentUid).child("children").child(childIds.get(childPosition)).child("tasks");
        String taskId = tasksRef.push().getKey();

        if (taskId == null) {
            btnAssign.setEnabled(true);
            Toast.makeText(this, R.string.assign_task_error_create_id, Toast.LENGTH_SHORT).show();
            return;
        }

        TaskTemplate selectedTemplate = null;
        int templatePosition = spTemplates.getSelectedItemPosition();
        if (templatePosition >= 0 && templatePosition < templates.size()) {
            selectedTemplate = templates.get(templatePosition);
        }

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("dueAt", dueDate);
        task.put("isDone", false);
        task.put("starsWorth", (selectedTemplate != null) ? selectedTemplate.safeStarsWorth() : TaskTemplate.DEFAULT_STARS_WORTH);
        task.put("imageBase64", (selectedTemplate != null) ? selectedTemplate.getImageBase64() : null);
        task.put("createdAt", System.currentTimeMillis());

        tasksRef.child(taskId).setValue(task).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(AssignTaskToChildActivity.this, R.string.assign_task_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                btnAssign.setEnabled(true);
                Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.error_save_generic, exception.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מחזיר הפניה להורה המחובר: /parents/{uid}
    private DatabaseReference parentRef() {
        return FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid);
    }

    // מכניס רשימת טקסטים לתוך Spinner פשוט
    private void setSpinnerItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                items
        );
        spinner.setAdapter(adapter);
    }

    private void displayBase64Image(String base64) {
        // Base64 = קידוד שהופך תמונה למחרוזת טקסט כדי לשמור ב-Firebase
        if (base64 == null || base64.isEmpty()) {
            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }

        android.graphics.Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
        if (bitmap == null) {
            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }

        imgTaskPreview.setImageBitmap(bitmap);
    }

    // פותח DatePicker ובוחר תאריך יעד למשימה
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker datePicker, int year, int month, int dayOfMonth) {
                etDueDate.setText(getString(R.string.default_date_format, dayOfMonth, month + 1, year));
            }
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
