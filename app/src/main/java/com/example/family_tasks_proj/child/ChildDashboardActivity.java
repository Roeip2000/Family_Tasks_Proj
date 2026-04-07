package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChildDashboardActivity extends AppCompatActivity {

    private static final String FILTER_DEFAULT_TEXT_COLOR = "#355070";
    private static final String FILTER_SELECTED_TEXT_COLOR = "#102A43";
    private static final String FILTER_DEFAULT_STROKE_COLOR = "#D6E4F0";
    private static final String FILTER_SELECTED_STROKE_COLOR = "#FFFFFF";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // רקע גרדיאנט סגול-כחול למסך הילד
        ScrollView root = findViewById(R.id.scrollRootChildDashboard);
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#7B61FF"), Color.parseColor("#4A90E2")}));

        bindViews();
        setupTaskList();
        setupFilters();

        btnLogout.setOnClickListener(v -> showLogoutDialog());

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

    private void setupTaskList() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(visibleTasks, this::markTaskDone);
        rvTasks.setAdapter(adapter);
    }

    private void setupFilters() {
        filterNotCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.NOT_COMPLETED));
        filterCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.COMPLETED));
        filterUrgent.setOnClickListener(v -> setActiveFilter(FilterMode.URGENT));
    }

    private void resolveSession() {
        Intent intent = getIntent();
        if (intent != null) {
            parentId = intent.getStringExtra(EXTRA_PARENT_ID);
            childId = intent.getStringExtra(EXTRA_CHILD_ID);
        }

        if (!isBlank(parentId) && !isBlank(childId)) {
            return;
        }

        // אם לא הגיעו extras, מנסים להשלים את הסשן מהכניסה האחרונה של הילד
        SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        parentId = preferences.getString(EXTRA_PARENT_ID, parentId);
        childId = preferences.getString(EXTRA_CHILD_ID, childId);
    }

    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance()
                .getReference(ROOT_PARENTS)
                .child(parentId)
                .child(NODE_CHILDREN)
                .child(childId);
    }

    private void loadChildHeader() {
        childRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String displayName = NameUtils.fullNameOrDefault(firstName, lastName, getString(R.string.default_child_name));

                tvChildName.setText(getString(R.string.child_hello_with_name, displayName));

                String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                if (isBlank(base64)) {
                    imgChildAvatar.setImageDrawable(null);
                    return;
                }

                android.graphics.Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
                if (bitmap == null) {
                    imgChildAvatar.setImageDrawable(null);
                    return;
                }

                imgChildAvatar.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
                imgChildAvatar.setAlpha(0f);
                imgChildAvatar.animate().alpha(1f).setDuration(300).start();
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

    private void loadTasks() {
        childRef().child(NODE_TASKS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();

                int completedCount = 0;
                int urgentCount = 0;
                int openCount = 0;
                long stars = 0;

                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    ChildTask task = taskSnapshot.getValue(ChildTask.class);
                    if (task == null) {
                        continue;
                    }

                    if (isBlank(task.id)) {
                        task.id = taskSnapshot.getKey();
                    }

                    if (task.isDone) {
                        completedCount++;
                        stars += task.starsWorth;
                    } else {
                        openCount++;
                        if (DateUtils.isDueSoon(task.dueAt)) {
                            urgentCount++;
                        }
                    }

                    allTasks.add(task);
                }

                // קודם מעדכנים את הסיכום העליון, ואז מפעילים את הפילטר הפעיל על הרשימה
                tvTotalTasks.setText(String.valueOf(openCount));
                tvCompleted.setText(String.valueOf(completedCount));
                tvDueSoon.setText(String.valueOf(urgentCount));
                tvStars.setText(getString(R.string.child_stars_count, stars));

                applyFilter();
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

    private void markTaskDone(ChildTask task) {
        if (task == null || isBlank(task.id)) {
            Toast.makeText(this, R.string.child_error_missing_task_id, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.child_mark_task_title)
                .setMessage(getString(R.string.child_mark_task_message, safeText(task.title)))
                .setPositiveButton(R.string.child_mark_task_confirm, (dialog, which) ->
                        childRef()
                                .child(NODE_TASKS)
                                .child(task.id)
                                .child("isDone")
                                .setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(
                                            ChildDashboardActivity.this,
                                            R.string.child_mark_task_success,
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    task.isDone = true;
                                    // מרעננים כדי לעדכן גם את הספירות וגם את תוצאות הפילטר
                                    loadTasks();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        ChildDashboardActivity.this,
                                        getString(R.string.child_error_updating_task, e.getMessage()),
                                        Toast.LENGTH_SHORT
                                ).show()))
                .setNegativeButton(R.string.child_mark_task_later, null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.child_logout_title)
                .setMessage(R.string.child_logout_message)
                .setPositiveButton(R.string.child_logout_confirm, (dialog, which) -> {
                    // מוחקים את הסשן המקומי כדי שלא ניכנס אוטומטית עם ילד ישן
                    SharedPreferences preferences = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
                    preferences.edit().clear().apply();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void setActiveFilter(FilterMode filterMode) {
        if (filterMode == null || filterMode == activeFilter) return;

        activeFilter = filterMode;
        updateFilterSelectionUi();
        updateFilterLabels();
        applyFilter();
    }

    private void applyFilter() {
        visibleTasks.clear();

        // הפילטר עובד רק על הרשימה שכבר נטענה מה-Firebase — בלי query נוסף
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

    private boolean matchesActiveFilter(ChildTask task) {
        switch (activeFilter) {
            case COMPLETED:  return task.isDone;
            case URGENT:     return !task.isDone && DateUtils.isDueSoon(task.dueAt);
            default:         return !task.isDone;
        }
    }

    // מעדכן את כותרת הסקשן ואת טקסט ה-empty state לפי הפילטר הנוכחי
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

    private void updateFilterSelectionUi() {
        updateFilterBlock(filterUrgent, activeFilter == FilterMode.URGENT, "#FFF4E5", "#FFD199");
        updateFilterBlock(filterCompleted, activeFilter == FilterMode.COMPLETED, "#ECF8F1", "#BDE7C9");
        updateFilterBlock(filterNotCompleted, activeFilter == FilterMode.NOT_COMPLETED, "#EAF4FF", "#B8DBFF");
    }

    private void updateFilterBlock(LinearLayout layout, boolean selected, String defaultColor, String selectedColor) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(Color.parseColor(selected ? selectedColor : defaultColor));
        background.setStroke(selected ? 4 : 2,
                Color.parseColor(selected ? FILTER_SELECTED_STROKE_COLOR : FILTER_DEFAULT_STROKE_COLOR));

        layout.setSelected(selected);
        layout.setBackground(background);
        layout.setAlpha(1f);
        layout.setScaleX(selected ? 1.02f : 1f);
        layout.setScaleY(selected ? 1.02f : 1f);
        layout.setElevation(selected ? 6f : 0f);

        int textColor = Color.parseColor(selected ? FILTER_SELECTED_TEXT_COLOR : FILTER_DEFAULT_TEXT_COLOR);
        for (int index = 0; index < layout.getChildCount(); index++) {
            View child = layout.getChildAt(index);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(textColor);
                child.setAlpha(selected ? 1f : 0.86f);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private enum FilterMode {
        NOT_COMPLETED,
        COMPLETED,
        URGENT
    }
}
