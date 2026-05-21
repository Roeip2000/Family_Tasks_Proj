package com.example.family_tasks_proj.parent;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.models.TaskTemplate;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.utils.ImageHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTitle;
    private ImageView imagePreview;
    private Button btnSave, btnBack;
    private ListView lvTemplates;

    private final List<TaskTemplate> taskTemplates = new ArrayList<>();
    private TemplateListAdapter templateAdapter;
    private Bitmap currentSelectedBitmap = null;
    private String parentId;
    private DatabaseReference templatesReference;

    // פתיחת גלריה לבחירת תמונה לתבנית
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        // הקטנת התמונה לפני שמירה
                        currentSelectedBitmap = ImageHelper.loadResizedBitmap(getContentResolver(), uri);
                        if (currentSelectedBitmap != null) {
                            imagePreview.setImageBitmap(currentSelectedBitmap);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_task_template);

        parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // הנתיב לתבניות המשימה ב-Firebase
        templatesReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("task_templates");

        etTitle = findViewById(R.id.etTitle);
        imagePreview = findViewById(R.id.imgTask);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvTemplates = findViewById(R.id.lvTemplates);

        // כפתור לבחירת תמונה
        findViewById(R.id.btnPickImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryLauncher.launch("image/*");
            }
        });

        // כפתור שמירה
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog();
            }
        });

        // חזרה לדשבורד
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        templateAdapter = new TemplateListAdapter();
        lvTemplates.setAdapter(templateAdapter);

        loadTemplatesFromFirebase();
    }

    // טעינת תבניות מה-Firebase
    private void loadTemplatesFromFirebase() {
        templatesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskTemplates.clear();
                for (DataSnapshot templateSnapshot : snapshot.getChildren()) {
                    TaskTemplate template = templateSnapshot.getValue(TaskTemplate.class);
                    if (template != null) {
                        template.setId(templateSnapshot.getKey());
                        taskTemplates.add(template);
                    }
                }
                templateAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // הצגת דיאלוג אישור לפני שמירה
    private void showConfirmDialog() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("אישור שמירה");
        builder.setMessage("האם אתה בטוח שהפרטים נכונים?");
        
        builder.setPositiveButton("כן, שמור", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveTemplate(title);
            }
        });
        
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    // שמירה של תבנית חדשה
    private void saveTemplate(String title) {
        // יצירת מזהה חדש ב-Firebase
        String templateId = templatesReference.push().getKey();
        DatabaseReference templateRef = templatesReference.child(templateId);

        // שמירת הכותרת
        templateRef.child("title").setValue(title);

        // אם נבחרה תמונה, ממירים לטקסט (Base64) ושומרים
        if (currentSelectedBitmap != null) {
            String base64 = ImageHelper.bitmapToBase64(currentSelectedBitmap);
            templateRef.child("imageBase64").setValue(base64);
        }

        Toast.makeText(ParentTaskTemplateActivity.this, R.string.toast_template_saved, Toast.LENGTH_SHORT).show();
        clearForm();
    }

    // ניקוי הטופס אחרי שמירה
    private void clearForm() {
        currentSelectedBitmap = null;
        etTitle.setText("");
        imagePreview.setImageDrawable(null);
    }

    // Adapter פנימי שמציג כל תבנית ברשימה
    private class TemplateListAdapter extends ArrayAdapter<TaskTemplate> {
        TemplateListAdapter() {
            super(ParentTaskTemplateActivity.this, 0, taskTemplates);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_task_template, parent, false);
            }

            TaskTemplate template = getItem(position);
            if (template != null) {
                TextView tvTemplateTitle = convertView.findViewById(R.id.tvTemplateTitle);
                ImageView imageThumb = convertView.findViewById(R.id.ivTemplateThumb);

                tvTemplateTitle.setText(template.getTitle());

                // המרת הטקסט חזרה לתמונה לתצוגה ברשימה
                Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
                if (bitmap != null) {
                    imageThumb.setImageBitmap(bitmap);
                } else {
                    imageThumb.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
            return convertView;
        }
    }
}
