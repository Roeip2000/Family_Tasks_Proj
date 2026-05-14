package com.example.family_tasks_proj.parent;

import android.content.Intent;
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

import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.parent.adapter.ParentDashboardTaskAdapter;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

// דשבורד ניהול להורה - מציג את כל המשימות של כל הילדים וסיכום נתונים
public class ParentDashboardActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש (UI)
    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvName, tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvFilterTitle;
    private View fOpen, fUrgent, fOverdue, fDone;
    private RecyclerView rvTasks;

    // רשימות לשמירת הנתונים בזיכרון
    private final List<AssignedTask> allTasks = new ArrayList<>(); // כל המשימות שנטענו
    private final List<AssignedTask> visibleTasks = new ArrayList<>(); // רק המשימות שמוצגות כרגע לפי הפילטר

    private ParentDashboardTaskAdapter taskAdapter;
    private FilterMode activeFilter = FilterMode.ALL; // מצב הסינון הנוכחי
    private DatabaseReference childrenReference;
    private ValueEventListener childrenListener;

    // הגדרת סוגי הפילטרים האפשריים
    private enum FilterMode { ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);
        
        // אתחול רכיבים וקישור לממשק
        initViews();
        setupActions();
        setupLists();
        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // בדיקה שההורה מחובר בכל פעם שחוזרים למסך
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            tvName.setText(R.string.parent_greeting);
            loadData(user); // טעינת נתונים מ-Firebase
        } else {
            // אם לא מחובר, מחזירים למסך הכניסה
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // הסרת המאזין של Firebase כדי לחסוך במשאבים כשהמסך לא גלוי
        removeChildrenListener();
    }

    // קישור המשתנים לרכיבים ב-XML
    private void initViews() {
        tvName = findViewById(R.id.tvParentName);
        tvTotal = findViewById(R.id.tvParentTotalTasks);
        tvDone = findViewById(R.id.tvParentCompleted);
        tvUrgent = findViewById(R.id.tvParentDueSoon);
        tvOverdue = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvFilterTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);

        fOpen = findViewById(R.id.containerFilterOpen);
        fUrgent = findViewById(R.id.containerFilterUrgent);
        fOverdue = findViewById(R.id.containerFilterOverdue);
        fDone = findViewById(R.id.containerFilterCompleted);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);
    }

    // הגדרת לחיצות על כפתורי הסינון (פילטרים)
    private void setupFilters() {
        View.OnClickListener filterClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == fOpen) {
                    setFilter(FilterMode.ASSIGNED);
                } else if (v == fUrgent) {
                    setFilter(FilterMode.URGENT);
                } else if (v == fOverdue) {
                    setFilter(FilterMode.OVERDUE);
                } else if (v == fDone) {
                    setFilter(FilterMode.COMPLETED);
                }
            }
        };
        fOpen.setOnClickListener(filterClick);
        fUrgent.setOnClickListener(filterClick);
        fOverdue.setOnClickListener(filterClick);
        fDone.setOnClickListener(filterClick);
    }

    // עדכון הפילטר הנבחר וריענון הרשימה
    private void setFilter(FilterMode mode) {
        if (activeFilter == mode) {
            activeFilter = FilterMode.ALL; // לחיצה חוזרת מבטלת את הפילטר
        } else {
            activeFilter = mode;
        }
        updateFilterUI();
        refreshTaskList();
    }

    // עדכון המראה של הפילטרים בממשק
    private void updateFilterUI() {
        tintFilter(fOpen, activeFilter == FilterMode.ASSIGNED);
        tintFilter(fUrgent, activeFilter == FilterMode.URGENT);
        tintFilter(fOverdue, activeFilter == FilterMode.OVERDUE);
        tintFilter(fDone, activeFilter == FilterMode.COMPLETED);

        // עדכון הכותרת מעל רשימת המשימות
        if (activeFilter == FilterMode.ASSIGNED) {
            tvFilterTitle.setText(R.string.parent_filter_open);
        } else if (activeFilter == FilterMode.URGENT) {
            tvFilterTitle.setText(R.string.parent_filter_urgent);
        } else if (activeFilter == FilterMode.OVERDUE) {
            tvFilterTitle.setText(R.string.parent_filter_overdue);
        } else if (activeFilter == FilterMode.COMPLETED) {
            tvFilterTitle.setText(R.string.parent_filter_completed);
        } else {
            tvFilterTitle.setText(R.string.parent_filter_all);
        }
    }

    private void tintFilter(View view, boolean active) {
        if (active) {
            view.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // הגדרת פעולות הכפתורים למעבר למסכים אחרים
    private void setupActions() {
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });
        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });
    }

    // הגדרת ה-RecyclerView וה-Adapter
    private void setupLists() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTasks);
        taskAdapter.setShowChildName(true);

        // טיפול בלחיצה על משימה ברשימה
        taskAdapter.setOnItemClickListener(new ParentDashboardTaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AssignedTask task, int position) {
                String message = getString(R.string.parent_task_click_toast, task.getChildName(), task.getTitle());
                Toast.makeText(ParentDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        rvTasks.setAdapter(taskAdapter);
    }

    // טעינת נתונים מ-Firebase
    private void loadData(FirebaseUser user) {
        removeChildrenListener();
        
        // נתיב ב-Database לילדים של ההורה הנוכחי
        childrenReference = FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children");
        
        childrenListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear(); // מנקים את הרשימה הישנה
                
                // מונים לסיכום המשימות
                int totalCount = 0;
                int doneCount = 0;
                int urgentCount = 0;
                int lateCount = 0;

                if (snapshot.exists()) {
                    // עוברים על כל הילדים
                    for (DataSnapshot childSnap : snapshot.getChildren()) {
                        String childId = childSnap.getKey();
                        String childName = childSnap.child("firstName").getValue(String.class);
                        
                        // עוברים על כל המשימות של כל ילד
                        DataSnapshot tasksSnap = childSnap.child("tasks");
                        for (DataSnapshot tSnap : tasksSnap.getChildren()) {
                            
                            // יצירת אובייקט משימה מנתוני ה-Firebase
                            AssignedTask task = new AssignedTask();
                            task.setChildId(childId);
                            task.setChildName(childName != null ? childName : getString(R.string.default_child_name_fallback));
                            task.setTaskId(tSnap.getKey());
                            task.setTitle(tSnap.child("title").getValue(String.class));
                            task.setDueAt(tSnap.child("dueAt").getValue(String.class));
                            task.setImageBase64(tSnap.child("imageBase64").getValue(String.class));
                            
                            Boolean isDone = tSnap.child("isDone").getValue(Boolean.class);
                            task.setIsDone(isDone != null && isDone);

                            allTasks.add(task); // הוספה לרשימה הכללית בזיכרון

                            // עדכון המונים לפי מצב המשימה
                            if (task.getIsDone()) {
                                doneCount++;
                            } else {
                                totalCount++;
                                if (DateUtils.isOverdue(task.getDueAt())) {
                                    lateCount++;
                                } else if (DateUtils.isDueSoon(task.getDueAt())) {
                                    urgentCount++;
                                }
                            }
                        }
                    }
                }
                
                // עדכון מספרי הסיכום במסך
                tvTotal.setText(String.valueOf(totalCount));
                tvDone.setText(String.valueOf(doneCount));
                tvUrgent.setText(String.valueOf(urgentCount));
                tvOverdue.setText(String.valueOf(lateCount));

                // עדכון הרשימה שמוצגת לפי הפילטר
                refreshTaskList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };
        childrenReference.addValueEventListener(childrenListener);
    }

    private void removeChildrenListener() {
        if (childrenReference != null && childrenListener != null) {
            childrenReference.removeEventListener(childrenListener);
        }
        childrenReference = null;
        childrenListener = null;
    }

    // ריענון הרשימה המוצגת (visibleTasks) מתוך כל הרשימה (allTasks) לפי הפילטר שנבחר
    private void refreshTaskList() {
        visibleTasks.clear();
        
        for (AssignedTask task : allTasks) {
            boolean isMatch = false;
            
            // בדיקה האם המשימה מתאימה לפילטר הפעיל
            if (activeFilter == FilterMode.ASSIGNED) {
                isMatch = !task.getIsDone();
            } else if (activeFilter == FilterMode.COMPLETED) {
                isMatch = task.getIsDone();
            } else if (activeFilter == FilterMode.URGENT) {
                isMatch = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt());
            } else if (activeFilter == FilterMode.OVERDUE) {
                isMatch = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt());
            } else {
                isMatch = true; // במצב ALL מציגים הכל
            }
            
            if (isMatch) {
                visibleTasks.add(task);
            }
        }
        
        // הצגה או הסתרה של הודעת "אין משימות"
        if (visibleTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
        }
        
        // הודעה ל-Adapter שהנתונים השתנו וצריך לרענן את ה-RecyclerView
        taskAdapter.notifyDataSetChanged();
    }
}
