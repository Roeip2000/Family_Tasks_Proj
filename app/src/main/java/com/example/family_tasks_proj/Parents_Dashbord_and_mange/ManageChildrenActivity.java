package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

/**
 * מסך ניהול ילדים של הורה.
 * ההורה מוסיף, עורך ומוחק ילדים תחת /parents/{uid}/children/{childId}.
 * עריכת ילד משנה רק שם ותמונה, ולא משנה את המשימות שלו.
 */
public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName;
    private EditText etLastName;
    private Button btnAddChild;
    private Button btnCancelEdit;
    private ListView lvChildren;
    private TextView tvNoChildren;
    private TextView tvFormTitle;
    private ImageView imgChildPhoto;

    private final List<ChildItem> childItems = new ArrayList<>();

    private DatabaseReference database;
    private String parentUid;
    private String editingChildId;
    private String editingChildOldImageBase64;
    private Bitmap selectedChildPhoto;
    private ChildListAdapter childListAdapter;

    private final ActivityResultLauncher<String> childImagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            handleChildImageResult(uri);
                        }
                    }
            );

    // יוצר את המסך, מזהה את ההורה המחובר, וטוען את רשימת הילדים
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        bindViews();

        parentUid = FirebaseAuth.getInstance().getUid();
        if (parentUid == null) {
            Toast.makeText(this, R.string.manage_children_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance().getReference();
        setupChildrenList();
        bindActions();
        loadChildren();
    }

    // מחבר את רכיבי המסך
    private void bindViews() {
        imgChildPhoto = findViewById(R.id.imgChildPhoto);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        lvChildren = findViewById(R.id.lvChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        tvFormTitle = findViewById(R.id.tvFormTitle);
    }

    // מגדיר את רשימת הילדים ואת הלחיצה על שורה
    private void setupChildrenList() {
        childListAdapter = new ChildListAdapter();
        lvChildren.setAdapter(childListAdapter);
        lvChildren.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                showChildOptionsDialog(position);
            }
        });
    }

    // מחבר כפתורי טופס וניווט
    private void bindActions() {
        findViewById(R.id.btnPickChildPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                childImagePicker.launch("image/*");
            }
        });

        findViewById(R.id.btnBackToDashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnAddChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChild();
            }
        });

        btnCancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetForm();
            }
        });
    }

    // מטפל בתמונה שנבחרה מהגלריה עבור פרופיל ילד
    private void handleChildImageResult(Uri uri) {
        if (uri == null) {
            return;
        }

        selectedChildPhoto = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
        if (selectedChildPhoto == null) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            return;
        }

        imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(selectedChildPhoto));
    }

    // שומר ילד חדש או מעדכן ילד קיים ב-Firebase
    private void saveChild() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (!validateChildForm(firstName, lastName)) {
            return;
        }

        String childId = getChildIdForSave();
        if (childId == null) {
            return;
        }

        String imageBase64 = getImageBase64ForSave();
        btnAddChild.setEnabled(false);

        if (editingChildId == null) {
            saveNewChild(childId, firstName, lastName, imageBase64);
        } else {
            updateExistingChild(childId, firstName, lastName, imageBase64);
        }
    }

    // בודק שהשם הפרטי ושם המשפחה מולאו
    private boolean validateChildForm(String firstName, String lastName) {
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // מחזיר id קיים בעריכה או id חדש ביצירת ילד
    private String getChildIdForSave() {
        if (editingChildId != null) {
            return editingChildId;
        }
        String childId = childrenRef().push().getKey();
        if (childId == null) {
            Toast.makeText(this, R.string.manage_children_error_create_child_id, Toast.LENGTH_SHORT).show();
        }
        return childId;
    }

    // מחזיר תמונה חדשה ב-Base64 או את התמונה הישנה בזמן עריכה
    private String getImageBase64ForSave() {
        if (selectedChildPhoto != null) {
            return ImageHelper.bitmapToBase64(selectedChildPhoto);
        }
        return editingChildOldImageBase64;
    }

    // שומר ילד חדש תחת /parents/{uid}/children/{childId}
    private void saveNewChild(String childId, String firstName, String lastName, String imageBase64) {
        DatabaseReference childRef = childrenRef().child(childId);
        Task<Void> saveTask = childRef.setValue(new Child(firstName, lastName, imageBase64));
        attachSaveChildListeners(saveTask, firstName, lastName);
    }

    // מעדכן פרטי ילד קיים בלי לגעת בענף tasks שלו
    private void updateExistingChild(String childId, String firstName, String lastName, String imageBase64) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        if (imageBase64 != null) {
            updates.put("profileImageBase64", imageBase64);
        }

        DatabaseReference childRef = childrenRef().child(childId);
        Task<Void> updateTask = childRef.updateChildren(updates);
        attachSaveChildListeners(updateTask, firstName, lastName);
    }

    // מחבר מאזיני הצלחה וכישלון לשמירת ילד
    private void attachSaveChildListeners(Task<Void> saveTask, final String firstName, final String lastName) {
        saveTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                handleChildSaved(firstName, lastName);
            }
        });

        saveTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                btnAddChild.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this,
                        getString(R.string.error_with_details, exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מציג הודעת הצלחה, מאפס טופס, וטוען ילדים מחדש
    private void handleChildSaved(String firstName, String lastName) {
        btnAddChild.setEnabled(true);

        int messageRes;
        if (editingChildId == null) {
            messageRes = R.string.manage_children_added_success;
        } else {
            messageRes = R.string.manage_children_updated_success;
        }

        Toast.makeText(this, getString(messageRes, firstName, lastName), Toast.LENGTH_SHORT).show();
        resetForm();
        loadChildren();
    }

    // טוען את הילדים מ-Firebase: /parents/{uid}/children
    private void loadChildren() {
        DatabaseReference childrenRef = childrenRef();
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleChildrenSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ממיר את נתוני הילדים לרשימה שמוצגת במסך
    private void handleChildrenSnapshot(DataSnapshot snapshot) {
        childItems.clear();

        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
            addChildItemFromSnapshot(childSnapshot);
        }

        childListAdapter.notifyDataSetChanged();
        updateListViewHeight(lvChildren);
        updateChildrenEmptyState();
    }

    // מוסיף ילד אחד מהרשומה שלו ב-Firebase
    private void addChildItemFromSnapshot(DataSnapshot childSnapshot) {
        childItems.add(new ChildItem(
                childSnapshot.getKey(),
                childSnapshot.child("firstName").getValue(String.class),
                childSnapshot.child("lastName").getValue(String.class),
                childSnapshot.child("profileImageBase64").getValue(String.class)
        ));
    }

    // מציג או מסתיר את הודעת הריק של הילדים
    private void updateChildrenEmptyState() {
        boolean isEmpty = childItems.isEmpty();
        tvNoChildren.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        lvChildren.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // מכניס את הטופס למצב עריכת ילד קיים
    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;

        etFirstName.setText(item.firstName);
        etLastName.setText(item.lastName);
        showChildPhoto(item.profileImageBase64);
        toggleUI(true);
    }

    // מציג תמונת ילד קיימת או תמונה חלופית
    private void showChildPhoto(String profileImageBase64) {
        if (profileImageBase64 == null) {
            imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        Bitmap bitmap = ImageHelper.base64ToBitmap(profileImageBase64);
        if (bitmap == null) {
            imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
    }

    // מאפס את הטופס למצב הוספת ילד חדש
    private void resetForm() {
        editingChildId = null;
        editingChildOldImageBase64 = null;
        selectedChildPhoto = null;

        etFirstName.setText("");
        etLastName.setText("");
        imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);

        toggleUI(false);
    }

    // משנה טקסטים וצבעים לפי מצב עריכה או הוספה
    private void toggleUI(boolean isEdit) {
        if (isEdit) {
            btnAddChild.setText(R.string.manage_children_save_changes);
        } else {
            btnAddChild.setText(R.string.add_child);
        }

        int colorRes;
        if (isEdit) {
            colorRes = R.color.primary;
        } else {
            colorRes = R.color.accent;
        }
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));

        btnCancelEdit.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        if (isEdit) {
            tvFormTitle.setText(R.string.manage_children_form_title_edit);
        } else {
            tvFormTitle.setText(R.string.manage_children_form_title_new);
        }
    }

    // פותח תפריט עריכה/מחיקה לילד שנבחר
    private void showChildOptionsDialog(final int position) {
        if (position < 0 || position >= childItems.size()) {
            return;
        }

        String[] options = {
                getString(R.string.manage_children_option_edit),
                getString(R.string.manage_children_option_delete)
        };

        new AlertDialog.Builder(this)
                .setTitle(getChildDisplayName(childItems.get(position)))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleChildOption(position, which);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מפעיל עריכה או מחיקה לפי הבחירה בדיאלוג
    private void handleChildOption(int position, int which) {
        if (which == 0) {
            enterEditMode(position);
        } else {
            showDeleteChildDialog(position);
        }
    }

    // פותח דיאלוג אישור לפני מחיקת ילד
    private void showDeleteChildDialog(int position) {
        final ChildItem item = childItems.get(position);

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_children_delete_title)
                .setMessage(getString(R.string.manage_children_delete_message, getChildDisplayName(item)))
                .setPositiveButton(R.string.manage_children_option_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteChild(item);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מוחק ילד מ-Firebase: /parents/{uid}/children/{childId}
    private void deleteChild(final ChildItem item) {
        DatabaseReference childRef = childrenRef().child(item.id);
        Task<Void> deleteTask = childRef.removeValue();

        deleteTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                handleChildDeleted(item);
            }
        });

        deleteTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ManageChildrenActivity.this,
                        getString(R.string.error_with_details, exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מטפל במחיקה מוצלחת של ילד ומרענן את הרשימה
    private void handleChildDeleted(ChildItem item) {
        Toast.makeText(
                this,
                getString(R.string.manage_children_deleted_success, getChildDisplayName(item)),
                Toast.LENGTH_SHORT
        ).show();

        if (item.id.equals(editingChildId)) {
            resetForm();
        }
        loadChildren();
    }

    // מחזיר הפניה לנתיב הילדים: /parents/{uid}/children
    private DatabaseReference childrenRef() {
        return database.child("parents").child(parentUid).child("children");
    }

    // מחזיר שם מלא של ילד או ברירת מחדל
    private String getChildDisplayName(ChildItem item) {
        return NameUtils.fullNameOrDefault(
                item.firstName,
                item.lastName,
                getString(R.string.default_child_name)
        );
    }

    // מחשב גובה לרשימה ידנית כדי שלא יהיה גוש ריק בתוך המסך הנגלל
    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int listWidth;
        if (listView.getWidth() > 0) {
            listWidth = listView.getWidth();
        } else {
            listWidth = getResources().getDisplayMetrics().widthPixels - 100;
        }

        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);

        for (int index = 0; index < adapter.getCount(); index++) {
            View item = adapter.getView(index, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    /**
     * מתאם פנימי שמציג ילד אחד בכל שורה.
     */
    private class ChildListAdapter extends ArrayAdapter<ChildItem> {

        // מחבר את המתאם לרשימת הילדים של המסך
        ChildListAdapter() {
            super(ManageChildrenActivity.this, 0, childItems);
        }

        // מציג שם ותמונה עבור ילד אחד ברשימה
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

            bindChildRow(convertView, item);
            return convertView;
        }

        // מחבר את נתוני הילד לשורה של הרשימה
        private void bindChildRow(View rowView, ChildItem item) {
            TextView tvChildFullName = rowView.findViewById(R.id.tvChildFullName);
            tvChildFullName.setText(getChildDisplayName(item));

            ImageView imageView = rowView.findViewById(R.id.ivChildThumb);
            if (item.profileImageBase64 != null) {
                Bitmap bitmap = ImageHelper.base64ToBitmap(item.profileImageBase64);
                if (bitmap != null) {
                    imageView.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
                    return;
                }
            }

            imageView.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    /**
     * פריט פשוט שמייצג ילד ברשימת ניהול הילדים.
     */
    private static class ChildItem {
        String id;
        String firstName;
        String lastName;
        String profileImageBase64;

        // שומר את הערכים שהגיעו מ-Firebase עבור ילד אחד
        ChildItem(String id, String firstName, String lastName, String profileImageBase64) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.profileImageBase64 = profileImageBase64;
        }
    }
}
