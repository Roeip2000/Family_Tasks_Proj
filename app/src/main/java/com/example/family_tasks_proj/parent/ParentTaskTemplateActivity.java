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

    private final List<TaskTemplate> templateDataList = new ArrayList<>();
    private TemplateListAdapter templateAdapter;
    private String currentEditTemplateId = null;
    private Bitmap currentSelectedBitmap = null;
    private String currentParentUserId;

    // מאזין לקבלת תמונה מהגלריה
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        currentSelectedBitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
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

        currentParentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupEvents();
        setupList();
        loadFromFirebase();
    }

    private void initViews() {
        etTemplateTitle = findViewById(R.id.etTitle);
        imagePreview = findViewById(R.id.imgTask);
        btnSaveTemplate = findViewById(R.id.btnSave);
        btnBackToMain = findViewById(R.id.btnBackToDashboard);
        listViewTemplates = findViewById(R.id.lvTemplates);
        tvFormHeader = findViewById(R.id.tvFormTitle);
    }

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
                processSave();
            }
        });

        btnBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

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
    private void loadFromFirebase() {
        getTemplatesReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                templateDataList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    TaskTemplate template = snap.getValue(TaskTemplate.class);
                    if (template != null) {
                        template.setId(snap.getKey());
                        templateDataList.add(template);
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

    private void processSave() {
        String title = etTemplateTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String templateId;
        if (currentEditTemplateId != null) {
            templateId = currentEditTemplateId;
        } else {
            templateId = getTemplatesReference().push().getKey();
        }

        DatabaseReference templateRef = getTemplatesReference().child(templateId);

        // אם נבחרה תמונה חדשה שומרים אותה; אחרת התמונה הקיימת נשארת ב-Firebase
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

    // לחיצה על תבנית ברשימה טוענת את הכותרת והתמונה שלה לטופס לעריכה
    private void startEditTemplate(int position) {
        TaskTemplate template = templateDataList.get(position);

        currentEditTemplateId = template.getId();
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

    private void clearForm() {
        currentEditTemplateId = null;
        currentSelectedBitmap = null;
        etTemplateTitle.setText("");
        imagePreview.setImageResource(R.drawable.ic_image_placeholder);
        tvFormHeader.setText(R.string.title_create_template);
        btnSaveTemplate.setText(R.string.btn_save_template);
    }

    private static final int DEFAULT_LIST_WIDTH = 500;

    // מחשב גובה ל-ListView כדי שכל השורות יוצגו בתוך ה-ScrollView
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

    private DatabaseReference getTemplatesReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(currentParentUserId).child("task_templates");
    }

    // Adapter פשוט שמתרגם כל תבנית משימה לשורה ב-ListView עם כותרת ותמונה
    private class TemplateListAdapter extends ArrayAdapter<TaskTemplate> {
        TemplateListAdapter() {
            super(ParentTaskTemplateActivity.this, 0, templateDataList);
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
