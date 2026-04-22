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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.model.Child;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private Button btnAddChild, btnCancelEdit;
    private ListView lvChildren;
    private TextView tvNoChildren, tvFormTitle;
    private ImageView imgChildPhoto;

    private final List<ChildItem> childItems = new ArrayList<>();

    private DatabaseReference database;
    private String parentUid, editingChildId, editingChildOldImageBase64;
    private Bitmap selectedChildPhoto;
    private ChildListAdapter childListAdapter;

    private final ActivityResultLauncher<String> childImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                // אותה תשתית תמונות משמשת גם לילדים וגם להורה
                selectedChildPhoto = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (selectedChildPhoto == null) {
                    Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                    return;
                }

                imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(selectedChildPhoto));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        bindViews();

        parentUid = FirebaseAuth.getInstance().getUid();
        if (parentUid == null) {
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance().getReference();

        childListAdapter = new ChildListAdapter();
        lvChildren.setAdapter(childListAdapter);
        lvChildren.setOnItemClickListener((p, v, pos, id) -> showChildOptionsDialog(pos));

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
        tvFormTitle = findViewById(R.id.tvFormTitle);

        findViewById(R.id.btnPickChildPhoto).setOnClickListener(v -> childImagePicker.launch("image/*"));
        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> finish());
        btnAddChild.setOnClickListener(v -> saveChild());
        btnCancelEdit.setOnClickListener(v -> resetForm());
    }

    private void saveChild() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = editingChildId != null ? editingChildId : childrenRef().push().getKey();
        if (childId == null) {
            return;
        }

        String imageBase64 = selectedChildPhoto != null
                ? ImageHelper.bitmapToBase64(selectedChildPhoto)
                : editingChildOldImageBase64;

        btnAddChild.setEnabled(false);

        com.google.android.gms.tasks.Task<Void> saveTask;
        if (editingChildId == null) {
            // ילד חדש נשמר כענף חדש תחת /parents/{uid}/children
            saveTask = childrenRef().child(childId).setValue(new Child(firstName, lastName, imageBase64));
        } else {
            // בעריכה מעדכנים רק את השדות של הילד ולא נוגעים במשימות שלו
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", firstName);
            updates.put("lastName", lastName);
            if (imageBase64 != null) {
                updates.put("profileImageBase64", imageBase64);
            }
            saveTask = childrenRef().child(childId).updateChildren(updates);
        }

        saveTask.addOnSuccessListener(v -> {
            btnAddChild.setEnabled(true);
            Toast.makeText(
                    this,
                    getString(editingChildId == null
                            ? R.string.manage_children_added_success
                            : R.string.manage_children_updated_success, firstName, lastName),
                    Toast.LENGTH_SHORT
            ).show();
            resetForm();
            loadChildren();
        }).addOnFailureListener(e -> {
            btnAddChild.setEnabled(true);
            Toast.makeText(this, getString(R.string.error_save_generic, e.getMessage()), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChildren() {
        // רשימת הילדים נטענת מהנתיב הקבוע של ההורה המחובר
        childrenRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childItems.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    childItems.add(new ChildItem(
                            childSnapshot.getKey(),
                            childSnapshot.child("firstName").getValue(String.class),
                            childSnapshot.child("lastName").getValue(String.class),
                            childSnapshot.child("profileImageBase64").getValue(String.class)
                    ));
                }

                childListAdapter.notifyDataSetChanged();
                updateListViewHeight(lvChildren);
                tvNoChildren.setVisibility(childItems.isEmpty() ? View.VISIBLE : View.GONE);
                lvChildren.setVisibility(childItems.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;

        etFirstName.setText(item.firstName);
        etLastName.setText(item.lastName);

        if (item.profileImageBase64 != null) {
            imgChildPhoto.setImageBitmap(
                    ImageHelper.getCircularBitmap(ImageHelper.base64ToBitmap(item.profileImageBase64)));
        } else {
            imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        toggleUI(true);
    }

    private void resetForm() {
        editingChildId = null;
        editingChildOldImageBase64 = null;
        selectedChildPhoto = null;

        etFirstName.setText("");
        etLastName.setText("");
        imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);

        toggleUI(false);
    }

    private void toggleUI(boolean isEdit) {
        btnAddChild.setText(isEdit ? R.string.manage_children_save_changes : R.string.add_child);
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                this,
                isEdit ? R.color.primary : R.color.accent
        )));
        btnCancelEdit.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        tvFormTitle.setText(isEdit
                ? R.string.manage_children_form_title_edit
                : R.string.manage_children_form_title_new);
    }

    private void showChildOptionsDialog(int position) {
        String[] options = {
                getString(R.string.manage_children_option_edit),
                getString(R.string.manage_children_option_delete)
        };

        new AlertDialog.Builder(this)
                .setTitle(getChildDisplayName(childItems.get(position)))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        enterEditMode(position);
                    } else {
                        showDeleteChildDialog(position);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showDeleteChildDialog(int position) {
        ChildItem item = childItems.get(position);

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_children_delete_title)
                .setMessage(getString(R.string.manage_children_delete_message, getChildDisplayName(item)))
                .setPositiveButton(R.string.manage_children_option_delete, (dialog, which) ->
                        // מחיקת ילד מוחקת גם את המשימות שנמצאות מתחתיו באותו ענף
                        childrenRef().child(item.id).removeValue().addOnSuccessListener(aVoid -> {
                            Toast.makeText(
                                    this,
                                    getString(R.string.manage_children_deleted_success, getChildDisplayName(item)),
                                    Toast.LENGTH_SHORT
                            ).show();
                            if (item.id.equals(editingChildId)) {
                                resetForm();
                            }
                            loadChildren();
                        }))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private DatabaseReference childrenRef() {
        // נתיב הילדים של ההורה: /parents/{uid}/children
        return database.child("parents").child(parentUid).child("children");
    }

    private String getChildDisplayName(ChildItem item) {
        return NameUtils.fullNameOrDefault(
                item.firstName,
                item.lastName,
                getString(R.string.default_child_name)
        );
    }

    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                listView.getWidth() > 0
                        ? listView.getWidth()
                        : getResources().getDisplayMetrics().widthPixels - 100,
                View.MeasureSpec.AT_MOST
        );

        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
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

            ((TextView) convertView.findViewById(R.id.tvChildFullName))
                    .setText(getChildDisplayName(item));

            ImageView imageView = convertView.findViewById(R.id.ivChildThumb);
            if (item.profileImageBase64 != null) {
                imageView.setImageBitmap(
                        ImageHelper.getCircularBitmap(ImageHelper.base64ToBitmap(item.profileImageBase64)));
            } else {
                imageView.setImageResource(R.drawable.ic_avatar_placeholder);
            }

            return convertView;
        }
    }

    private static class ChildItem {
        String id, firstName, lastName, profileImageBase64;

        ChildItem(String id, String firstName, String lastName, String profileImageBase64) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.profileImageBase64 = profileImageBase64;
        }
    }
}
