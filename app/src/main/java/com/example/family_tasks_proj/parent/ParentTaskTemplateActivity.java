package com.example.family_tasks_proj.parent;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// מסך לניהול תבניות משימה (הוספה, עריכה, מחיקה)
public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTemplateTitle;
    private ImageView imagePreview;
    private Button btnSaveTemplate, btnCancelTemplateEdit, btnBackToMain;
    private ListView listViewTemplates;
    private TextView tvFormHeader, tvNoTemplatesInfo;

    private final List<TaskTemplate> templateDataList = new ArrayList<>();
    private TemplateListAdapter templateAdapter;
    private String currentEditTemplateId = null;
    private Bitmap currentSelectedBitmap = null;
    private String currentParentUserId;

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentParentUserId = user.getUid();

        initViews();
        setupEvents();
        setupList();
        loadFromFirebase();
    }

    private void initViews() {
        etTemplateTitle = findViewById(R.id.etTitle);
        imagePreview = findViewById(R.id.imgTask);
        btnSaveTemplate = findViewById(R.id.btnSave);
        btnCancelTemplateEdit = findViewById(R.id.btnCancelEdit);
        btnBackToMain = findViewById(R.id.btnBackToDashboard);
        listViewTemplates = findViewById(R.id.lvTemplates);
        tvFormHeader = findViewById(R.id.tvFormTitle);
        tvNoTemplatesInfo = findViewById(R.id.tvNoTemplates);
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

        btnCancelTemplateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearForm();
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
            public void onItemClick(AdapterView<?> p, View v, int position, long id) {
                showOptions(position);
            }
        });
    }

    private void loadFromFirebase() {
        // תבניות נטענות בזמן אמת מה-Firebase, כדי שכל שמירה או מחיקה תופיע מיד ברשימה.
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
                if (templateDataList.isEmpty()) {
                    tvNoTemplatesInfo.setVisibility(View.VISIBLE);
                } else {
                    tvNoTemplatesInfo.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // אם טעינת התבניות נכשלה - מציגים הודעה במקום מסך ריק בלי הסבר.
                Toast.makeText(ParentTaskTemplateActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSave() {
        String title = etTemplateTitle.getText().toString().trim();

        // תבנית חייבת שם כדי שאפשר יהיה להעתיק אותו למשימה שמקצים לילד.
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_template_missing_details, Toast.LENGTH_SHORT).show();
            return;
        }

        String templateId;
        if (currentEditTemplateId != null) {
            templateId = currentEditTemplateId;
        } else {
            templateId = UUID.randomUUID().toString();
        }
        // הפנייה לנתיב של התבנית ב-Firebase.
        DatabaseReference templateRef = getTemplatesReference().child(templateId);

        // כתיבת כל שדה ישירות לנתיב שלו ב-Firebase במקום להשתמש ב-HashMap.
        templateRef.child("id").setValue(templateId);
        templateRef.child("title").setValue(title);

        if (currentSelectedBitmap != null) {
            String base64 = ImageHelper.bitmapToBase64(currentSelectedBitmap);
            templateRef.child("imageBase64").setValue(base64);
        }

        // מוסיפים מאזין לסיום הפעולה רק על השדה האחרון או על הפנייה הכללית.
        templateRef.child("id").setValue(templateId).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentTaskTemplateActivity.this, R.string.toast_template_saved, Toast.LENGTH_SHORT).show();
                clearForm();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ParentTaskTemplateActivity.this, getString(R.string.error_with_details, exception.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void delete(String templateId) {
        getTemplatesReference().child(templateId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentTaskTemplateActivity.this, R.string.toast_template_deleted, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ParentTaskTemplateActivity.this, getString(R.string.error_with_details, exception.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOptions(int position) {
        final TaskTemplate template = templateDataList.get(position);
        String[] options = {
                getString(R.string.dialog_option_edit),
                getString(R.string.dialog_option_delete)
        };
        new AlertDialog.Builder(this).setTitle(template.getTitle()).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (which == 0) {
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
                    btnCancelTemplateEdit.setVisibility(View.VISIBLE);
                } else {
                    delete(template.getId());
                }
            }
        }).setNegativeButton(R.string.dialog_cancel, null).show();
    }

    private void clearForm() {
        currentEditTemplateId = null;
        currentSelectedBitmap = null;
        etTemplateTitle.setText("");
        imagePreview.setImageResource(R.drawable.ic_image_placeholder);
        tvFormHeader.setText(R.string.title_create_template);
        btnSaveTemplate.setText(R.string.btn_save_template);
        btnCancelTemplateEdit.setVisibility(View.GONE);
    }

    private void fitListHeight(ListView listView) {
        if (listView.getAdapter() == null) {
            return;
        }

        int listWidth = (listView.getWidth() > 0) ? listView.getWidth() : 500;
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
                ((TextView) convertView.findViewById(R.id.tvTemplateTitle)).setText(template.getTitle());
                ImageView imageThumb = convertView.findViewById(R.id.ivTemplateThumb);
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
