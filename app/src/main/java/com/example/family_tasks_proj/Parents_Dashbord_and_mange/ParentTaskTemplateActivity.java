package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;

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

/**
 * מסך יצירת תבנית משימה (Task Template).
 *
 * אחריות:
 * - ההורה בוחר תמונה מהגלריה וכותב כותרת.
 * - התמונה מוקטנת ל-400×400 ומומרת ל-Base64 (JPEG 75%).
 * - התבנית נשמרת ב-Firebase בנתיב /parents/{uid}/task_templates/{id}.
 * - משמשת מאוחר יותר ב-AssignTaskToChildActivity להקצאת משימות.
 */
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

    /** פותח את הגלריה לבחירת תמונה. */
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

    /**
     * שומר תבנית חדשה ב-Firebase תחת /parents/{uid}/task_templates/{id}.
     * Side-effect: סוגר את ה-Activity בהצלחה.
     */
    private void saveTemplate()
    {
        String title = etTitle.getText().toString().trim();

        if (title.isEmpty() || imageUri == null)
        {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = imageUriToBase64(imageUri);

        if (imageBase64 == null)
        {
            Toast.makeText(this, "Image conversion failed", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = UUID.randomUUID().toString();

        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("title", title);
        task.put("imageBase64", imageBase64);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
        {
            Toast.makeText(this, "Parent not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("task_templates")
                .child(taskId)
                .setValue(task)


                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(this, "Template saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ממיר Uri של תמונה למחרוזת Base64.
     * מקטין ל-400×400, דוחס ל-JPEG 75%.
     *
     * @param uri  ה-Uri שנבחר מהגלריה
     * @return מחרוזת Base64, או null אם ההמרה נכשלה
     */
    private String imageUriToBase64(Uri uri)
    {

        try (InputStream inputStream = getContentResolver().openInputStream(uri))
        {

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) return null;

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
