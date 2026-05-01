package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.example.family_tasks_proj.models.ChildTask;
import com.example.family_tasks_proj.child.adapter.ChildTaskAdapter;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * דשבורד הילד. מציג משימות וכוכבים.
 * הקוד מחולק למתודות ברורות כדי שיהיה קל להוסיף כפתורים או שינויי לוגיקה בלייב.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    // --- 1. משתני ממשק ---
    private TextView tvChildName, tvStars, tvTotalTasks, tvDueSoon, tvOverdue, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;
    private Button btnLogout;
    private View filterOpen, filterUrgent, filterOverdue;

    // --- 2. משתני נתונים ---
    private final List<ChildTask> allTasks = new ArrayList<>();
    private final List<ChildTask> visibleTasks = new ArrayList<>();
    private ChildTaskAdapter taskAdapter;
    private String parentId, childId;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    // הילד רואה רק משימות פתוחות, דחופות או באיחור. אין סינון "סיימתי".
    private enum FilterMode { NOT_COMPLETED, URGENT, OVERDUE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // שלב א: טעינת פרטי המשתמש מהסשן
        resolveSession();
        if (parentId == null || childId == null) {
            finish();
            return;
        }

        // שלב ב: אתחול מסך
        initViews();
        setupRecyclerView();
        setupListeners();

        // שלב ג: טעינת נתונים
        loadChildProfile();
        loadChildTasks();
    }

    private void initViews() {
        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvChildTasks);
        btnLogout = findViewById(R.id.btnChildLogout);

        filterOpen = findViewById(R.id.filterOpen);
        filterUrgent = findViewById(R.id.filterUrgent);
        filterOverdue = findViewById(R.id.filterOverdue);
    }

    private void setupRecyclerView() {
        // --- נושא במחוון: RecyclerView ---
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ChildTaskAdapter(visibleTasks, new ChildTaskAdapter.OnTaskDoneListener() {
            @Override
            public void onTaskDone(ChildTask task) {
                processMarkTaskAsDone(task); // לוגיקה לסיום משימה
            }
        });
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        // מאזין לכפתור התנתקות
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        // מאזינים לשלושת הפילטרים
        View.OnClickListener filterClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == filterOpen) {
                    activeFilter = FilterMode.NOT_COMPLETED;
                } else if (v == filterUrgent) {
                    activeFilter = FilterMode.URGENT;
                } else if (v == filterOverdue) {
                    activeFilter = FilterMode.OVERDUE;
                }
                updateFilterUI();
            }
        };
        filterOpen.setOnClickListener(filterClick);
        filterUrgent.setOnClickListener(filterClick);
        filterOverdue.setOnClickListener(filterClick);
    }

    // --- לוגיקה: סיום משימה ועדכון כוכבים ---
    private void processMarkTaskAsDone(ChildTask task) {
        // הגנה מפני לחיצה כפולה: אם המשימה כבר סומנה כבוצעה, לא מוסיפים כוכבים שוב.
        if (task.getIsDone()) {
            return;
        }
        // הופכים את הסטטוס בזיכרון מיד, כדי שלחיצה מהירה נוספת לא תיכנס לבלוק הזה.
        task.setIsDone(true);

        final long starsReward = task.getStarsWorth();

        // הילד משנה רק את הסטטוס של המשימה שלו ל-"בוצע".
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks")
                .child(task.getTaskId()).child("isDone").setValue(true)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ChildDashboardActivity.this, R.string.child_task_mark_done_success, Toast.LENGTH_SHORT).show();
                        // מוסיפים בדיוק את כמות הכוכבים שההורה הגדיר למשימה (מינימום 10 אם לא הוגדר).
                        long reward;
                        if (starsReward > 0) {
                            reward = starsReward;
                        } else {
                            reward = 10;
                        }
                        addStarToChild(reward);
                    }
                });
    }

    private void addStarToChild(final long rewardStars) {
        // משיכת כמות הכוכבים הנוכחית והוספת ערך המשימה.
        // הכוכבים נשמרים אצל הילד עצמו, ולכן אפשר להציג אותם גם בכניסה הבאה.
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("stars")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentStars = 0;
                if (snapshot.exists()) {
                    Long val = snapshot.getValue(Long.class);
                    if (val != null) {
                        currentStars = val;
                    }
                }
                snapshot.getRef().setValue(currentStars + rewardStars);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // --- לוגיקה: טעינת נתונים ---
    private void loadChildProfile() {
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                String name = snapshot.child("firstName").getValue(String.class);
                // אם אין שם ב-Firebase מציגים "ילד" כדי שלא ייראה ריק
                String displayName;
                if (name != null && !name.isEmpty()) {
                    displayName = name;
                } else {
                    displayName = getString(R.string.default_child_name_fallback);
                }
                tvChildName.setText(getString(R.string.child_greeting, displayName));
                Long stars = snapshot.child("stars").getValue(Long.class);
                long starsCount;
                if (stars != null) {
                    starsCount = stars;
                } else {
                    starsCount = 0;
                }
                tvStars.setText(getString(R.string.child_stars_count, starsCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadChildTasks() {
        // המשימות של הילד נמצאות מתחת להורה שלו, ולכן צריך גם parentId וגם childId.
        FirebaseDatabase.getInstance().getReference("parents").child(parentId).child("children").child(childId).child("tasks")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int open = 0, urgent = 0, overdue = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        ChildTask task = snap.getValue(ChildTask.class);
                        if (task == null) {
                            continue;
                        }
                        task.setTaskId(snap.getKey());
                        allTasks.add(task);
                        if (!task.getIsDone()) {
                            open++;
                            if (DateUtils.isOverdue(task.getDueAt())) {
                                overdue++;
                            } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                urgent++;
                            }
                        }
                    }
                }
                updateCountersUI(open, urgent, overdue);
                applyFilterAndRefresh();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void applyFilterAndRefresh() {
        visibleTasks.clear();
        for (ChildTask task : allTasks) {
            // משימות שסומנו כבוצעו לעולם לא יופיעו לילד - הוא רואה רק משימות פתוחות.
            if (task.getIsDone()) {
                continue;
            }
            boolean match = false;
            switch (activeFilter) {
                case NOT_COMPLETED:
                    match = true;
                    break;
                case URGENT:
                    match = DateUtils.isDueSoon(task.getDueAt());
                    break;
                case OVERDUE:
                    match = DateUtils.isOverdue(task.getDueAt());
                    break;
            }
            if (match) {
                visibleTasks.add(task);
            }
        }
        if (visibleTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
        }
        taskAdapter.notifyDataSetChanged();
    }

    private void updateFilterUI() {
        // צובע את הפילטר הפעיל עם רקע אפור שקוף ואת השאר שקופים
        if (activeFilter == FilterMode.NOT_COMPLETED) {
            filterOpen.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            filterOpen.setBackgroundColor(Color.TRANSPARENT);
        }
        if (activeFilter == FilterMode.URGENT) {
            filterUrgent.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            filterUrgent.setBackgroundColor(Color.TRANSPARENT);
        }
        if (activeFilter == FilterMode.OVERDUE) {
            filterOverdue.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            filterOverdue.setBackgroundColor(Color.TRANSPARENT);
        }

        switch (activeFilter) {
            case NOT_COMPLETED:
                tvTaskSectionTitle.setText(R.string.filter_label_open);
                break;
            case URGENT:
                tvTaskSectionTitle.setText(R.string.filter_label_urgent);
                break;
            case OVERDUE:
                tvTaskSectionTitle.setText(R.string.filter_label_overdue);
                break;
        }
        applyFilterAndRefresh();
    }

    private void updateCountersUI(int open, int urgent, int overdue) {
        tvTotalTasks.setText(String.valueOf(open));
        tvDueSoon.setText(String.valueOf(urgent));
        tvOverdue.setText(String.valueOf(overdue));
    }

    private void resolveSession() {
        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        if (parentId == null || childId == null) {
            // --- נושא במחוון: SharedPreferences ---
            // אם המסך נפתח בלי Intent, מנסים לשחזר את הילד האחרון שנכנס מהמכשיר.
            SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
            parentId = sp.getString("parentId", null);
            childId = sp.getString("childId", null);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSharedPreferences("child_session", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(ChildDashboardActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }
}
