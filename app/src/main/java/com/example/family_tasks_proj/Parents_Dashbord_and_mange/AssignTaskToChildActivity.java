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

/** מסך להקצאת משימה לילד. מאפשר לבחור תבנית משימה, ילד ותאריך יעד. */
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

    // מעדכן את הפרטים לפי התבנית שנבחרה
    private void handleTemplateSelected(int position) {
        if (position < 0 || position >= templates.size()) {
            return;
        }
        TaskTemplate selectedTemplate = templates.get(position);
        etTitle.setText(selectedTemplate.getTitle());
        displayBase64Image(selectedTemplate.getImageBase64());
    }

    // טוען את כל התבניות מהשרת
    private void loadTemplates() {
        templates.clear();
        // מושך את כל תבניות המשימות שההורה יצר מה-Firebase
        parentRef().child("task_templates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> titles = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    TaskTemplate template = snap.getValue(TaskTemplate.class);
                    if (template != null) {
                        if (template.getId() == null) {
                            template.setId(snap.getKey());
                        }
                        templates.add(template);
                        titles.add(template.getTitle());
                    }
                }
                setSpinnerItems(spTemplates, titles);
                if (!templates.isEmpty()) {
                    displayBase64Image(templates.get(0).getImageBase64());
                } else {
                    imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.assign_task_error_loading_templates, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // טוען את רשימת הילדים מהשרת
    private void loadChildren() {
        childIds.clear();
        // מושך את רשימת הילדים של ההורה כדי שנוכל לבחור למי להקצות את המשימה
        parentRef().child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> names = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String childId = snap.getKey();
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);
                    childIds.add(childId);
                    
                    String fullName = firstName;
                    if (lastName != null && !lastName.isEmpty()) {
                        fullName = fullName + " " + lastName;
                    }
                    names.add(fullName);
                }
                setSpinnerItems(spAssignee, names);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.assign_task_error_loading_children, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מציג דיאלוג לאישור לפני השמירה
    private void showAssignConfirmDialog() {
        if (!isAssignmentInputValid()) {
            return;
        }
        
        final String title = etTitle.getText().toString().trim();
        final String assignee = (String) spAssignee.getSelectedItem();
        final String dueDate = etDueDate.getText().toString().trim();

        new AlertDialog.Builder(this)
                .setTitle(R.string.assign_task_confirm_title)
                .setMessage(getString(R.string.assign_task_confirm_message, title, assignee, dueDate))
                .setPositiveButton(R.string.assign_task_confirm_action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        assignTask();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private boolean isAssignmentInputValid() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDueDate.getText().toString().trim().isEmpty() || spAssignee.getSelectedItemPosition() < 0) {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // שומר את המשימה החדשה ב-Firebase
    private void assignTask() {
        if (!isAssignmentInputValid()) {
            return;
        }
        btnAssign.setEnabled(false);
        int selectedChildPosition = spAssignee.getSelectedItemPosition();
        // יוצר נתיב חדש למשימה תחת הילד שנבחר ויוצר מזהה ייחודי בעזרת push()
        DatabaseReference ref = parentRef().child("children").child(childIds.get(selectedChildPosition)).child("tasks").push();
        
        TaskTemplate template = null;
        int selectedTemplatePosition = spTemplates.getSelectedItemPosition();
        if (selectedTemplatePosition >= 0 && selectedTemplatePosition < templates.size()) {
            template = templates.get(selectedTemplatePosition);
        }

        int starsWorth = 10;
        String imageBase64 = null;
        if (template != null) {
            starsWorth = template.safeStarsWorth();
            imageBase64 = template.getImageBase64();
        }

        Map<String, Object> task = new HashMap<>();
        task.put("title", etTitle.getText().toString().trim());
        task.put("dueAt", etDueDate.getText().toString().trim());
        task.put("isDone", false);
        task.put("starsWorth", starsWorth);
        task.put("imageBase64", imageBase64);
        task.put("createdAt", System.currentTimeMillis());

        // שומר את כל נתוני המשימה ב-Firebase בבת אחת
        ref.setValue(task).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(AssignTaskToChildActivity.this, R.string.assign_task_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                btnAssign.setEnabled(true);
                Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.error_save_generic, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private DatabaseReference parentRef() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentUid);
    }

    private void setSpinnerItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
    }

    private void displayBase64Image(String base64) {
        if (base64 == null || base64.isEmpty()) {
            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        android.graphics.Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
        if (bitmap != null) {
            imgTaskPreview.setImageBitmap(bitmap);
        } else {
            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                etDueDate.setText(day + "/" + (month + 1) + "/" + year);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
