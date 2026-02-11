package com.example.family_tasks_proj.child;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class ChildDashboardActivity extends AppCompatActivity {

    private TextView tvChildName, tvStars, tvTotalTasks, tvCompleted, tvDueSoon;
    private RecyclerView rvTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        rvTasks = findViewById(R.id.rvTasks);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        loadChildData();
        loadTasks();
    }

    private void loadChildData() {
        String childUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("users")
                .child(childUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String name = snapshot.child("firstName").getValue(String.class);
                    Long stars = snapshot.child("stars").child("total").getValue(Long.class);

                    tvChildName.setText("שלום " + name + " 👋");
                    tvStars.setText("⭐ כוכבים: " + (stars != null ? stars : 0));
                });
    }

    private void loadTasks() {
        // כאן בהמשך תוסיף Adapter למשימות של הילד בלבד
        // tasks where assignedTo == childUID
    }
}
