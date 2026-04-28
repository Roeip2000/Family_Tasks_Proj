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

/** מסך ניהול הילדים על ידי ההורה. מאפשר הוספה, עריכה ומחיקה של ילדים. */
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

    // פתיחת הגלריה לבחירת תמונת פרופיל לילד
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        bindViews();

        parentUid = FirebaseAuth.getInstance().getUid();
        if (parentUid == null) {
            Toast.makeText(this, "לא מחובר למערכת", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance().getReference();
        setupChildrenList();
        bindActions();
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
    }

    // הגדרת רשימת הילדים והפעולות עליה
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

    // מטפל בתמונה שנבחרה מהגלריה
    private void handleChildImageResult(Uri uri) {
        if (uri == null) return;

        selectedChildPhoto = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
        if (selectedChildPhoto == null) {
            Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
            return;
        }
        imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(selectedChildPhoto));
    }

    // שומר ילד חדש או מעדכן ילד קיים ב-Firebase
    private void saveChild() {
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        final String childId = (editingChildId != null) ? editingChildId : childrenRef().push().getKey();
        if (childId == null) {
            Toast.makeText(this, "שגיאה ביצירת מזהה ילד", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageBase64 = (selectedChildPhoto != null) ? ImageHelper.bitmapToBase64(selectedChildPhoto) : editingChildOldImageBase64;
        btnAddChild.setEnabled(false);

        Task<Void> saveTask;
        if (editingChildId == null) {
            saveTask = childrenRef().child(childId).setValue(new Child(firstName, lastName, imageBase64));
        } else {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", firstName);
            updates.put("lastName", lastName);
            if (imageBase64 != null) updates.put("profileImageBase64", imageBase64);
            saveTask = childrenRef().child(childId).updateChildren(updates);
        }

        saveTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                btnAddChild.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, "הילד נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                resetForm();
                loadChildren();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                btnAddChild.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, "שגיאה: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // טוען את רשימת הילדים מ-Firebase
    private void loadChildren() {
        childrenRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleChildrenSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // מעדכן את הרשימה המקומית מהנתונים שהגיעו מהשרת
    private void handleChildrenSnapshot(DataSnapshot snapshot) {
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
        updateChildrenEmptyState();
    }

    // מציג הודעה אם אין ילדים רשומים
    private void updateChildrenEmptyState() {
        boolean isEmpty = childItems.isEmpty();
        tvNoChildren.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        lvChildren.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ממלא את הטופס בפרטי הילד לצורך עריכה
    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;
        etFirstName.setText(item.firstName);
        etLastName.setText(item.lastName);
        showChildPhoto(item.profileImageBase64);
        toggleUI(true);
    }

    // מציג את תמונת הילד בעיגול
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

    // מאפס את הטופס למצב הוספה
    private void resetForm() {
        editingChildId = null;
        editingChildOldImageBase64 = null;
        selectedChildPhoto = null;
        etFirstName.setText("");
        etLastName.setText("");
        imgChildPhoto.setImageResource(R.drawable.ic_avatar_placeholder);
        toggleUI(false);
    }

    // משנה את עיצוב הכפתורים בין מצב עריכה להוספה
    private void toggleUI(boolean isEdit) {
        btnAddChild.setText(isEdit ? "שמור שינויים" : "הוסף ילד");
        int colorRes = isEdit ? R.color.primary : R.color.accent;
        btnAddChild.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
        btnCancelEdit.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        tvFormTitle.setText(isEdit ? "עריכת פרטי ילד" : "הוספת ילד חדש");
    }

    // מציג תפריט אפשרויות לילד שנבחר
    private void showChildOptionsDialog(final int position) {
        if (position < 0 || position >= childItems.size()) return;
        String[] options = {"ערוך", "מחק"};
        new AlertDialog.Builder(this)
                .setTitle("בחר פעולה")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) enterEditMode(position);
                        else showDeleteChildDialog(position);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מציג דיאלוג לאישור מחיקת ילד
    private void showDeleteChildDialog(int position) {
        final ChildItem item = childItems.get(position);
        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד")
                .setMessage("האם אתה בטוח שברצונך למחוק את הילד?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteChild(item);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מוחק את הילד מ-Firebase
    private void deleteChild(final ChildItem item) {
        childrenRef().child(item.id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, "הילד נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                if (item.id.equals(editingChildId)) resetForm();
                loadChildren();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ManageChildrenActivity.this, "שגיאה: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private DatabaseReference childrenRef() {
        return database.child("parents").child(parentUid).child("children");
    }

    // מעדכן את גובה הרשימה באופן ידני כדי שתעבוד בתוך גלילה
    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listView.getWidth() > 0 ? listView.getWidth() : getResources().getDisplayMetrics().widthPixels - 100, View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    // מתאם להצגת רשימת הילדים
    private class ChildListAdapter extends ArrayAdapter<ChildItem> {
        ChildListAdapter() { super(ManageChildrenActivity.this, 0, childItems); }
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_manage_child, parent, false);
            ChildItem item = getItem(position);
            if (item != null) bindChildRow(convertView, item);
            return convertView;
        }
        private void bindChildRow(View rowView, ChildItem item) {
            TextView tvChildFullName = rowView.findViewById(R.id.tvChildFullName);
            tvChildFullName.setText(NameUtils.fullNameOrDefault(item.firstName, item.lastName, "ילד"));
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

    private static class ChildItem {
        String id, firstName, lastName, profileImageBase64;
        ChildItem(String id, String firstName, String lastName, String profileImageBase64) {
            this.id = id; this.firstName = firstName; this.lastName = lastName; this.profileImageBase64 = profileImageBase64;
        }
    }
}
