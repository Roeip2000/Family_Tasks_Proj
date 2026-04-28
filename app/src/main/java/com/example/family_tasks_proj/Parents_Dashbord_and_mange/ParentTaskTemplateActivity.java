package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

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

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskTemplate;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
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

/** מסך לניהול תבניות משימה. מאפשר להורה ליצור תבניות מוכנות מראש עם תמונה וכמות כוכבים. */
public class ParentTaskTemplateActivity extends AppCompatActivity {

    private static final int MIN_STARS = 1;
    private static final int MAX_STARS = 100;

    private EditText etTitle;
    private EditText etStarsWorth;
    private ImageView imgTask;
    private Button btnSave;
    private Button btnCancelEdit;
    private TextView tvFormTitle;
    private TextView tvNoTemplates;
    private ListView lvTemplates;

    private Bitmap correctedBitmap;
    private String editingTemplateId;

    private final List<TaskTemplate> templateList = new ArrayList<>();
    private TemplateListAdapter templateListAdapter;

    // --- נושא במחוון 6.14: ActivityResultContract ---
    // --- נושא במחוון 10.3: Camera & Gallery ---
    // הגדרת "חוזה" לפתיחת הגלריה לבחירת תמונה וחזרה עם התוצאה (URI של התמונה)
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            // הפונקציה הזו מופעלת כשהמשתמש מסיים לבחור תמונה מהגלריה
                            handleTemplateImageResult(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_task_template);

        bindViews();
        bindActions();
        loadTemplates();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etStarsWorth = findViewById(R.id.etStarsWorth);
        imgTask = findViewById(R.id.imgTask);
        btnSave = findViewById(R.id.btnSave);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvNoTemplates = findViewById(R.id.tvNoTemplates);
        lvTemplates = findViewById(R.id.lvTemplates);

        templateListAdapter = new TemplateListAdapter();
        lvTemplates.setAdapter(templateListAdapter);
    }

    private void bindActions() {
        findViewById(R.id.btnPickImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePicker.launch("image/*");
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOrUpdateTemplate();
            }
        });

        btnCancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetForm();
            }
        });

        lvTemplates.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                showTemplateOptionsDialog(position);
            }
        });

        findViewById(R.id.btnBackToDashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // מטפל בתמונה שנבחרה מהגלריה
    private void handleTemplateImageResult(Uri uri) {
        if (uri == null) {
            return;
        }
        correctedBitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
        if (correctedBitmap == null) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            return;
        }
        imgTask.setImageBitmap(correctedBitmap);
    }

    // טוען את התבניות הקיימות מ-Firebase
    private void loadTemplates() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        getTemplatesRef(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                templateList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    TaskTemplate template = snap.getValue(TaskTemplate.class);
                    if (template != null) {
                        if (template.getId() == null) {
                            template.setId(snap.getKey());
                        }
                        templateList.add(template);
                    }
                }
                templateListAdapter.notifyDataSetChanged();
                updateTemplateListVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentTaskTemplateActivity.this, getString(R.string.error_loading_data, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTemplateListVisibility() {
        boolean empty = templateList.isEmpty();
        if (empty) {
            tvNoTemplates.setVisibility(View.VISIBLE);
            lvTemplates.setVisibility(View.GONE);
        } else {
            tvNoTemplates.setVisibility(View.GONE);
            lvTemplates.setVisibility(View.VISIBLE);
        }
    }

    // שומר תבנית חדשה או מעדכן קיימת
    private void saveOrUpdateTemplate() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty() || (editingTemplateId == null && correctedBitmap == null)) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        int stars;
        try {
            stars = Integer.parseInt(etStarsWorth.getText().toString().trim());
            if (stars < MIN_STARS || stars > MAX_STARS) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            Toast.makeText(this, R.string.template_stars_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_parent_session_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        submitTemplateToFirebase(user.getUid(), title, stars);
    }

    // מבצע את השמירה בפועל מול השרת
    private void submitTemplateToFirebase(String uid, String title, int stars) {
        String templateId;
        if (editingTemplateId != null) {
            templateId = editingTemplateId;
        } else {
            templateId = UUID.randomUUID().toString();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", templateId);
        data.put("title", title);
        data.put("starsWorth", stars);

        if (correctedBitmap != null) {
            String imageBase64 = ImageHelper.bitmapToBase64(correctedBitmap);
            if (imageBase64 == null) {
                Toast.makeText(this, R.string.error_image_conversion, Toast.LENGTH_SHORT).show();
                return;
            }
            data.put("imageBase64", imageBase64);
        }

        btnSave.setEnabled(false);
        getTemplatesRef(uid).child(templateId).updateChildren(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentTaskTemplateActivity.this, R.string.template_saved_success, Toast.LENGTH_SHORT).show();
                resetForm();
                loadTemplates();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                btnSave.setEnabled(true);
                Toast.makeText(ParentTaskTemplateActivity.this, getString(R.string.error_save_generic, exception.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מציג אפשרויות עבור תבנית שנבחרה
    private void showTemplateOptionsDialog(int position) {
        if (position < 0 || position >= templateList.size()) {
            return;
        }
        final TaskTemplate template = templateList.get(position);
        String[] options = {getString(R.string.template_option_edit), getString(R.string.template_option_delete)};
        new AlertDialog.Builder(this)
                .setTitle(template.getTitle())
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            startEditTemplate(template);
                        } else {
                            showDeleteTemplateDialog(template);
                        }
                    }
                })
                .show();
    }

    // מציג דיאלוג לאישור מחיקה
    private void showDeleteTemplateDialog(final TaskTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.template_delete_title)
                .setMessage(getString(R.string.template_delete_message, template.getTitle()))
                .setPositiveButton(R.string.template_option_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTemplate(template);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteTemplate(TaskTemplate template) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || template.getId() == null) {
            return;
        }
        getTemplatesRef(user.getUid()).child(template.getId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentTaskTemplateActivity.this, R.string.template_deleted_success, Toast.LENGTH_SHORT).show();
                resetForm();
                loadTemplates();
            }
        });
    }

    private void startEditTemplate(TaskTemplate template) {
        editingTemplateId = template.getId();
        etTitle.setText(template.getTitle());
        etStarsWorth.setText(String.valueOf(template.safeStarsWorth()));
        if (template.getImageBase64() != null) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
            if (bitmap != null) {
                imgTask.setImageBitmap(bitmap);
            }
        }
        tvFormTitle.setText(R.string.template_form_title_edit);
        btnSave.setText(R.string.template_save_changes);
        btnCancelEdit.setVisibility(View.VISIBLE);
    }

    private void resetForm() {
        editingTemplateId = null;
        correctedBitmap = null;
        etTitle.setText("");
        etStarsWorth.setText("");
        imgTask.setImageResource(R.drawable.ic_image_placeholder);
        tvFormTitle.setText(R.string.template_form_title_new);
        btnSave.setText(R.string.btn_save_template);
        btnSave.setEnabled(true);
        btnCancelEdit.setVisibility(View.GONE);
    }

    private DatabaseReference getTemplatesRef(String uid) {
        return FirebaseDatabase.getInstance().getReference("parents").child(uid).child("task_templates");
    }

    // מתאם להצגת רשימת התבניות
    private class TemplateListAdapter extends ArrayAdapter<TaskTemplate> {
        TemplateListAdapter() {
            super(ParentTaskTemplateActivity.this, 0, templateList);
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
                ((TextView) convertView.findViewById(R.id.tvTemplateStars)).setText(getString(R.string.template_item_stars, (int) template.safeStarsWorth()));
                ImageView imageView = convertView.findViewById(R.id.ivTemplateThumb);
                if (template.getImageBase64() != null) {
                    Bitmap bitmap = ImageHelper.base64ToBitmap(template.getImageBase64());
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.ic_image_placeholder);
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
            return convertView;
        }
    }
}
