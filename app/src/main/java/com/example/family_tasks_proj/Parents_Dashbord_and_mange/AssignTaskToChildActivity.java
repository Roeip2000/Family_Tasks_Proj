package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskTemplate;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.database.DatabaseReference;
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
 * מסך הקצאת משימה לילד — ההורה בוחר תבנית, ילד, תאריך יעד ושולח.
 * תהליך:
 * 1. טוען תבניות מ-Firebase → מוצגות ב-Spinner
 * 2. טוען ילדים → מוצגים ב-Spinner שני
 * 3. ההורה בוחר תבנית → הכותרת והתמונה מוצגות אוטומטית
 * 4. ההורה בוחר תאריך → DatePicker
 * 5. לחיצה על "שלח" → שומרת את המשימה ב-Firebase תחת הילד שנבחר
 * נתיב Firebase: /parents/{uid}/children/{childId}/tasks/{taskId}
 */
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

        loadTemplates();
        loadChildren();

        spTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

                if (position < 0 || position >= templates.size())
                {
                    return;
                }

                // בחירת תבנית מעדכנת אוטומטית את שם המשימה והתמונה המקדימה
                TaskTemplate selectedTemplate = templates.get(position);
                etTitle.setText(selectedTemplate.toDisplayTitle());
                displayBase64Image(selectedTemplate.imageBase64);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });



        etDueDate.setOnClickListener(v -> showDatePicker());
        btnAssign.setOnClickListener(v -> showAssignConfirmDialog());

        // חזרה לדשבורד ההורה
        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> finish());
    }

    private void bindViews()
    {
        etTitle = findViewById(R.id.etTitle);
        etDueDate = findViewById(R.id.etDueDate);
        spTemplates = findViewById(R.id.spTemplates);
        spAssignee = findViewById(R.id.spAssignee);
        imgTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);
    }

    private void loadTemplates()

    {
        templates.clear();

        parentRef().child("task_templates").addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {

                        List<String> titles = new ArrayList<>();

                        for (DataSnapshot templateSnapshot : snapshot.getChildren()) {
                            TaskTemplate template = templateSnapshot.getValue(TaskTemplate.class);
                            if (template == null) {
                                continue;
                            }
                            if (template.id == null) {
                                template.id = templateSnapshot.getKey();
                            }

                            templates.add(template);
                            titles.add(template.toDisplayTitle());
                        }

                        setSpinnerItems(spTemplates, titles);
                        if (!templates.isEmpty()) {
                            displayBase64Image(templates.get(0).imageBase64);
                        } else {
                            imgTaskPreview.setImageResource(R.drawable.ic_image_placeholder);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.assign_task_error_loading_templates, error.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadChildren()
    {
        childIds.clear();

        parentRef().child("children").addListenerForSingleValueEvent(new ValueEventListener()
        {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)

                    {

                        List<String> childNames = new ArrayList<>();


                        for (DataSnapshot childSnapshot : snapshot.getChildren())
                        {
                            String childId = childSnapshot.getKey();

                            if (childId == null)
                            {
                                continue;
                            }

                            String firstName = childSnapshot.child("firstName").getValue(String.class);
                            String lastName = childSnapshot.child("lastName").getValue(String.class);
                            childIds.add(childId);

                            childNames.add(NameUtils.fullNameOrDefault(firstName, lastName, getString(R.string.default_child_name)));
                        }

                        setSpinnerItems(spAssignee, childNames);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error)
                    {


                        Toast.makeText(AssignTaskToChildActivity.this, getString(R.string.assign_task_error_loading_children, error.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showAssignConfirmDialog()
    {


        String title = etTitle.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        int childPosition = spAssignee.getSelectedItemPosition();


        if (!validateAssignment(title, dueDate, childPosition)) return;

        String childName = String.valueOf(spAssignee.getSelectedItem());


        new AlertDialog.Builder(this)
                .setTitle(R.string.assign_task_confirm_title)
                .setMessage(getString(R.string.assign_task_confirm_message, title, childName, dueDate))
                .setPositiveButton(R.string.assign_task_confirm_action, (dialog, which) -> assignTask())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void assignTask()
    {
        String title = etTitle.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        int childPosition = spAssignee.getSelectedItemPosition();
        if (!validateAssignment(title, dueDate, childPosition)) return;

        DatabaseReference tasksRef = tasksRef(childIds.get(childPosition));
        String taskId = tasksRef.push().getKey();

        if (taskId == null) {
            Toast.makeText(this, R.string.assign_task_error_create_id, Toast.LENGTH_SHORT).show();
            return;
        }

        // ערך הכוכבים נלקח מהתבנית שנבחרה — כך שכל משימה יורשת את מה שההורה הגדיר
        TaskTemplate selectedTemplate = getSelectedTemplate();
        int starsWorth = selectedTemplate != null
                ? selectedTemplate.safeStarsWorth()
                : TaskTemplate.DEFAULT_STARS_WORTH;

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("dueAt", dueDate);
        task.put("isDone", false);
        task.put("starsWorth", starsWorth);
        task.put("imageBase64", selectedTemplate != null ? selectedTemplate.imageBase64 : null);
        task.put("createdAt", System.currentTimeMillis());

        // כתיבה אחת פשוטה לנתיב של הילד שנבחר
        tasksRef.child(taskId)
                .setValue(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.assign_task_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.error_save_generic, e.getMessage()),
                        Toast.LENGTH_SHORT
                ).show());
    }

    private DatabaseReference parentRef() {
        return FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid);
    }

    private DatabaseReference tasksRef(String childId)

    {
        return parentRef()
                .child("children")
                .child(childId)
                .child("tasks");
    }

    private boolean validateAssignment(String title, String dueDate, int childPosition)

    {

        if (title.isEmpty())
        {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return false;
        }


        if (dueDate.isEmpty() || childPosition < 0 || childPosition >= childIds.size())
        {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private TaskTemplate getSelectedTemplate()
    {
        int templatePosition = spTemplates.getSelectedItemPosition();

        return templatePosition >= 0 && templatePosition < templates.size()
                ? templates.get(templatePosition) : null;
    }

    private void setSpinnerItems(Spinner spinner, List<String> items)
    {

        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
    }

    private void displayBase64Image(String base64)
    {
        if (base64 == null || base64.isEmpty())
        {
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

    private void showDatePicker()
    {
        Calendar calendar = Calendar.getInstance();


        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                etDueDate.setText(getString(R.string.default_date_format, dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();

    }
}
