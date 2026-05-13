package com.example.family_tasks_proj.child;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * דשבורד הילד. מציג משימות פתוחות.
 * הקוד מחולק למתודות ברורות כדי שיהיה קל להוסיף כפתורים או שינויי לוגיקה בלייב.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "parentId";
    public static final String EXTRA_CHILD_ID = "childId";

    // --- 1. משתני ממשק ---
    private TextView tvChildName, tvTotalTasks, tvDueSoon, tvOverdue, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;
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

        // שלב א: טעינת מזהי ההורה והילד מה-Intent
        resolveIntentIds();
        if (parentId == null || childId == null) {
            returnToMainAfterMissingChild();
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
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvChildTasks);

        filterOpen = findViewById(R.id.filterOpen);
        filterUrgent = findViewById(R.id.filterUrgent);
        filterOverdue = findViewById(R.id.filterOverdue);
    }

    private void setupRecyclerView() {
        // RecyclerView מציג את המשימות ברשימה.
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

    // מחזיר את ההפניה הבסיסית של הילד ב-Firebase: parents/{parentId}/children/{childId}.
    // כל הקריאות והכתיבות במסך מתחילות מאותו מקום, אז שומרים את הנתיב במקום אחד.
    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance().getReference("parents")
                .child(parentId).child("children").child(childId);
    }

    // --- לוגיקה: סיום משימה ---
    private void processMarkTaskAsDone(ChildTask task) {
        // הגנה מפני לחיצה כפולה: אם המשימה כבר סומנה כבוצעה, לא כותבים שוב.
        if (task.getIsDone()) {
            return;
        }
        // Firebase עובד בצורה אסינכרונית: מסמנים בזיכרון כדי למנוע לחיצה כפולה,
        // ואם הכתיבה תיכשל נחזיר את המשימה למצב פתוח.
        task.setIsDone(true);

        // הילד משנה רק את הסטטוס של המשימה שלו ל-"בוצע".
        childRef().child("tasks").child(task.getId()).child("isDone").setValue(true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ChildDashboardActivity.this, R.string.child_task_mark_done_success, Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        task.setIsDone(false);
                        applyFilterAndRefresh();
                        Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_with_details, exception.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- לוגיקה: טעינת נתונים ---
    private void loadChildProfile() {
        childRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    returnToMainAfterMissingChild();
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // אם טעינת פרופיל הילד נכשלה - מציגים הודעת שגיאה.
                Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChildTasks() {
        // המשימות של הילד נמצאות מתחת להורה שלו, ולכן צריך גם parentId וגם childId.
        childRef().child("tasks").addValueEventListener(new ValueEventListener() {
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
                        task.setId(snap.getKey());
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
                // אם טעינת המשימות של הילד נכשלה - מציגים הודעה.
                Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
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
        tintFilter(filterOpen, activeFilter == FilterMode.NOT_COMPLETED);
        tintFilter(filterUrgent, activeFilter == FilterMode.URGENT);
        tintFilter(filterOverdue, activeFilter == FilterMode.OVERDUE);

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

    // עוזר לצביעת רקע של כפתור פילטר: אפור שקוף כשפעיל, שקוף לחלוטין אחרת
    private void tintFilter(View view, boolean active) {
        if (active) {
            view.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void updateCountersUI(int open, int urgent, int overdue) {
        tvTotalTasks.setText(String.valueOf(open));
        tvDueSoon.setText(String.valueOf(urgent));
        tvOverdue.setText(String.valueOf(overdue));
    }

    private void resolveIntentIds() {
        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
    }

    private void returnToMainAfterMissingChild() {
        Toast.makeText(this, R.string.error_child_missing, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
