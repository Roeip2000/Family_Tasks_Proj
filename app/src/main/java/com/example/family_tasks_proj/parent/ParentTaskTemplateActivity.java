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

    private EditText etTemplateTitle;
    private ImageView imgTemplatePreview;
    private Button btnSaveTemplate, btnBack;
    private ListView lvTemplates;

    private final List<TaskTemplate> taskTemplates = new ArrayList<>();
    private TemplateListAdapter templateAdapter;
    private Bitmap selectedTemplateBitmap = null;
    private String parentId;
    private DatabaseReference templatesReference;

    // בחירת תמונה לתבנית מהגלריה
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        selectedTemplateBitmap = ImageHelper.loadResizedBitmap(getContentResolver(), uri);
                        if (selectedTemplateBitmap != null) {
                            imgTemplatePreview.setImageBitmap(selectedTemplateBitmap);
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

        etTemplateTitle = findViewById(R.id.etTitle);
        imgTemplatePreview = findViewById(R.id.imgTask);
        btnSaveTemplate = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvTemplates = findViewById(R.id.lvTemplates);

        findViewById(R.id.btnPickImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryLauncher.launch("image/*");
            }
        });

        btnSaveTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTemplate();
            }
        });

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

    // שמירה של תבנית חדשה
    private void saveTemplate() {
        String title = etTemplateTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTemplateBitmap == null) {
            Toast.makeText(this, R.string.error_select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        String templateId = templatesReference.push().getKey();
        DatabaseReference templateReference = templatesReference.child(templateId);

        String imageBase64 = ImageHelper.bitmapToBase64(selectedTemplateBitmap);

        templateReference.child("title").setValue(title);
        templateReference.child("imageBase64").setValue(imageBase64);

        Toast.makeText(ParentTaskTemplateActivity.this, R.string.toast_template_saved, Toast.LENGTH_SHORT).show();
        clearForm();
    }

    // ניקוי הטופס אחרי שמירה
    private void clearForm() {
        selectedTemplateBitmap = null;
        etTemplateTitle.setText("");
        imgTemplatePreview.setImageDrawable(null);
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
                ImageView imgTemplateThumb = convertView.findViewById(R.id.ivTemplateThumb);

                tvTemplateTitle.setText(template.getTitle());

                // המרת הטקסט חזרה לתמונה לתצוגה ברשימה
                Bitmap templateBitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
                imgTemplateThumb.setImageBitmap(templateBitmap);
            }
            return convertView;
        }
    }
}
