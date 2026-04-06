package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
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

public class AssignTaskToChildActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etDueDate;
    private Spinner spTemplates;
    private Spinner spAssignee;
    private ImageView imgTaskPreview;
    private Button btnAssign;

    private final List<String> childIds = new ArrayList<>();
    private final List<Map<String, String>> templates = new ArrayList<>();

    private String parentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assign_task_to_child);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= templates.size()) {
                    return;
                }

                Map<String, String> selectedTemplate = templates.get(position);
                etTitle.setText(selectedTemplate.get("title"));
                displayBase64Image(selectedTemplate.get("imageBase64"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etDueDate.setOnClickListener(v -> showDatePicker());
        btnAssign.setOnClickListener(v -> showAssignConfirmDialog());
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDueDate = findViewById(R.id.etDueDate);
        spTemplates = findViewById(R.id.spTemplates);
        spAssignee = findViewById(R.id.spAssignee);
        imgTaskPreview = findViewById(R.id.imgTaskPreview);
        btnAssign = findViewById(R.id.btnAssign);
    }

    private void loadTemplates() {
        templates.clear();

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("task_templates")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> titles = new ArrayList<>();

                        for (DataSnapshot templateSnapshot : snapshot.getChildren()) {
                            String title = templateSnapshot.child("title").getValue(String.class);
                            String imageBase64 = templateSnapshot.child("imageBase64").getValue(String.class);

                            Map<String, String> template = new HashMap<>();
                            template.put("title", title == null ? "" : title);
                            template.put("imageBase64", imageBase64);
                            templates.add(template);
                            titles.add(template.get("title"));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                AssignTaskToChildActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                titles
                        );
                        spTemplates.setAdapter(adapter);

                        if (!templates.isEmpty()) {
                            displayBase64Image(templates.get(0).get("imageBase64"));
                        }
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

    private void loadChildren() {
        childIds.clear();

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> childNames = new ArrayList<>();

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String childId = childSnapshot.getKey();
                            if (childId == null) {
                                continue;
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

                        spAssignee.setAdapter(new ArrayAdapter<>(
                                AssignTaskToChildActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                childNames
                        ));
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

    private void showAssignConfirmDialog() {
        String title = etTitle.getText().toString().trim();
        String date = etDueDate.getText().toString().trim();
        int childPosition = spAssignee.getSelectedItemPosition();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty() || childPosition < 0 || childPosition >= childIds.size()) {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String childName = String.valueOf(spAssignee.getSelectedItem());
        new AlertDialog.Builder(this)
                .setTitle(R.string.assign_task_confirm_title)
                .setMessage(getString(R.string.assign_task_confirm_message, title, childName, date))
                .setPositiveButton(R.string.assign_task_confirm_action, (dialog, which) -> assignTask())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void assignTask() {
        int childPosition = spAssignee.getSelectedItemPosition();
        String title = etTitle.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.assign_task_missing_title, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty() || childPosition < 0 || childPosition >= childIds.size()) {
            Toast.makeText(this, R.string.assign_task_missing_child_or_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = childIds.get(childPosition);
        String imageBase64 = null;
        int templatePosition = spTemplates.getSelectedItemPosition();
        if (templatePosition >= 0 && templatePosition < templates.size()) {
            imageBase64 = templates.get(templatePosition).get("imageBase64");
        }

        String taskId = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .child(childId)
                .child("tasks")
                .push()
                .getKey();

        if (taskId == null) {
            Toast.makeText(this, R.string.assign_task_error_create_id, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("dueAt", dueDate);
        task.put("isDone", false);
        task.put("starsWorth", 10);
        task.put("imageBase64", imageBase64);
        task.put("createdAt", System.currentTimeMillis());

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentUid)
                .child("children")
                .child(childId)
                .child("tasks")
                .child(taskId)
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

    private void displayBase64Image(String base64) {
        if (base64 == null || base64.isEmpty()) {
            imgTaskPreview.setImageDrawable(null);
            return;
        }

        Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
        imgTaskPreview.setImageBitmap(bitmap);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) ->
                etDueDate.setText(getString(R.string.default_date_format, dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}
