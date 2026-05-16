package com.example.family_tasks_proj.parent;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.google.android.gms.tasks.OnSuccessListener;
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
    private ImageView imagePreview;
    private Button btnSaveTemplate, btnBackToMain;
    private ListView listViewTemplates;
    private TextView tvFormHeader;

    private final List<TaskTemplate> taskTemplates = new ArrayList<>();
    private TemplateListAdapter templateAdapter;
    private String editingTemplateId = null;
    private Bitmap currentSelectedBitmap = null;
    private String parentId;

    // פתיחת הגלריה וקבלת התמונה שנבחרה לתבנית
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        // טעינת התמונה מהגלריה והקטנתה לפני שמירה
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

        initViews();
        setupEvents();
        setupList();
        loadTemplatesFromFirebase();
    }

    // חיבור רכיבי המסך מה-XML לקוד
    private void initViews() {
        etTemplateTitle = findViewById(R.id.etTitle);
        imagePreview = findViewById(R.id.imgTask);
        btnSaveTemplate = findViewById(R.id.btnSave);
        btnBackToMain = findViewById(R.id.btnBackToDashboard);
        listViewTemplates = findViewById(R.id.lvTemplates);
        tvFormHeader = findViewById(R.id.tvFormTitle);
    }

    // הגדרת פעולות הכפתורים במסך
    private void setupEvents() {
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

        btnBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // הכנת רשימת התבניות ולחיצה על תבנית לעריכה
    private void setupList() {
        templateAdapter = new TemplateListAdapter();
        listViewTemplates.setAdapter(templateAdapter);
        listViewTemplates.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startEditTemplate(position);
            }
        });
    }

    // טעינת תבניות המשימה של ההורה מ-Firebase
    private void loadTemplatesFromFirebase() {
        getTemplatesReference().addValueEventListener(new ValueEventListener() {
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
                fitListHeight(listViewTemplates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase מחייב מימוש, גם אם ריק
            }
        });
    }

    // שמירת תבנית חדשה או עדכון תבנית קיימת
    private void saveTemplate() {
        String title = etTemplateTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String templateId;
        if (editingTemplateId != null) {
            templateId = editingTemplateId;
        } else {
            templateId = getTemplatesReference().push().getKey();
        }

        DatabaseReference templateRef = getTemplatesReference().child(templateId);

        // אם נבחרה תמונה חדשה, ממירים אותה ל-Base64 ושומרים ב-Firebase
        if (currentSelectedBitmap != null) {
            String base64 = ImageHelper.bitmapToBase64(currentSelectedBitmap);
            templateRef.child("imageBase64").setValue(base64);
        }

        templateRef.child("title").setValue(title).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentTaskTemplateActivity.this, R.string.toast_template_saved, Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    // לחיצה על תבנית ברשימה טוענת אותה לטופס לעריכה
    private void startEditTemplate(int position) {
        TaskTemplate template = taskTemplates.get(position);

        editingTemplateId = template.getId();

        // אין תמונה חדשה שנבחרה כרגע, כדי לא לדרוס תמונה קיימת בשמירה
        currentSelectedBitmap = null;

        etTemplateTitle.setText(template.getTitle());

        Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap);
        } else {
            imagePreview.setImageResource(R.drawable.ic_image_placeholder);
        }

        tvFormHeader.setText(R.string.title_edit_template);
        btnSaveTemplate.setText(R.string.btn_update_template);
    }

    // ניקוי הטופס אחרי שמירה או חזרה למצב יצירת תבנית חדשה
    private void clearForm() {
        editingTemplateId = null;
        currentSelectedBitmap = null;
        etTemplateTitle.setText("");
        // אם אין תמונה שנבחרה, אזור התמונה נשאר ריק
        imagePreview.setImageDrawable(null);
        tvFormHeader.setText(R.string.title_create_template);
        btnSaveTemplate.setText(R.string.btn_save_template);
    }

    private static final int DEFAULT_LIST_WIDTH = 500;

    // מחשב גובה ל-ListView כדי שכל התבניות יוצגו בתוך המסך
    private void fitListHeight(ListView listView) {
        if (listView.getAdapter() == null) {
            return;
        }

        int listWidth = listView.getWidth();
        if (listWidth <= 0) {
            listWidth = DEFAULT_LIST_WIDTH;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);

        int totalHeight = 0;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            View itemView = listView.getAdapter().getView(i, null, listView);
            itemView.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += itemView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listView.getAdapter().getCount() - 1));
        listView.setLayoutParams(params);
    }

    // מחזיר את מיקום תבניות המשימה של ההורה ב-Firebase
    private DatabaseReference getTemplatesReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("task_templates");
    }

    // Adapter שמציג כל תבנית כשורה עם כותרת ותמונה
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
                TextView tvTitle = convertView.findViewById(R.id.tvTemplateTitle);
                ImageView imageThumb = convertView.findViewById(R.id.ivTemplateThumb);

                tvTitle.setText(template.getTitle());

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
