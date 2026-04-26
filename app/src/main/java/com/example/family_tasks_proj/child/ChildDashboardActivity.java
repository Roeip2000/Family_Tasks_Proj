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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך דשבורד הילד.
 * קורא את הילד והמשימות מתוך /parents/{parentId}/children/{childId},
 * מציג סיכום כוכבים ומשימות, ומאפשר לילד לסמן משימה כבוצעה.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    private static final String PREFS_SESSION = "child_session";
    private static final String EXTRA_PARENT_ID = "parentId";
    private static final String EXTRA_CHILD_ID = "childId";
    private static final String ROOT_PARENTS = "parents";
    private static final String NODE_CHILDREN = "children";
    private static final String NODE_TASKS = "tasks";

    private TextView tvChildName;
    private TextView tvStars;
    private TextView tvTotalTasks;
    private TextView tvCompleted;
    private TextView tvDueSoon;
    private TextView tvNoTasks;
    private TextView tvTaskSectionTitle;
    private RecyclerView rvTasks;
    private Button btnLogout;
    private LinearLayout filterNotCompleted;
    private LinearLayout filterCompleted;
    private LinearLayout filterUrgent;
    private ImageView imgChildAvatar;

    private final List<ChildTask> allTasks = new ArrayList<>();
    private final List<ChildTask> visibleTasks = new ArrayList<>();

    private String parentId;
    private String childId;
    private ChildTaskAdapter adapter;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    // יוצר את המסך, פותר סשן, וטוען את נתוני הילד
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        bindViews();
        setupTaskList();
        setupFilters();
        bindActions();

        resolveSession();
        if (isBlank(parentId) || isBlank(childId)) {
            Toast.makeText(this, R.string.child_missing_session, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        updateFilterSelectionUi();
        updateFilterLabels();
        loadChildHeader();
        loadTasks();
    }

    // מחבר את כל רכיבי המסך
    private void bindViews() {
        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);
        btnLogout = findViewById(R.id.btnLogout);
        imgChildAvatar = findViewById(R.id.imgChildAvatar);
        filterNotCompleted = findViewById(R.id.filterNotCompleted);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterUrgent = findViewById(R.id.filterUrgent);
    }

    // מגדיר RecyclerView למשימות הילד
    private void setupTaskList() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(visibleTasks, new ChildTaskAdapter.OnTaskDoneListener() {
            @Override
            public void onTaskDone(ChildTask task) {
                markTaskDone(task);
            }
        });
        rvTasks.setAdapter(adapter);
    }

    // מחבר את כפתורי הפילטרים
    private void setupFilters() {
        filterNotCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.NOT_COMPLETED);
            }
        });
        filterCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.COMPLETED);
            }
        });
        filterUrgent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.URGENT);
            }
        });
    }

    // מחבר את כפתור ההתנתקות
    private void bindActions() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });
    }

    // קודם קורא parentId ו-childId מה-Intent, ואם חסר משהו משלים מ-SharedPreferences
    private void resolveSession() {
        Intent intent = getIntent();
        if (intent != null) {
            parentId = intent.getStringExtra(EXTRA_PARENT_ID);
            childId = intent.getStringExtra(EXTRA_CHILD_ID);
        }

        if (!isBlank(parentId) && !isBlank(childId)) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        parentId = preferences.getString(EXTRA_PARENT_ID, parentId);
        childId = preferences.getString(EXTRA_CHILD_ID, childId);
    }

    // מחזיר הפניה לילד הנוכחי: /parents/{parentId}/children/{childId}
    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance()
                .getReference(ROOT_PARENTS)
                .child(parentId)
                .child(NODE_CHILDREN)
                .child(childId);
    }

    // טוען שם ותמונת ילד מ-Firebase
    private void loadChildHeader() {
        DatabaseReference currentChildRef = childRef();
        currentChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bindChildHeader(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        ChildDashboardActivity.this,
                        getString(R.string.child_error_loading_name, error.getMessage()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // מציג שם ותמונה מתוך נתוני הילד שהגיעו מ-Firebase
    private void bindChildHeader(DataSnapshot snapshot) {
        String firstName = snapshot.child("firstName").getValue(String.class);
        String lastName = snapshot.child("lastName").getValue(String.class);
        String displayName = NameUtils.fullNameOrDefault(firstName, lastName, getString(R.string.default_child_name));

        tvChildName.setText(getString(R.string.child_hello_with_name, displayName));

        String base64 = snapshot.child("profileImageBase64").getValue(String.class);
        showChildAvatar(base64);
    }

    // מציג תמונת ילד עגולה או תמונה חלופית
    private void showChildAvatar(String base64) {
        if (isBlank(base64)) {
            imgChildAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        android.graphics.Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
        if (bitmap == null) {
            imgChildAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        imgChildAvatar.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
    }

    // טוען משימות מ-Firebase: /parents/{parentId}/children/{childId}/tasks
    private void loadTasks() {
        DatabaseReference tasksRef = childRef().child(NODE_TASKS);
        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleTasksSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        ChildDashboardActivity.this,
                        getString(R.string.child_error_loading_tasks, error.getMessage()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // ממיר את נתוני המשימות לרשימה וסיכומים
    private void handleTasksSnapshot(DataSnapshot snapshot) {
        allTasks.clear();

        TaskCounts counts = new TaskCounts();
        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
            addTaskFromSnapshot(taskSnapshot, counts);
        }

        bindTaskCounts(counts);
        applyFilter();
    }

    // מוסיף משימה אחת מהרשומה שלה ב-Firebase ומעדכן סיכומים
    private void addTaskFromSnapshot(DataSnapshot taskSnapshot, TaskCounts counts) {
        ChildTask task = taskSnapshot.getValue(ChildTask.class);
        if (task == null) {
            return;
        }

        if (isBlank(task.getId())) {
            task.setId(taskSnapshot.getKey());
        }

        countTask(task, counts);
        allTasks.add(task);
    }

    // מעדכן מוני פתוחות, בוצעו, דחופות וכוכבים
    private void countTask(ChildTask task, TaskCounts counts) {
        if (task.getIsDone()) {
            counts.completedCount++;
            counts.stars += task.getStarsWorth();
            return;
        }

        counts.openCount++;
        // משימה נחשבת דחופה אם היא קרובה למועד (Due Soon) או שכבר עבר המועד (Overdue)
        if (DateUtils.isDueSoon(task.getDueAt()) || DateUtils.isOverdue(task.getDueAt())) {
            counts.urgentCount++;
        }
    }

    // מציג את הסיכומים בראש הדשבורד
    private void bindTaskCounts(TaskCounts counts) {
        tvTotalTasks.setText(String.valueOf(counts.openCount));
        tvCompleted.setText(String.valueOf(counts.completedCount));
        tvDueSoon.setText(String.valueOf(counts.urgentCount));
        tvStars.setText(getString(R.string.child_stars_count, counts.stars));
    }

    // מבקש אישור לפני סימון משימה כבוצעה
    private void markTaskDone(final ChildTask task) {
        if (task == null || isBlank(task.getId())) {
            Toast.makeText(this, R.string.child_error_missing_task_id, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.child_mark_task_title)
                .setMessage(getString(R.string.child_mark_task_message, safeText(task.getTitle())))
                .setPositiveButton(R.string.child_mark_task_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        writeTaskDone(task);
                    }
                })
                .setNegativeButton(R.string.child_mark_task_later, null)
                .show();
    }

    // כותב isDone=true ב-Firebase עבור המשימה שנבחרה
    private void writeTaskDone(final ChildTask task) {
        DatabaseReference doneRef = childRef()
                .child(NODE_TASKS)
                .child(task.getId())
                .child("isDone");
        Task<Void> updateTask = doneRef.setValue(true);

        updateTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                handleTaskMarkedDone(task);
            }
        });

        updateTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(
                        ChildDashboardActivity.this,
                        getString(R.string.child_error_updating_task, exception.getMessage()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // מרענן את הרשימה אחרי סימון משימה כבוצעה
    private void handleTaskMarkedDone(ChildTask task) {
        Toast.makeText(this, R.string.child_mark_task_success, Toast.LENGTH_SHORT).show();
        task.setIsDone(true);
        loadTasks();
    }

    // מציג דיאלוג אישור להתנתקות ילד
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.child_logout_title)
                .setMessage(R.string.child_logout_message)
                .setPositiveButton(R.string.child_logout_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logoutChild();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מוחק את סשן הילד המקומי וחוזר למסך הראשי
    private void logoutChild() {
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // משנה פילטר פעיל ומרענן את הרשימה
    private void setActiveFilter(FilterMode filterMode) {
        if (filterMode == null || filterMode == activeFilter) {
            return;
        }

        activeFilter = filterMode;
        updateFilterSelectionUi();
        updateFilterLabels();
        applyFilter();
    }

    // מסנן את הרשימה שכבר נטענה מ-Firebase בלי קריאה נוספת
    private void applyFilter() {
        visibleTasks.clear();

        for (ChildTask task : allTasks) {
            if (task != null && matchesActiveFilter(task)) {
                visibleTasks.add(task);
            }
        }

        boolean hasTasks = !visibleTasks.isEmpty();
        tvNoTasks.setVisibility(hasTasks ? View.GONE : View.VISIBLE);
        rvTasks.setVisibility(hasTasks ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    // בודק אם משימה מתאימה לפילטר הנוכחי
    private boolean matchesActiveFilter(ChildTask task) {
        switch (activeFilter) {
            case COMPLETED:
                return task.getIsDone();
            case URGENT:
                // משימה דחופה היא כזו שלא בוצעה והיא או קרובה למועד או שכבר עבר המועד
                return !task.getIsDone() && (DateUtils.isDueSoon(task.getDueAt()) || DateUtils.isOverdue(task.getDueAt()));
            default:
                return !task.getIsDone();
        }
    }

    // מעדכן כותרת והודעת ריק לפי הפילטר
    private void updateFilterLabels() {
        switch (activeFilter) {
            case COMPLETED:
                tvTaskSectionTitle.setText(R.string.child_task_section_completed);
                tvNoTasks.setText(R.string.child_no_tasks_completed);
                break;
            case URGENT:
                tvTaskSectionTitle.setText(R.string.child_task_section_urgent);
                tvNoTasks.setText(R.string.child_no_tasks_urgent);
                break;
            default:
                tvTaskSectionTitle.setText(R.string.child_task_section_open);
                tvNoTasks.setText(R.string.child_no_tasks_open);
                break;
        }
    }

    // מעדכן את העיצוב של שלושת כפתורי הפילטר
    private void updateFilterSelectionUi() {
        updateFilterBlock(filterUrgent, activeFilter == FilterMode.URGENT, R.color.surface_soft_orange);
        updateFilterBlock(filterCompleted, activeFilter == FilterMode.COMPLETED, R.color.surface_soft_green);
        updateFilterBlock(filterNotCompleted, activeFilter == FilterMode.NOT_COMPLETED, R.color.surface_soft_blue);
    }

    // צובע פילטר אחד לפי מצב הבחירה
    private void updateFilterBlock(LinearLayout layout, boolean selected, int fillColorRes) {
        int fillColor;
        int strokeWidth;
        int strokeColor;
        int textColor;

        if (selected) {
            fillColor = getColor(fillColorRes);
            strokeWidth = 2;
            strokeColor = getColor(R.color.primary);
            textColor = getColor(R.color.text_primary);
        } else {
            fillColor = getColor(android.R.color.transparent);
            strokeWidth = 0;
            strokeColor = getColor(android.R.color.transparent);
            textColor = getColor(R.color.text_secondary);
        }

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(fillColor);
        background.setStroke(strokeWidth, strokeColor);

        layout.setSelected(selected);
        layout.setBackground(background);
        layout.setAlpha(1f);
        layout.setElevation(0f);

        updateFilterTextColors(layout, selected, textColor);
    }

    // צובע את הטקסטים שנמצאים בתוך כפתור פילטר
    private void updateFilterTextColors(LinearLayout layout, boolean selected, int textColor) {
        for (int index = 0; index < layout.getChildCount(); index++) {
            View child = layout.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(textColor);
                if (selected) {
                    child.setAlpha(1f);
                } else {
                    child.setAlpha(0.9f);
                }
            }
        }
    }

    // בודק null או מחרוזת ריקה אחרי ניקוי רווחים
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // מחזיר טקסט בטוח להצגה בדיאלוג
    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * מונה פשוט לסיכומי המשימות של הילד.
     */
    private static class TaskCounts {
        int completedCount;
        int urgentCount;
        int openCount;
        long stars;
    }

    /**
     * פילטרים אפשריים למשימות הילד.
     */
    private enum FilterMode {
        NOT_COMPLETED,
        COMPLETED,
        URGENT
    }
}
