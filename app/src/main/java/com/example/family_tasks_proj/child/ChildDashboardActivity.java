package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.child.model.ChildTask;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/** דשבורד לילד בעיצוב נקי ופשוט. */
public class ChildDashboardActivity extends AppCompatActivity {

    private TextView tvChildName, tvStars, tvTotalTasks, tvCompleted, tvDueSoon, tvOverdue, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;
    private Button btnLogout;
    private View filterOpen, filterUrgent, filterOverdue, filterCompleted;

    private final List<ChildTask> allTasks = new ArrayList<>();
    private final List<ChildTask> visibleTasks = new ArrayList<>();
    private ChildTaskAdapter adapter;
    private String parentId, childId;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    private enum FilterMode {
        NOT_COMPLETED, COMPLETED, URGENT, OVERDUE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);
        resolveSession();
        if (parentId == null || childId == null) {
            finish();
            return;
        }
        bindViews();
        setupTaskList();
        setupInteractiveFilters();
        loadChildHeader();
        loadTasks();
    }

    private void bindViews() {
        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);
        btnLogout = findViewById(R.id.btnLogout);

        filterOpen = findViewById(R.id.filterNotCompleted);
        filterUrgent = findViewById(R.id.filterUrgent);
        filterOverdue = findViewById(R.id.filterOverdue);
        filterCompleted = findViewById(R.id.filterCompleted);

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupInteractiveFilters() {
        View.OnClickListener click = v -> {
            if (v == filterOpen) setActiveFilter(FilterMode.NOT_COMPLETED);
            else if (v == filterUrgent) setActiveFilter(FilterMode.URGENT);
            else if (v == filterOverdue) setActiveFilter(FilterMode.OVERDUE);
            else if (v == filterCompleted) setActiveFilter(FilterMode.COMPLETED);
        };
        filterOpen.setOnClickListener(click);
        filterUrgent.setOnClickListener(click);
        filterOverdue.setOnClickListener(click);
        filterCompleted.setOnClickListener(click);
    }

    private void setActiveFilter(FilterMode mode) {
        activeFilter = mode;
        updateFilterUi();
        buildVisibleTasks();
    }

    private void updateFilterUi() {
        filterOpen.setBackgroundColor(activeFilter == FilterMode.NOT_COMPLETED ? 0x1A000000 : Color.TRANSPARENT);
        filterUrgent.setBackgroundColor(activeFilter == FilterMode.URGENT ? 0x1A000000 : Color.TRANSPARENT);
        filterOverdue.setBackgroundColor(activeFilter == FilterMode.OVERDUE ? 0x1A000000 : Color.TRANSPARENT);
        filterCompleted.setBackgroundColor(activeFilter == FilterMode.COMPLETED ? 0x1A000000 : Color.TRANSPARENT);

        switch (activeFilter) {
            case NOT_COMPLETED: tvTaskSectionTitle.setText("משימות פתוחות"); break;
            case URGENT: tvTaskSectionTitle.setText("משימות דחופות"); break;
            case OVERDUE: tvTaskSectionTitle.setText("משימות באיחור"); break;
            case COMPLETED: tvTaskSectionTitle.setText("משימות שסיימתי"); break;
        }
    }

    private void resolveSession() {
        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        if (parentId == null || childId == null) {
            SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
            parentId = sp.getString("parentId", null);
            childId = sp.getString("childId", null);
        }
    }

    private void loadChildHeader() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                tvChildName.setText("היי " + firstName + "!");
                Long stars = snapshot.child("stars").getValue(Long.class);
                tvStars.setText("⭐ " + (stars != null ? stars : 0));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTasks() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int open = 0, done = 0, urgent = 0, overdue = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChildTask task = snap.getValue(ChildTask.class);
                    if (task == null) continue;
                    task.setTaskId(snap.getKey());
                    allTasks.add(task);
                    if (task.getIsDone()) done++;
                    else {
                        open++;
                        if (DateUtils.isOverdue(task.getDueAt())) overdue++;
                        else if (DateUtils.isDueSoon(task.getDueAt())) urgent++;
                    }
                }
                tvTotalTasks.setText(String.valueOf(open));
                tvCompleted.setText(String.valueOf(done));
                tvDueSoon.setText(String.valueOf(urgent));
                tvOverdue.setText(String.valueOf(overdue));
                buildVisibleTasks();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void buildVisibleTasks() {
        visibleTasks.clear();
        for (ChildTask task : allTasks) {
            boolean match = false;
            switch (activeFilter) {
                case NOT_COMPLETED: match = !task.getIsDone(); break;
                case COMPLETED: match = task.getIsDone(); break;
                case URGENT: match = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt()); break;
                case OVERDUE: match = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt()); break;
            }
            if (match) visibleTasks.add(task);
        }
        tvNoTasks.setVisibility(visibleTasks.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void setupTaskList() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(visibleTasks, task -> markTaskDone(task));
        rvTasks.setAdapter(adapter);
    }

    private void markTaskDone(ChildTask task) {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks")
                .child(task.getTaskId()).child("isDone").setValue(true).addOnSuccessListener(v -> {
                    Toast.makeText(this, "כל הכבוד! סיימת את המשימה", Toast.LENGTH_SHORT).show();
                    awardStar();
                });
    }

    private void awardStar() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("stars")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long current = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                snapshot.getRef().setValue(current + 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("יציאה").setMessage("בטוח שרוצה לצאת?")
                .setPositiveButton("כן", (d, w) -> {
                    getSharedPreferences("child_session", MODE_PRIVATE).edit().clear().apply();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }).setNegativeButton("לא", null).show();
    }
}
