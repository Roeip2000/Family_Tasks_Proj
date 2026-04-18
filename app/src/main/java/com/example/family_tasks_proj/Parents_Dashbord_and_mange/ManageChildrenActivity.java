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
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
                if (uri == null) return;
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
        if (parentUid == null) { finish(); return; }
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
        String f = etFirstName.getText().toString().trim(), l = etLastName.getText().toString().trim();
        if (f.isEmpty() || l.isEmpty()) { Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show(); return; }

        String id = editingChildId != null ? editingChildId : childrenRef().push().getKey();
        if (id == null) return;
        String img = selectedChildPhoto != null ? ImageHelper.bitmapToBase64(selectedChildPhoto) : editingChildOldImageBase64;
        btnAddChild.setEnabled(false);

        com.google.android.gms.tasks.Task<Void> t;
        if (editingChildId == null) t = childrenRef().child(id).setValue(new Child(f, l, img));
        else {
            Map<String, Object> u = new HashMap<>();
            u.put("firstName", f); u.put("lastName", l);
            if (img != null) u.put("profileImageBase64", img);
            t = childrenRef().child(id).updateChildren(u);
        }

        t.addOnSuccessListener(v -> {
            btnAddChild.setEnabled(true);
            Toast.makeText(this, getString(editingChildId == null ? R.string.manage_children_added_success : R.string.manage_children_updated_success, f, l), Toast.LENGTH_SHORT).show();
            resetForm(); loadChildren();
        }).addOnFailureListener(e -> {
            btnAddChild.setEnabled(true);
            Toast.makeText(this, getString(R.string.error_save_generic, e.getMessage()), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChildren() {
        childrenRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childItems.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    childItems.add(new ChildItem(s.getKey(), s.child("firstName").getValue(String.class),
                            s.child("lastName").getValue(String.class), s.child("profileImageBase64").getValue(String.class)));
                }
                childListAdapter.notifyDataSetChanged();
                updateListViewHeight(lvChildren);
                tvNoChildren.setVisibility(childItems.isEmpty() ? View.VISIBLE : View.GONE);
                lvChildren.setVisibility(childItems.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;
        etFirstName.setText(item.firstName);
        etLastName.setText(item.lastName);
        if (item.profileImageBase64 != null) {
            imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(ImageHelper.base64ToBitmap(item.profileImageBase64)));
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
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, isEdit ? R.color.primary : R.color.accent)));
        btnCancelEdit.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        tvFormTitle.setText(isEdit ? R.string.manage_children_form_title_edit : R.string.manage_children_form_title_new);
    }

    private void showChildOptionsDialog(int pos) {
        String[] options = {getString(R.string.manage_children_option_edit), getString(R.string.manage_children_option_delete)};
        new AlertDialog.Builder(this).setTitle(getChildDisplayName(childItems.get(pos))).setItems(options, (d, w) -> {
            if (w == 0) enterEditMode(pos); else showDeleteChildDialog(pos);
        }).setNegativeButton(R.string.action_cancel, null).show();
    }

    private void showDeleteChildDialog(int pos) {
        ChildItem item = childItems.get(pos);
        new AlertDialog.Builder(this).setTitle(R.string.manage_children_delete_title)
                .setMessage(getString(R.string.manage_children_delete_message, getChildDisplayName(item)))
                .setPositiveButton(R.string.manage_children_option_delete, (d, w) -> 
                    childrenRef().child(item.id).removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.manage_children_deleted_success, getChildDisplayName(item)), Toast.LENGTH_SHORT).show();
                        if (item.id.equals(editingChildId)) resetForm();
                        loadChildren();
                    })).setNegativeButton(R.string.action_cancel, null).show();
    }

    private DatabaseReference childrenRef() {
        return database.child("parents").child(parentUid).child("children");
    }

    private String getChildDisplayName(ChildItem item) {
        return NameUtils.fullNameOrDefault(item.firstName, item.lastName, getString(R.string.default_child_name));
    }

    private void updateListViewHeight(ListView lv) {
        ListAdapter adapter = lv.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(lv.getWidth() > 0 ? lv.getWidth() : getResources().getDisplayMetrics().widthPixels - 100, View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, lv);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams p = lv.getLayoutParams();
        p.height = totalHeight + (lv.getDividerHeight() * (adapter.getCount() - 1));
        lv.setLayoutParams(p);
    }

    private class ChildListAdapter extends ArrayAdapter<ChildItem> {
        ChildListAdapter() { super(ManageChildrenActivity.this, 0, childItems); }
        @NonNull @Override public View getView(int pos, View v, @NonNull ViewGroup p) {
            if (v == null) v = getLayoutInflater().inflate(R.layout.item_manage_child, p, false);
            ChildItem item = getItem(pos);
            if (item == null) return v;
            ((TextView) v.findViewById(R.id.tvChildFullName)).setText(getChildDisplayName(item));
            ImageView iv = v.findViewById(R.id.ivChildThumb);
            if (item.profileImageBase64 != null) {
                iv.setImageBitmap(ImageHelper.getCircularBitmap(ImageHelper.base64ToBitmap(item.profileImageBase64)));
            } else {
                iv.setImageResource(R.drawable.ic_avatar_placeholder);
            }
            return v;
        }
    }

    private static class ChildItem {
        String id, firstName, lastName, profileImageBase64;
        ChildItem(String id, String f, String l, String img) { this.id = id; this.firstName = f; this.lastName = l; this.profileImageBase64 = img; }
    }
}
