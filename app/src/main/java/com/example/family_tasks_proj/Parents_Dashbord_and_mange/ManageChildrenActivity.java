package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

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
            @Override public void onClick(View view) { saveChildData(); }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { clearForm(); }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveChildData() {
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = (editChildId != null) ? editChildId : getChildrenReference().push().getKey();
        if (childId == null) return;

        btnAdd.setEnabled(false);
        // שמירה ישירה ללא HashMap
        DatabaseReference currentChildRef = getChildrenReference().child(childId);
        currentChildRef.child("firstName").setValue(firstName);
        currentChildRef.child("lastName").setValue(lastName).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                btnAdd.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, "הנתונים נשמרו", Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    private void delete(String childId) {
        getChildrenReference().child(childId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override public void onSuccess(Void unused) { Toast.makeText(ManageChildrenActivity.this, "הילד נמחק", Toast.LENGTH_SHORT).show(); }
        });
    }

    private void clearForm() {
        editChildId = null;
        etFirstName.setText("");
        etLastName.setText("");
        btnAdd.setText("הוסף ילד");
        btnCancel.setVisibility(View.GONE);
        tvTitle.setText("הוספת ילד");
    }

    private void updateUI() {
        listAdapter.notifyDataSetChanged();
        updateListViewHeight();
        tvEmpty.setVisibility(childList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openOptions(int position) {
        final ChildItem item = childList.get(position);
        String[] options = {"עריכה", "מחיקה"};
        new AlertDialog.Builder(this).setTitle(item.firstName).setItems(options, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    editChildId = item.id;
                    etFirstName.setText(item.firstName);
                    etLastName.setText(item.lastName);
                    btnAdd.setText("עדכן");
                    btnCancel.setVisibility(View.VISIBLE);
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
        ChildListAdapter() { super(ManageChildrenActivity.this, 0, childList); }
        @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_manage_child, parent, false);
            ChildItem item = getItem(position);
            if (item != null) ((TextView) convertView.findViewById(R.id.tvChildFullName)).setText(item.firstName + " " + item.lastName);
            return convertView;
        }
    }

    private static class ChildItem {
        String id, firstName, lastName;
        ChildItem(String id, String f, String l) { this.id = id; this.firstName = f; this.lastName = l; }
    }

    private void updateListViewHeight() {
        ListAdapter adapter = lvChildren.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(lvChildren.getWidth() > 0 ? lvChildren.getWidth() : 500, View.MeasureSpec.AT_MOST);
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
