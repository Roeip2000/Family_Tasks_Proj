package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTitle;
    private ImageView imgTask;
    private Bitmap correctedBitmap;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                correctedBitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (correctedBitmap == null) {
                    Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                    return;
                }

                imgTask.setImageBitmap(correctedBitmap);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_task_template);

        etTitle = findViewById(R.id.etTitle);
        imgTask = findViewById(R.id.imgTask);

        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btnSave = findViewById(R.id.btnSave);

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> saveTemplate());
    }

    private void saveTemplate() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty() || correctedBitmap == null) {
            Toast.makeText(this, R.string.template_missing_title_or_image, Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = ImageHelper.bitmapToBase64(correctedBitmap);
        if (imageBase64 == null) {
            Toast.makeText(this, R.string.error_image_conversion, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.template_parent_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String taskId = UUID.randomUUID().toString();
        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("title", title);
        task.put("imageBase64", imageBase64);

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("task_templates")
                .child(taskId)
                .setValue(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.template_saved_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        getString(R.string.error_save_generic, e.getMessage()),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
