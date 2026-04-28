package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskListItem;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.util.DateUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// דשבורד ניהול להורה - מציג את כל המשימות של כל הילדים
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR, btnLogout;
    private TextView tvName, tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvFilterTitle;
    private View fOpen, fUrgent, fOverdue, fDone;
    private ListView lvTasks;

    private final List<AssignedTask> allTasks = new ArrayList<>();
    private final List<TaskListItem> visibleItems = new ArrayList<>();

    private ParentDashboardTaskAdapter taskAdapter;
    private FilterMode activeFilter = FilterMode.ALL;

    private enum FilterMode { ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);
        initViews();
        setupActions();
        setupLists();
        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadProfile(user);
            loadData(user);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initViews() {
        tvName = findViewById(R.id.tvParentName);
        tvTotal = findViewById(R.id.tvParentTotalTasks);
        tvDone = findViewById(R.id.tvParentCompleted);
        tvUrgent = findViewById(R.id.tvParentDueSoon);
        tvOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvFilterTitle = findViewById(R.id.tvTaskSectionTitle);
        lvTasks = findViewById(R.id.lvTasks);

        fOpen = findViewById(R.id.containerFilterOpen);
        fUrgent = findViewById(R.id.containerFilterUrgent);
        fOverdue = findViewById(R.id.containerFilterOverdue);
        fDone = findViewById(R.id.containerFilterCompleted);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupFilters() {
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == fOpen) setFilter(FilterMode.ASSIGNED);
                else if (v == fUrgent) setFilter(FilterMode.URGENT);
                else if (v == fOverdue) setFilter(FilterMode.OVERDUE);
                else if (v == fDone) setFilter(FilterMode.COMPLETED);
            }
        };
        fOpen.setOnClickListener(click);
        fUrgent.setOnClickListener(click);
        fOverdue.setOnClickListener(click);
        fDone.setOnClickListener(click);
    }

    private void setFilter(FilterMode mode) {
        if (activeFilter == mode) activeFilter = FilterMode.ALL;
        else activeFilter = mode;
        updateFilterUI();
        refreshTaskList();
    }

    private void updateFilterUI() {
        // צובע את הפילטר הפעיל ברקע אפור שקוף ומאפס את השאר
        if (activeFilter == FilterMode.ASSIGNED) {
            fOpen.setBackgroundColor(0x1A000000);
        } else {
            fOpen.setBackgroundColor(Color.TRANSPARENT);
        }
        if (activeFilter == FilterMode.URGENT) {
            fUrgent.setBackgroundColor(0x1A000000);
        } else {
            fUrgent.setBackgroundColor(Color.TRANSPARENT);
        }
        if (activeFilter == FilterMode.OVERDUE) {
            fOverdue.setBackgroundColor(0x1A000000);
        } else {
            fOverdue.setBackgroundColor(Color.TRANSPARENT);
        }
        if (activeFilter == FilterMode.COMPLETED) {
            fDone.setBackgroundColor(0x1A000000);
        } else {
            fDone.setBackgroundColor(Color.TRANSPARENT);
        }

        switch (activeFilter) {
            case ASSIGNED: tvFilterTitle.setText("משימות פתוחות"); break;
            case URGENT: tvFilterTitle.setText("משימות דחופות"); break;
            case OVERDUE: tvFilterTitle.setText("משימות באיחור"); break;
            case COMPLETED: tvFilterTitle.setText("משימות שהושלמו"); break;
            default: tvFilterTitle.setText("כל המשימות"); break;
        }
    }

    private void setupActions() {
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class)); }
        });
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class)); }
        });
        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class)); }
        });
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class)); }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showLogoutDialog(); }
        });
    }

    private void setupLists() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleItems);
        taskAdapter.setShowChildName(true);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                showTaskOptions(pos);
            }
        });
    }

    private void loadProfile(FirebaseUser user) {
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("firstName").getValue(String.class);
                String displayName;
                if (name != null) {
                    displayName = name;
                } else {
                    displayName = "הורה";
                }
                tvName.setText("שלום " + displayName + "! 👋");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadData(FirebaseUser user) {
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int totalCount = 0, doneCount = 0, urgentCount = 0, lateCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot cSnap : snapshot.getChildren()) {
                        String cId = cSnap.getKey();
                        String cName = cSnap.child("firstName").getValue(String.class);
                        
                        for (DataSnapshot tSnap : cSnap.child("tasks").getChildren()) {
                            AssignedTask task = new AssignedTask();
                            task.setChildId(cId);
                            if (cName != null) {
                                task.setChildName(cName);
                            } else {
                                task.setChildName("ילד");
                            }
                            task.setTaskId(tSnap.getKey());
                            task.setTitle(tSnap.child("title").getValue(String.class));
                            task.setDueAt(tSnap.child("dueAt").getValue(String.class));
                            task.setImageBase64(tSnap.child("imageBase64").getValue(String.class));
                            Boolean isDone = tSnap.child("isDone").getValue(Boolean.class);
                            task.setIsDone(isDone != null && isDone);

                            allTasks.add(task);
                            if (task.getIsDone()) doneCount++;
                            else {
                                totalCount++;
                                if (DateUtils.isOverdue(task.getDueAt())) lateCount++;
                                else if (DateUtils.isDueSoon(task.getDueAt())) urgentCount++;
                            }
                        }
                    }
                }
                
                tvTotal.setText(String.valueOf(totalCount));
                tvDone.setText(String.valueOf(doneCount));
                tvUrgent.setText(String.valueOf(urgentCount));
                tvOverdue.setText(String.valueOf(lateCount));

                refreshTaskList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshTaskList() {
        visibleItems.clear();
        for (AssignedTask task : allTasks) {
            boolean match = false;
            switch (activeFilter) {
                case ASSIGNED: match = !task.getIsDone(); break;
                case COMPLETED: match = task.getIsDone(); break;
                case URGENT: match = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt()); break;
                case OVERDUE: match = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt()); break;
                case ALL: default: match = true; break;
            }
            if (match) visibleItems.add(TaskListItem.createTask(task));
        }
        if (visibleItems.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
        }
        taskAdapter.notifyDataSetChanged();
        updateHeight();
    }

    private void showTaskOptions(int pos) {
        TaskListItem item = visibleItems.get(pos);
        if (item.getIsHeader()) return;
        final AssignedTask task = item.getTask();
        new AlertDialog.Builder(this).setTitle(task.getTitle())
                .setMessage("לילד: " + task.getChildName() + "\nיעד: " + task.getDueAt())
                .setNeutralButton("מחק משימה", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { deleteTask(task); }
                }).setNegativeButton("סגור", null).show();
    }

    private void deleteTask(AssignedTask task) {
        FirebaseDatabase.getInstance().getReference("parents").child(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.getChildId()).child("tasks").child(task.getTaskId()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override public void onSuccess(Void unused) { Toast.makeText(ParentDashboardActivity.this, "נמחק", Toast.LENGTH_SHORT).show(); }
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("בטוח שרוצה לצאת?")
                .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(ParentDashboardActivity.this, MainActivity.class));
                        finish();
                    }
                }).setNegativeButton("ביטול", null).show();
    }

    private void updateHeight() {
        ListAdapter adp = lvTasks.getAdapter();
        if (adp == null) return;
        int h = 0;
        int spec = View.MeasureSpec.makeMeasureSpec(lvTasks.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adp.getCount(); i++) {
            View v = adp.getView(i, null, lvTasks);
            v.measure(spec, View.MeasureSpec.UNSPECIFIED);
            h += v.getMeasuredHeight();
        }
        ViewGroup.LayoutParams p = lvTasks.getLayoutParams();
        p.height = h + (lvTasks.getDividerHeight() * (adp.getCount() - 1));
        lvTasks.setLayoutParams(p);
    }
}
