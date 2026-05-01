package com.example.family_tasks_proj.parent;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.utils.NameUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// מסך לניהול הילדים במשפחה
public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private Button btnAdd, btnCancel, btnBack;
    private ListView lvChildren;
    private TextView tvEmpty, tvTitle;

    private final List<ChildItem> childList = new ArrayList<>();
    private ChildListAdapter listAdapter;
    private String parentUserId;
    private String editChildId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        parentUserId = FirebaseAuth.getInstance().getUid();
        if (parentUserId == null) {
            finish();
            return;
        }

        initViews();
        setupEvents();
        setupList();
        loadFromFirebase();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnAdd = findViewById(R.id.btnAddChild);
        btnCancel = findViewById(R.id.btnCancelEdit);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);
        tvEmpty = findViewById(R.id.tvNoChildren);
        tvTitle = findViewById(R.id.tvFormTitle);
    }

    private void setupEvents() {
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChildData();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearForm();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupList() {
        listAdapter = new ChildListAdapter();
        lvChildren.setAdapter(listAdapter);
        lvChildren.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptions(position);
            }
        });
    }

    private void loadFromFirebase() {
        // מציגים את רשימת הילדים מהנתיב parents/{parentId}/children.
        // כל שינוי ב-Firebase מעדכן את הרשימה במסך דרך ValueEventListener.
        getChildrenReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childList.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String id = childSnapshot.getKey();
                    String first = childSnapshot.child("firstName").getValue(String.class);
                    String last = childSnapshot.child("lastName").getValue(String.class);
                    childList.add(new ChildItem(id, first, last));
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void saveChildData() {
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();

        // לא שומרים ילד בלי שם מלא, כדי שלא יופיעו רשומות ריקות בדשבורדים.
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId;
        if (editChildId != null) {
            childId = editChildId;
        } else {
            // push יוצר id ייחודי לילד חדש תחת ההורה המחובר.
            childId = getChildrenReference().push().getKey();
        }
        if (childId == null) {
            return;
        }

        btnAdd.setEnabled(false);
        // שמירה של שני שדות פשוטים: שם פרטי ושם משפחה. אין כאן תמונת ילד.
        DatabaseReference currentChildRef = getChildrenReference().child(childId);
        currentChildRef.child("firstName").setValue(firstName);
        currentChildRef.child("lastName").setValue(lastName).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                btnAdd.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_saved, Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    private void delete(String childId) {
        getChildrenReference().child(childId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_deleted, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearForm() {
        editChildId = null;
        etFirstName.setText("");
        etLastName.setText("");
        btnAdd.setText(R.string.btn_add_child);
        btnCancel.setVisibility(View.GONE);
        tvTitle.setText(R.string.title_add_child);
    }

    private void updateUI() {
        listAdapter.notifyDataSetChanged();
        updateListViewHeight();
        if (childList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void openOptions(int position) {
        final ChildItem item = childList.get(position);
        String[] options = {
                getString(R.string.dialog_option_edit),
                getString(R.string.dialog_option_delete)
        };
        new AlertDialog.Builder(this).setTitle(item.firstName).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // מצב עריכה משתמש באותו טופס של הוספה, רק עם id קיים.
                    editChildId = item.id;
                    etFirstName.setText(item.firstName);
                    etLastName.setText(item.lastName);
                    btnAdd.setText(R.string.btn_update);
                    btnCancel.setVisibility(View.VISIBLE);
                    tvTitle.setText(R.string.title_edit_child);
                } else {
                    delete(item.id);
                }
            }
        }).show();
    }

    private DatabaseReference getChildrenReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentUserId).child("children");
    }

    private class ChildListAdapter extends ArrayAdapter<ChildItem> {
        ChildListAdapter() {
            super(ManageChildrenActivity.this, 0, childList);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_manage_child, parent, false);
            }
            ChildItem item = getItem(position);
            if (item != null) {
                // הרשימה מציגה רק שם מלא וטקסט עזר, בלי תמונה ליד הילד.
                TextView tvChildFullName = convertView.findViewById(R.id.tvChildFullName);
                tvChildFullName.setText(NameUtils.fullNameOrDefault(
                        item.firstName,
                        item.lastName,
                        getString(R.string.default_child_name_fallback)));
            }
            return convertView;
        }
    }

    private static class ChildItem {
        String id, firstName, lastName;

        ChildItem(String id, String f, String l) {
            this.id = id;
            this.firstName = f;
            this.lastName = l;
        }
    }

    private void updateListViewHeight() {
        ListAdapter adapter = lvChildren.getAdapter();
        if (adapter == null) {
            return;
        }
        int totalHeight = 0;
        int listWidth;
        if (lvChildren.getWidth() > 0) {
            listWidth = lvChildren.getWidth();
        } else {
            listWidth = 500;
        }
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);
        for (int index = 0; index < adapter.getCount(); index++) {
            View itemView = adapter.getView(index, null, lvChildren);
            itemView.measure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += itemView.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = lvChildren.getLayoutParams();
        params.height = totalHeight + (lvChildren.getDividerHeight() * (adapter.getCount() - 1));
        lvChildren.setLayoutParams(params);
    }
}
