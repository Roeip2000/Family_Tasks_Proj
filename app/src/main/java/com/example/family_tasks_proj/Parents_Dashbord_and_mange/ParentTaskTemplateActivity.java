package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
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

/**
 * מסך ניהול תבניות משימה — יצירה, עריכה ומחיקה.
 *
 * ההורה יכול ליצור תבנית חדשה עם כותרת ותמונה,
 * לערוך תבנית קיימת (שם + תמונה), או למחוק תבנית.
 *
 * נתיב Firebase: /parents/{uid}/task_templates/{templateId}
 */
public class ParentTaskTemplateActivity extends AppCompatActivity {

    private EditText etTitle;
    private ImageView imgTask;
    private Button btnSave;
    private Button btnCancelEdit;
    private TextView tvFormTitle;
    private TextView tvNoTemplates;
    private ListView lvTemplates;

    private Bitmap correctedBitmap;

    // כאשר editingTemplateId != null, אנחנו במצב עריכה (ולא יצירה)
    private String editingTemplateId;

    // רשימת התבניות שנטענו מ-Firebase
    private final List<TaskTemplate> templateList = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

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
        btnSave = findViewById(R.id.btnSave);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvNoTemplates = findViewById(R.id.tvNoTemplates);
        lvTemplates = findViewById(R.id.lvTemplates);

        Button btnPickImage = findViewById(R.id.btnPickImage);

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> saveOrUpdateTemplate());
        btnCancelEdit.setOnClickListener(v -> resetForm());

        // לחיצה ארוכה על תבנית ברשימה — פותחת תפריט עריכה/מחיקה
        lvTemplates.setOnItemLongClickListener((parent, view, position, id) -> {
            showTemplateOptionsDialog(position);
            return true;
        });

        loadTemplates();
    }

    // טוען את כל התבניות מ-Firebase ומציג אותן ברשימה
    private void loadTemplates() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        getTemplatesRef(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        templateList.clear();
                        List<String> titles = new ArrayList<>();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            TaskTemplate template = snap.getValue(TaskTemplate.class);
                            if (template == null) continue;
                            if (template.id == null) template.id = snap.getKey();

                            templateList.add(template);
                            titles.add(template.title != null ? template.title : "");
                        }

                        lvTemplates.setAdapter(new ArrayAdapter<>(
                                ParentTaskTemplateActivity.this,
                                android.R.layout.simple_list_item_1,
                                titles));

                        // הצגת/הסתרת הודעה אם אין תבניות
                        boolean empty = templateList.isEmpty();
                        tvNoTemplates.setVisibility(empty ? View.VISIBLE : View.GONE);
                        lvTemplates.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentTaskTemplateActivity.this,
                                getString(R.string.error_loading_data, error.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // שומר תבנית חדשה או מעדכן תבנית קיימת (לפי editingTemplateId)
    private void saveOrUpdateTemplate() {
        String title = etTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.template_missing_title, Toast.LENGTH_SHORT).show();
            return;
        }

        // ביצירה חדשה — חייב תמונה. בעריכה — תמונה אופציונלית (שומר את הקיימת)
        if (editingTemplateId == null && correctedBitmap == null) {
            Toast.makeText(this, R.string.template_missing_title_or_image, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.template_parent_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית המפה לשמירה
        String templateId = editingTemplateId != null ? editingTemplateId : UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("id", templateId);
        data.put("title", title);

        // אם נבחרה תמונה חדשה — ממירים ושומרים
        if (correctedBitmap != null) {
            String imageBase64 = ImageHelper.bitmapToBase64(correctedBitmap);
            if (imageBase64 == null) {
                Toast.makeText(this, R.string.error_image_conversion, Toast.LENGTH_SHORT).show();
                return;
            }
            data.put("imageBase64", imageBase64);
        }

        boolean isEdit = editingTemplateId != null;

        // כתיבה ל-Firebase — updateChildren שומר שדות קיימים שלא נשלחו
        getTemplatesRef(user.getUid())
                .child(templateId)
                .updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            isEdit ? R.string.template_updated_success : R.string.template_saved_success,
                            Toast.LENGTH_SHORT).show();
                    resetForm();
                    loadTemplates();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.error_save_generic, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
    }

    // תפריט אפשרויות לתבנית — עריכה או מחיקה
    private void showTemplateOptionsDialog(int position) {
        if (position < 0 || position >= templateList.size()) return;

        TaskTemplate template = templateList.get(position);
        String[] options = {
                getString(R.string.template_option_edit),
                getString(R.string.template_option_delete)
        };

        new AlertDialog.Builder(this)
                .setTitle(template.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startEditTemplate(template);
                    } else {
                        showDeleteConfirmDialog(template);
                    }
                })
                .show();
    }

    // כניסה למצב עריכה — ממלא את הטופס בנתוני התבנית הקיימת
    private void startEditTemplate(TaskTemplate template) {
        editingTemplateId = template.id;
        etTitle.setText(template.title);
        correctedBitmap = null;

        // מציג את התמונה הקיימת של התבנית
        if (template.imageBase64 != null) {
            Bitmap bmp = ImageHelper.base64ToBitmap(template.imageBase64);
            if (bmp != null) {
                imgTask.setImageBitmap(bmp);
            }
        }

        tvFormTitle.setText(R.string.template_form_title_edit);
        btnCancelEdit.setVisibility(View.VISIBLE);
    }

    // דיאלוג אישור לפני מחיקה
    private void showDeleteConfirmDialog(TaskTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.template_delete_title)
                .setMessage(getString(R.string.template_delete_message, template.title))
                .setPositiveButton(R.string.template_option_delete, (dialog, which) ->
                        deleteTemplate(template))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מחיקת תבנית מ-Firebase
    private void deleteTemplate(TaskTemplate template) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || template.id == null) return;

        getTemplatesRef(user.getUid())
                .child(template.id)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.template_deleted_success, Toast.LENGTH_SHORT).show();
                    resetForm();
                    loadTemplates();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.error_save_generic, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
    }

    // מאפס את הטופס חזרה למצב "יצירת תבנית חדשה"
    private void resetForm() {
        editingTemplateId = null;
        correctedBitmap = null;
        etTitle.setText("");
        imgTask.setImageResource(android.R.drawable.ic_menu_gallery);
        tvFormTitle.setText(R.string.template_form_title_new);
        btnCancelEdit.setVisibility(View.GONE);
    }

    // מחזיר reference לנתיב תבניות ב-Firebase
    private DatabaseReference getTemplatesRef(String uid) {
        return FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(uid)
                .child("task_templates");
    }
}
