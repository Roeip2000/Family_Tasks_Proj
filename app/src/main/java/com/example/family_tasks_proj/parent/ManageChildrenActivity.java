package com.example.family_tasks_proj.parent;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName;
    private Button btnAdd, btnBack;
    private ListView lvChildren;
    private TextView tvTitle;

    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private String parentUserId;
    private String editChildId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        parentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupEvents();
        setupList();
        loadFromFirebase();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        btnAdd = findViewById(R.id.btnAddChild);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);
        tvTitle = findViewById(R.id.tvFormTitle);
    }

    private void setupEvents() {
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChildData();
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
        listAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_manage_child,
                R.id.tvChildFullName,
                childNames
        );
        lvChildren.setAdapter(listAdapter);
        lvChildren.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startEditChild(position);
            }
        });
    }

    // טעינת הילדים של ההורה מ-Firebase
    private void loadFromFirebase() {
        getChildrenReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                childNames.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String id = childSnapshot.getKey();
                    String first = childSnapshot.child("firstName").getValue(String.class);

                    childIds.add(id);
                    childNames.add(first);
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Firebase מחייב מימוש, גם אם ריק
            }
        });
    }

    private void saveChildData() {
        String firstName = etFirstName.getText().toString().trim();

        if (firstName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId;
        if (editChildId != null)
        {
            childId = editChildId;
        } else
        {
            // מזהה חדש לילד חדש
            childId = getChildrenReference().push().getKey();
        }
        DatabaseReference childNode = getChildrenReference().child(childId);

        childNode.child("firstName").setValue(firstName).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_saved, Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    private void clearForm() {
        editChildId = null;
        etFirstName.setText("");
        btnAdd.setText(R.string.btn_add_child);
        tvTitle.setText(R.string.title_add_child);
    }

    private void updateUI() {
        listAdapter.notifyDataSetChanged();
        fitListHeight(lvChildren);
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

    // לחיצה על ילד ברשימה טוענת את פרטיו לטופס לצורך עריכה
    private void startEditChild(int position) {
        editChildId = childIds.get(position);
        etFirstName.setText(childNames.get(position));
        btnAdd.setText(R.string.btn_update);
        tvTitle.setText(R.string.title_edit_child);
    }

    private DatabaseReference getChildrenReference() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentUserId).child("children");
    }
}
