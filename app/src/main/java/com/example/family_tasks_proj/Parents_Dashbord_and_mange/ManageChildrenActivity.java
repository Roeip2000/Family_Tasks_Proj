package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.model.Child;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
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

/**
 * מסך ניהול ילדים — הוספה, עריכה ומחיקה.
 *
 * ההורה רואה רשימת ילדים רשומים ויכול:
 * - להוסיף ילד חדש (שם + תמונה)
 * - ללחוץ על ילד ברשימה כדי לערוך/למחוק
 *
 * נתיב Firebase: /parents/{uid}/children/{childId}
 */
public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName;
    private EditText etLastName;
    private Button btnAddChild;
    private Button btnCancelEdit;
    private ListView lvChildren;
    private TextView tvNoChildren;
    private ImageView imgChildPhoto;

    private final List<ChildItem> childItems = new ArrayList<>();

    private DatabaseReference database;
    private String parentUid;
    private Bitmap selectedChildPhoto;
    private String editingChildId;
    private String editingChildOldImageBase64;
    private ChildListAdapter childListAdapter;

    private final ActivityResultLauncher<String> childImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                Bitmap bitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (bitmap == null) {
                    Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedChildPhoto = bitmap;
                imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        bindViews();
        bindActions();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.manage_children_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentUid = user.getUid();
        database = FirebaseDatabase.getInstance().getReference();

        childListAdapter = new ChildListAdapter();
        lvChildren.setAdapter(childListAdapter);
        lvChildren.setOnItemClickListener((parent, view, position, id) -> showChildOptionsDialog(position));

        loadChildren();
    }

    private void bindViews() {
        imgChildPhoto = findViewById(R.id.imgChildPhoto);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        lvChildren = findViewById(R.id.lvChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);

        Button btnPickChildPhoto = findViewById(R.id.btnPickChildPhoto);
        btnPickChildPhoto.setOnClickListener(v -> childImagePicker.launch("image/*"));

        // חזרה לדשבורד ההורה
        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> finish());
    }

    private void bindActions() {
        btnAddChild.setOnClickListener(v -> {
            if (editingChildId == null) {
                addChild();
            } else {
                updateExistingChild();
            }
        });

        btnCancelEdit.setOnClickListener(v -> resetForm());
    }

    private void addChild() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = childrenRef().push().getKey();

        if (childId == null) {
            Toast.makeText(this, R.string.manage_children_error_create_child_id, Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = selectedChildPhoto == null
                ? null
                : ImageHelper.bitmapToBase64(selectedChildPhoto);

        Child child = new Child(firstName, lastName, imageBase64);
        btnAddChild.setEnabled(false);

        // שומר את הילד החדש תחת ההורה שמחובר כרגע
        childrenRef()
                .child(childId)
                .setValue(child)
                .addOnSuccessListener(aVoid -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(
                            this,
                            getString(R.string.manage_children_added_success, firstName, lastName),
                            Toast.LENGTH_SHORT
                    ).show();
                    resetForm();
                    loadChildren();
                })
                .addOnFailureListener(e -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(
                            this,
                            getString(R.string.error_with_details, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateExistingChild() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);

        String imageBase64 = selectedChildPhoto == null
                ? editingChildOldImageBase64
                : ImageHelper.bitmapToBase64(selectedChildPhoto);

        if (imageBase64 != null) {
            updates.put("profileImageBase64", imageBase64);
        }

        btnAddChild.setEnabled(false);
        childrenRef()
                .child(editingChildId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(
                            this,
                            getString(R.string.manage_children_updated_success, firstName, lastName),
                            Toast.LENGTH_SHORT
                    ).show();
                    resetForm();
                    loadChildren();
                })
                .addOnFailureListener(e -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(
                            this,
                            getString(R.string.manage_children_error_update, e.getMessage()),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadChildren() {
        childrenRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childItems.clear();

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String childId = childSnapshot.getKey();
                            if (childId == null) {
                                continue;
                            }

                            String firstName = childSnapshot.child("firstName").getValue(String.class);
                            String lastName = childSnapshot.child("lastName").getValue(String.class);
                            String imageBase64 = childSnapshot.child("profileImageBase64").getValue(String.class);
                            childItems.add(new ChildItem(childId, firstName, lastName, imageBase64));
                        }

                        childListAdapter.notifyDataSetChanged();

                        boolean hasChildren = !childItems.isEmpty();
                        tvNoChildren.setVisibility(hasChildren ? View.GONE : View.VISIBLE);
                        lvChildren.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                ManageChildrenActivity.this,
                                getString(R.string.manage_children_error_loading_children, error.getMessage()),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // כניסה למצב עריכה — ממלא את הטופס בנתוני הילד שנבחר
    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;
        selectedChildPhoto = null;

        etFirstName.setText(safeText(item.firstName));
        etLastName.setText(safeText(item.lastName));

        if (item.profileImageBase64 == null || item.profileImageBase64.isEmpty()) {
            imgChildPhoto.setImageDrawable(null);
        } else {
            Bitmap bitmap = ImageHelper.base64ToBitmap(item.profileImageBase64);
            imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
        }

        btnAddChild.setText(R.string.manage_children_save_changes);
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(0xFF1976D2));
        btnCancelEdit.setVisibility(View.VISIBLE);
        etFirstName.requestFocus();
    }

    private void resetForm() {
        editingChildId = null;
        editingChildOldImageBase64 = null;
        selectedChildPhoto = null;

        etFirstName.setText("");
        etLastName.setText("");
        imgChildPhoto.setImageDrawable(null);

        btnAddChild.setText(R.string.add_child);
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50));
        btnCancelEdit.setVisibility(View.GONE);
    }

    private void showChildOptionsDialog(int position) {
        if (position < 0 || position >= childItems.size()) return;

        String childName = getChildDisplayName(childItems.get(position));
        String[] options = {
                getString(R.string.manage_children_option_edit),
                getString(R.string.manage_children_option_delete)
        };

        new AlertDialog.Builder(this)
                .setTitle(childName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) enterEditMode(position);
                    else showDeleteChildDialog(position);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showDeleteChildDialog(int position) {
        if (position < 0 || position >= childItems.size()) return;

        ChildItem item = childItems.get(position);
        String childName = getChildDisplayName(item);

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_children_delete_title)
                .setMessage(getString(R.string.manage_children_delete_message, childName))
                .setPositiveButton(R.string.manage_children_option_delete, (dialog, which) ->
                        childrenRef()
                                .child(item.id)
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(
                                            this,
                                            getString(R.string.manage_children_deleted_success, childName),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    if (item.id.equals(editingChildId)) {
                                        resetForm();
                                    }
                                    loadChildren();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        this,
                                        getString(R.string.manage_children_error_delete, e.getMessage()),
                                        Toast.LENGTH_SHORT
                                ).show()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private DatabaseReference childrenRef() {
        return database.child("parents")
                .child(parentUid)
                .child("children");
    }

    private String getChildDisplayName(ChildItem item) {
        return NameUtils.fullNameOrDefault(item.firstName, item.lastName,
                getString(R.string.default_child_name));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private class ChildListAdapter extends ArrayAdapter<ChildItem> {

        ChildListAdapter() {
            super(ManageChildrenActivity.this, 0, childItems);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_manage_child, parent, false);
            }

            ChildItem item = getItem(position);
            if (item == null) {
                return convertView;
            }

            ImageView ivChildThumb = convertView.findViewById(R.id.ivChildThumb);
            TextView tvChildFullName = convertView.findViewById(R.id.tvChildFullName);

            tvChildFullName.setText(getChildDisplayName(item));

            if (item.profileImageBase64 == null || item.profileImageBase64.isEmpty()) {
                ivChildThumb.setImageDrawable(null);
                return convertView;
            }

            Bitmap bitmap = ImageHelper.base64ToBitmap(item.profileImageBase64);
            ivChildThumb.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
            return convertView;
        }
    }

    private static class ChildItem {
        private final String id;
        private final String firstName;
        private final String lastName;
        private final String profileImageBase64;

        private ChildItem(String id, String firstName, String lastName, String profileImageBase64) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.profileImageBase64 = profileImageBase64;
        }
    }
}
