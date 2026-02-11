package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTitle;
    private ImageView imgTask;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_task_template);

        etTitle = findViewById(R.id.etTitle);
        imgTask = findViewById(R.id.imgTask);

        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btnSave = findViewById(R.id.btnSave);

        btnPickImage.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> saveTemplate());
    }






    // פתיחת גלריה לבחירת תמונה
    private void pickImage()
    {
        imagePicker.launch("image/*");
    }

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri ->
            {
                if (uri != null)
                {
                    imageUri = uri;
                    imgTask.setImageURI(uri);
                }
            });

    // שמירת תבנית משימה ל-Firebase (כ-Base64)
    // נשמר תחת ההורה המחובר: /parents/{parentUid}/profile/taskTemplates/{templateId}
    private void saveTemplate() {
        // ===== בדיקת התחברות הורה =====
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String parentId = currentUser.getUid();

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = imageUriToBase64(imageUri);
        if (imageBase64 == null) {
            Toast.makeText(this, "Image conversion failed", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = UUID.randomUUID().toString();

        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("title", title);
        task.put("imageBase64", imageBase64);

        // ✅ שמירה תחת ההורה
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("task_templates")
                .child(taskId)
                .setValue(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Template saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // המרת תמונה ל-String (Base64) — גרסה בטוחה ומוקטנת
    private String imageUriToBase64(Uri uri)
    {
        try (InputStream inputStream = getContentResolver().openInputStream(uri))
        {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;

            // הקטנה כדי לא לחרוג ממגבלות Firebase
            bitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] bytes = baos.toByteArray();

            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
