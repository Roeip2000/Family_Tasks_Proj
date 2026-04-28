package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/** מסך המשימות של הילד. מציג את המשימות שהוקצו לו ומאפשר לסמנן כבוצעו. */
public class ChildDashboardActivity extends AppCompatActivity {

    private TextView tvChildName, tvStars, tvTotalTasks, tvCompleted, tvDueSoon, tvOverdue, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;
    private Button btnLogout;
    private LinearLayout filterNotCompleted, filterCompleted, filterUrgent, filterOverdue;
    private ImageView imgChildAvatar;

    private final List<ChildTask> allTasks = new ArrayList<>();
    private final List<ChildTask> visibleTasks = new ArrayList<>();
    private String parentId, childId;
    private ChildTaskAdapter adapter;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    private enum FilterMode { NOT_COMPLETED, COMPLETED, URGENT, OVERDUE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        bindViews();
        setupTaskList();
        setupFilters();
        bindActions();

        resolveSession();
        if (parentId == null || childId == null) {
            Toast.makeText(this, R.string.child_missing_session, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        updateFilterSelectionUi();
        updateFilterLabels();
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
        imgChildAvatar = findViewById(R.id.imgChildAvatar);
        filterNotCompleted = findViewById(R.id.filterNotCompleted);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterUrgent = findViewById(R.id.filterUrgent);
        filterOverdue = findViewById(R.id.filterOverdue);
    }

    private void setupTaskList() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(visibleTasks, new ChildTaskAdapter.OnTaskDoneListener() {
            @Override public void onTaskDone(ChildTask task) { markTaskDone(task); }
        });
        rvTasks.setAdapter(adapter);
    }

    private void setupFilters() {
        filterNotCompleted.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.NOT_COMPLETED); } });
        filterCompleted.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.COMPLETED); } });
        filterUrgent.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.URGENT); } });
        filterOverdue.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.OVERDUE); } });
    }

    private void bindActions() {
        btnLogout.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { showLogoutDialog(); } });
    }

    // משחזר את המזהים של ההורה והילד מה-Intent או מהזיכרון המקומי
    private void resolveSession() {
        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        if (parentId == null || childId == null) {
            SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
            parentId = sp.getString("parentId", null);
            childId = sp.getString("childId", null);
        }
    }

    // טוען את שם הילד ותמונתו מהשרת
    private void loadChildHeader() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fName = snapshot.child("firstName").getValue(String.class);
                String lName = snapshot.child("lastName").getValue(String.class);
                tvChildName.setText(getString(R.string.child_hello_with_name, NameUtils.fullNameOrDefault(fName, lName, getString(R.string.default_child_name))));
                String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                if (base64 != null) {
                    android.graphics.Bitmap bmp = ImageHelper.base64ToBitmap(base64);
                    if (bmp != null) imgChildAvatar.setImageBitmap(ImageHelper.getCircularBitmap(bmp));
                    else imgChildAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                } else imgChildAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // טוען את כל המשימות של הילד מ-Firebase
    private void loadTasks() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int open = 0, done = 0, urgent = 0, late = 0; long stars = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChildTask task = snap.getValue(ChildTask.class);
                    if (task == null) continue;
                    if (task.getId() == null) task.setId(snap.getKey());
                    allTasks.add(task);
                    if (task.getIsDone()) { done++; stars += task.getStarsWorth(); }
                    else {
                        open++;
                        if (DateUtils.isOverdue(task.getDueAt())) late++;
                        else if (DateUtils.isDueSoon(task.getDueAt())) urgent++;
                    }
                }
                tvTotalTasks.setText(String.valueOf(open));
                tvCompleted.setText(String.valueOf(done));
                tvDueSoon.setText(String.valueOf(urgent));
                tvOverdue.setText(String.valueOf(late));
                tvStars.setText(getString(R.string.child_stars_count, (int)stars));
                applyFilter();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, getString(R.string.child_error_loading_tasks, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מסמן משימה כבוצעה לאחר אישור הילד
    private void markTaskDone(final ChildTask task) {
        new AlertDialog.Builder(this).setTitle(R.string.child_mark_task_title).setMessage(getString(R.string.child_mark_task_message, task.getTitle()))
                .setPositiveButton(R.string.child_mark_task_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks").child(task.getId()).child("isDone").setValue(true)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override public void onSuccess(Void unused) {
                                        Toast.makeText(ChildDashboardActivity.this, R.string.child_mark_task_success, Toast.LENGTH_SHORT).show();
                                        loadTasks();
                                    }
                                });
                    }
                }).setNegativeButton(R.string.child_mark_task_later, null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.child_logout_title).setMessage(R.string.child_logout_message)
                .setPositiveButton(R.string.child_logout_confirm, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        getSharedPreferences("child_session", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(ChildDashboardActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                    }
                }).setNegativeButton(R.string.action_cancel, null).show();
    }

    private void setActiveFilter(FilterMode mode) {
        activeFilter = mode;
        updateFilterSelectionUi();
        updateFilterLabels();
        applyFilter();
    }

    private void applyFilter() {
        visibleTasks.clear();
        for (ChildTask t : allTasks) {
            boolean match = false;
            switch (activeFilter) {
                case COMPLETED: match = t.getIsDone(); break;
                case URGENT: match = !t.getIsDone() && !DateUtils.isOverdue(t.getDueAt()) && DateUtils.isDueSoon(t.getDueAt()); break;
                case OVERDUE: match = !t.getIsDone() && DateUtils.isOverdue(t.getDueAt()); break;
                default: match = !t.getIsDone(); break;
            }
            if (match) visibleTasks.add(t);
        }
        tvNoTasks.setVisibility(visibleTasks.isEmpty() ? View.VISIBLE : View.GONE);
        rvTasks.setVisibility(visibleTasks.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void updateFilterLabels() {
        switch (activeFilter) {
            case COMPLETED: tvTaskSectionTitle.setText(R.string.child_task_section_completed); tvNoTasks.setText(R.string.child_no_tasks_completed); break;
            case URGENT: tvTaskSectionTitle.setText(R.string.child_task_section_urgent); tvNoTasks.setText(R.string.child_no_tasks_urgent); break;
            case OVERDUE: tvTaskSectionTitle.setText(R.string.child_dashboard_filter_overdue); tvNoTasks.setText(R.string.child_no_tasks_overdue); break;
            default: tvTaskSectionTitle.setText(R.string.child_task_section_open); tvNoTasks.setText(R.string.child_no_tasks_open); break;
        }
    }

    private void updateFilterSelectionUi() {
        updateBlock(filterUrgent, activeFilter == FilterMode.URGENT, R.color.surface_soft_orange);
        updateBlock(filterOverdue, activeFilter == FilterMode.OVERDUE, R.color.surface_soft_rose);
        updateBlock(filterCompleted, activeFilter == FilterMode.COMPLETED, R.color.surface_soft_green);
        updateBlock(filterNotCompleted, activeFilter == FilterMode.NOT_COMPLETED, R.color.surface_soft_blue);
    }

    private void updateBlock(LinearLayout l, boolean sel, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(24f); gd.setColor(sel ? getColor(color) : android.graphics.Color.TRANSPARENT);
        if (sel) gd.setStroke(2, getColor(R.color.primary));
        l.setBackground(gd);
        for (int i = 0; i < l.getChildCount(); i++) {
            View v = l.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTextColor(sel ? getColor(R.color.text_primary) : getColor(R.color.text_secondary));
        }
    }
}
