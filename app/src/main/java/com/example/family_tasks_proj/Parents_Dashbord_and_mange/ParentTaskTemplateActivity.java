package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

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

/**
 * מסך יצירת תבנית משימה (Task Template).
 *
 * אחריות:
 * - ההורה בוחר תמונה מהגלריה וכותב כותרת.
 * - התמונה עוברת תיקון EXIF + הקטנה עם שמירת יחס גובה-רוחב (דרך ImageHelper).
 * - אותו Bitmap בדיוק משמש לתצוגה מקדימה וגם לשמירה כ-Base64.
 * - התבנית נשמרת ב-Firebase בנתיב /parents/{uid}/task_templates/{id}.
 */
public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTitle;
    private ImageView imgTask;

    /**
     * ה-Bitmap המתוקן — אותו אובייקט בדיוק משמש לתצוגה מקדימה ולשמירה.
     * כך מובטח שמה שההורה רואה = מה שנשמר ב-Firebase = מה שהילד יראה.
     */
    private Bitmap correctedBitmap;

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

    /**
     * מקבל Uri מהגלריה, טוען ומתקן את התמונה דרך ImageHelper,
     * ומציג את ה-Bitmap המתוקן ב-ImageView.
     *
     * חשוב: לא משתמשים ב-setImageURI כי הוא מציג תמונה מתוקנת-EXIF
     * אבל ה-Base64 שנשמר אחר כך לא יהיה תואם. במקום זה — שניהם
     * (תצוגה + שמירה) משתמשים באותו correctedBitmap.
     */
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri ->
            {
                if (uri != null)
                {
                    correctedBitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                    if (correctedBitmap != null)
                    {
                        imgTask.setImageBitmap(correctedBitmap);
                    }
                    else
                    {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * שומר תבנית חדשה ב-Firebase תחת /parents/{uid}/task_templates/{id}.
     * ממיר את correctedBitmap ל-Base64 — אותו Bitmap שמוצג בתצוגה מקדימה.
     * Side-effect: סוגר את ה-Activity בהצלחה.
     */
    private void saveTemplate()
    {
        String title = etTitle.getText().toString().trim();

        if (title.isEmpty() || correctedBitmap == null)
        {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = ImageHelper.bitmapToBase64(correctedBitmap);

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
}
