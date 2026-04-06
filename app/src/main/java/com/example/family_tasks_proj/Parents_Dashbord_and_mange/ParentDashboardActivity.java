package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String METRIC_NEUTRAL_BG = "#EEF4FA";
    private static final String METRIC_NEUTRAL_TEXT = "#355070";
    private static final String METRIC_NEUTRAL_STROKE = "#D6E4F0";
    private static final String METRIC_BLUE_BG = "#EAF4FF";
    private static final String METRIC_BLUE_TEXT = "#1F4E79";
    private static final String METRIC_BLUE_STROKE = "#B8DBFF";
    private static final String METRIC_GREEN_BG = "#ECF8F1";
    private static final String METRIC_GREEN_TEXT = "#1E7A45";
    private static final String METRIC_GREEN_STROKE = "#BDE7C9";
    private static final String METRIC_ORANGE_BG = "#FFF4E5";
    private static final String METRIC_ORANGE_TEXT = "#9C5A00";
    private static final String METRIC_ORANGE_STROKE = "#FFD199";

    private Button btnManageChildren;
    private Button btnManageTemplates;
    private Button btnAssignTaskToChild;
    private Button btnShowQR;
    private Button btnLogout;
    private TextView tvParentTotalTasks;
    private TextView tvParentCompleted;
    private TextView tvParentDueSoon;
    private TextView tvNoTasks;
    private TextView tvTaskSectionTitle;
    private TextView tvTaskSectionSubtitle;
    private TextView tvParentName;
    private TextView tvNoChildren;
    private ImageView ivParentProfile;
    private ListView lvTasks;
    private RecyclerView rvChildren;
    private TextView filterAllTasks;
    private TextView filterOpenTasks;
    private TextView filterCompletedTasks;
    private TextView filterUrgentTasks;

    private final List<AssignedTask> allAssignedTasks = new ArrayList<>();
    private final List<TaskListItem> visibleTaskItems = new ArrayList<>();
    private final List<ChildSummary> childSummaries = new ArrayList<>();
    private final Map<String, Bitmap> childPhotoCache = new HashMap<>();

    private ParentDashboardTaskAdapter taskAdapter;
    private ParentDashboardChildSummaryAdapter childSummaryAdapter;
    private String selectedChildId;

    private final ActivityResultLauncher<String> profileImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                Bitmap bitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (bitmap == null) {
                    Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                    return;
                }

                showParentProfile(bitmap);
                saveProfileImage(bitmap);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        bindViews();
        bindActions();
        setupChildrenList();
        setupTaskList();
        setupTaskFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = getSignedInParentOrRedirect();
        if (user == null) {
            return;
        }

        loadParentProfile(user);
        loadDashboardData(user);
    }

    private void bindViews() {
        ivParentProfile = findViewById(R.id.ivParentProfile);
        tvParentName = findViewById(R.id.tvParentName);
        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        tvTaskSectionSubtitle = findViewById(R.id.tvTaskSectionSubtitle);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        lvTasks = findViewById(R.id.lvTasks);
        rvChildren = findViewById(R.id.rvChildren);
        filterAllTasks = findViewById(R.id.filterAllTasks);
        filterOpenTasks = findViewById(R.id.filterOpenTasks);
        filterCompletedTasks = findViewById(R.id.filterCompletedTasks);
        filterUrgentTasks = findViewById(R.id.filterUrgentTasks);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnShowQR = findViewById(R.id.btnShowQR);
        btnLogout = findViewById(R.id.btnLogout);

        Button btnChangeProfilePic = findViewById(R.id.btnChangeProfilePic);
        btnChangeProfilePic.setOnClickListener(v -> profileImagePicker.launch("image/*"));
    }

    private void bindActions() {
        btnManageChildren.setOnClickListener(v ->
                startActivity(new Intent(this, ManageChildrenActivity.class)));

        btnManageTemplates.setOnClickListener(v ->
                startActivity(new Intent(this, ParentTaskTemplateActivity.class)));

        btnAssignTaskToChild.setOnClickListener(v ->
                startActivity(new Intent(this, AssignTaskToChildActivity.class)));

        btnShowQR.setOnClickListener(v ->
                startActivity(new Intent(this, GenerateQRActivity.class)));

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupChildrenList() {
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(
                this,
                childSummaries,
                childPhotoCache,
                this::selectChild);
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems, childPhotoCache);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener((parent, view, position, id) -> showTaskOptionsDialog(position));
    }

    private void setupTaskFilters() {
        filterAllTasks.setOnClickListener(null);
        filterOpenTasks.setOnClickListener(null);
        filterCompletedTasks.setOnClickListener(null);
        filterUrgentTasks.setOnClickListener(null);
        updateTaskFilterSelectionUi();
    }

    private FirebaseUser getSignedInParentOrRedirect() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user;
        }

        Toast.makeText(this, R.string.error_parent_session_missing, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return null;
    }

    private void loadParentProfile(@NonNull FirebaseUser user) {

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה"));

                        // כאן טוענים את תמונת ההורה כדי שיהיה קל לזהות מי מחובר למסך הזה
                        String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                        showParentProfile(ImageHelper.base64ToBitmap(base64));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void showParentProfile(Bitmap bitmap) {
        if (bitmap == null) {
            ivParentProfile.setImageDrawable(null);
            return;
        }

        ivParentProfile.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
    }

    private void saveProfileImage(Bitmap bitmap) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String base64 = ImageHelper.bitmapToBase64(bitmap);
        if (base64 == null) {
            Toast.makeText(this, R.string.error_image_conversion, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("profileImageBase64")
                .setValue(base64)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, R.string.success_parent_photo_saved, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_save_generic, e.getMessage()),
                                Toast.LENGTH_SHORT).show());
    }

    private void loadDashboardData(@NonNull FirebaseUser user) {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int done = 0;
                        int urgent = 0;
                        int assigned = 0;

                        allAssignedTasks.clear();
                        childSummaries.clear();
                        childPhotoCache.clear();

                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String childId = childSnap.getKey();
                            if (childId == null || childId.trim().isEmpty()) continue;

                            String firstName = childSnap.child("firstName").getValue(String.class);
                            String lastName = childSnap.child("lastName").getValue(String.class);
                            String childName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד");
                            String childProfileBase64 =
                                    childSnap.child("profileImageBase64").getValue(String.class);

                            ChildSummary summary = new ChildSummary();
                            summary.childId = childId;
                            summary.displayName = childName;
                            summary.childProfileBase64 = childProfileBase64;

                            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                                String taskId = taskSnap.getKey();
                                if (taskId == null || taskId.trim().isEmpty()) continue;

                                AssignedTask task = new AssignedTask();
                                task.childId = childId;
                                task.childName = childName;
                                task.childProfileBase64 = childProfileBase64;
                                task.taskId = taskId;
                                task.title = safeText(taskSnap.child("title").getValue(String.class));
                                task.dueAt = safeText(taskSnap.child("dueAt").getValue(String.class));

                                Boolean isDoneValue = taskSnap.child("isDone").getValue(Boolean.class);
                                task.isDone = isDoneValue != null && isDoneValue;

                                summary.totalCount++;

                                if (task.isDone) {
                                    done++;
                                    summary.completedCount++;
                                } else {
                                    assigned++;
                                    summary.assignedCount++;
                                    if (isUrgentTask(task)) {
                                        urgent++;
                                        summary.urgentCount++;
                                    }
                                }

                                allAssignedTasks.add(task);
                            }

                            childSummaries.add(summary);
                        }

                        ensureSelectedChild();
                        childSummaryAdapter.setSelectedChildId(selectedChildId);
                        updateTaskFilterSelectionUi();
                        childSummaryAdapter.notifyDataSetChanged();
                        updateChildrenVisibility();

                        tvParentTotalTasks.setText(String.valueOf(assigned));
                        tvParentCompleted.setText(String.valueOf(done));
                        tvParentDueSoon.setText(String.valueOf(urgent));

                        updateSelectedChildSection();
                        buildSelectedChildTaskList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                getString(R.string.error_loading_data, error.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ensureSelectedChild() {
        if (childSummaries.isEmpty()) {
            selectedChildId = null;
            return;
        }

        for (ChildSummary summary : childSummaries) {
            if (summary.childId.equals(selectedChildId)) {
                return;
            }
        }

        selectedChildId = childSummaries.get(0).childId;
    }

    private void updateChildrenVisibility() {
        if (childSummaries.isEmpty()) {
            rvChildren.setVisibility(View.GONE);
            tvNoChildren.setVisibility(View.VISIBLE);
        } else {
            rvChildren.setVisibility(View.VISIBLE);
            tvNoChildren.setVisibility(View.GONE);
        }
    }

    private void updateTaskFilterSelectionUi() {
        ChildSummary selectedChild = getSelectedChildSummary();

        int totalCount = selectedChild == null ? 0 : selectedChild.totalCount;
        int assignedCount = selectedChild == null ? 0 : selectedChild.assignedCount;
        int completedCount = selectedChild == null ? 0 : selectedChild.completedCount;
        int urgentCount = selectedChild == null ? 0 : selectedChild.urgentCount;

        bindMetricChip(filterAllTasks, R.string.parent_dashboard_summary_total,
                totalCount, METRIC_NEUTRAL_BG, METRIC_NEUTRAL_TEXT, METRIC_NEUTRAL_STROKE);
        bindMetricChip(filterOpenTasks, R.string.parent_dashboard_summary_assigned,
                assignedCount, METRIC_BLUE_BG, METRIC_BLUE_TEXT, METRIC_BLUE_STROKE);
        bindMetricChip(filterCompletedTasks, R.string.parent_dashboard_summary_completed,
                completedCount, METRIC_GREEN_BG, METRIC_GREEN_TEXT, METRIC_GREEN_STROKE);
        bindMetricChip(filterUrgentTasks, R.string.parent_dashboard_summary_urgent,
                urgentCount, METRIC_ORANGE_BG, METRIC_ORANGE_TEXT, METRIC_ORANGE_STROKE);

        setSummaryChipEnabled(filterAllTasks, selectedChild != null);
        setSummaryChipEnabled(filterOpenTasks, selectedChild != null);
        setSummaryChipEnabled(filterCompletedTasks, selectedChild != null);
        setSummaryChipEnabled(filterUrgentTasks, selectedChild != null);
    }

    private void bindMetricChip(TextView textView, int labelResId, int count,
                                String backgroundColor, String textColor, String strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(18));
        background.setColor(Color.parseColor(backgroundColor));
        background.setStroke(dpToPx(1), Color.parseColor(strokeColor));

        textView.setBackground(background);
        textView.setTextColor(Color.parseColor(textColor));
        textView.setText(getString(R.string.parent_dashboard_metric_with_count,
                getString(labelResId), count));
    }

    private void setSummaryChipEnabled(TextView textView, boolean enabled) {
        textView.setEnabled(false);
        textView.setClickable(false);
        textView.setFocusable(false);
        textView.setAlpha(enabled ? 1f : 0.5f);
    }

    private void updateSelectedChildSection() {
        ChildSummary selectedChild = getSelectedChildSummary();
        if (selectedChild == null) {
            tvTaskSectionTitle.setText(R.string.parent_dashboard_selected_child_empty_title);
            tvTaskSectionSubtitle.setText(R.string.parent_dashboard_selected_child_empty_subtitle);
            return;
        }

        tvTaskSectionTitle.setText(
                getString(R.string.parent_dashboard_selected_child_title, selectedChild.displayName));
        tvTaskSectionSubtitle.setText(
                getString(R.string.parent_dashboard_selected_child_stats,
                        selectedChild.assignedCount,
                        selectedChild.completedCount,
                        selectedChild.urgentCount));
    }

    private void buildSelectedChildTaskList() {
        visibleTaskItems.clear();

        ChildSummary selectedChild = getSelectedChildSummary();
        if (selectedChild == null) {
            tvNoTasks.setText(R.string.parent_dashboard_selected_child_empty_subtitle);
            tvNoTasks.setVisibility(View.VISIBLE);
            lvTasks.setVisibility(View.GONE);
            taskAdapter.notifyDataSetChanged();
            return;
        }

        // הסינון מתבצע רק על הילד שנבחר, כדי לא לערבב משימות של כל הבית
        List<AssignedTask> urgentTasks = new ArrayList<>();
        List<AssignedTask> assignedTasks = new ArrayList<>();
        List<AssignedTask> completedTasks = new ArrayList<>();

        // כאן מחלקים את המשימות של הילד שנבחר לשלוש קבוצות פשוטות וברורות.
        for (AssignedTask task : allAssignedTasks) {
            if (task == null) continue;
            if (!selectedChild.childId.equals(task.childId)) continue;

            if (task.isDone) {
                completedTasks.add(task);
            } else if (isUrgentTask(task)) {
                urgentTasks.add(task);
            } else {
                assignedTasks.add(task);
            }
        }

        addTaskSection(getString(R.string.parent_dashboard_group_urgent), urgentTasks);
        addTaskSection(getString(R.string.parent_dashboard_group_assigned), assignedTasks);
        addTaskSection(getString(R.string.parent_dashboard_group_completed), completedTasks);

        tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_all, selectedChild.displayName));
        taskAdapter.notifyDataSetChanged();

        if (visibleTaskItems.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            lvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            lvTasks.setVisibility(View.VISIBLE);
        }
    }

    private void addTaskSection(String title, List<AssignedTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        visibleTaskItems.add(TaskListItem.createHeader(title, tasks.size()));
        for (AssignedTask task : tasks) {
            visibleTaskItems.add(TaskListItem.createTask(task));
        }
    }

    private boolean isUrgentTask(AssignedTask task) {
        return task != null && !task.isDone && DateUtils.isDueSoon(task.dueAt);
    }

    private ChildSummary getSelectedChildSummary() {
        if (selectedChildId == null) return null;

        for (ChildSummary summary : childSummaries) {
            if (summary.childId.equals(selectedChildId)) {
                return summary;
            }
        }

        return null;
    }

    private void selectChild(String childId) {
        if (childId == null || childId.equals(selectedChildId)) return;

        // כאן מחליפים את הילד הפעיל, ואז מרעננים רק את החלקים שתלויים בבחירה
        selectedChildId = childId;
        childSummaryAdapter.setSelectedChildId(selectedChildId);
        childSummaryAdapter.notifyDataSetChanged();
        updateSelectedChildSection();
        updateTaskFilterSelectionUi();
        buildSelectedChildTaskList();
    }

    private AssignedTask getTaskAtPosition(int position) {
        if (position < 0 || position >= visibleTaskItems.size()) {
            return null;
        }

        TaskListItem item = visibleTaskItems.get(position);
        if (item == null || item.isHeader) {
            return null;
        }

        return item.task;
    }

    private void showTaskOptionsDialog(int position) {
        AssignedTask task = getTaskAtPosition(position);
        if (task == null) return;
        String status = getTaskStatusLabel(task);

        String details = getString(R.string.parent_dashboard_task_details,
                safeText(task.title),
                safeText(task.childName),
                safeText(task.dueAt),
                status);

        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_task_details_title)
                .setMessage(details)
                .setPositiveButton(R.string.parent_dashboard_change_date,
                        (dialog, which) -> showChangeDateDialog(task))
                .setNeutralButton(R.string.parent_dashboard_delete_task,
                        (dialog, which) -> showDeleteTaskDialog(task))
                .setNegativeButton(R.string.parent_dashboard_close, null)
                .show();
    }

    private void showChangeDateDialog(AssignedTask task) {
        if (task == null) return;
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            String newDate = day + "/" + (month + 1) + "/" + year;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseDatabase.getInstance()
                    .getReference("parents")
                    .child(user.getUid())
                    .child("children")
                    .child(task.childId)
                    .child("tasks")
                    .child(task.taskId)
                    .child("dueAt")
                    .setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.parent_dashboard_date_updated, Toast.LENGTH_SHORT).show();
                        FirebaseUser refreshedUser = getSignedInParentOrRedirect();
                        if (refreshedUser != null) {
                            loadDashboardData(refreshedUser);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    getString(R.string.error_with_details, e.getMessage()),
                                    Toast.LENGTH_SHORT).show());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDeleteTaskDialog(AssignedTask task) {
        if (task == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_delete_task_title)
                .setMessage(getString(R.string.parent_dashboard_delete_task_message, task.title))
                .setPositiveButton(R.string.parent_dashboard_delete_task, (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    FirebaseDatabase.getInstance()
                            .getReference("parents")
                            .child(user.getUid())
                            .child("children")
                            .child(task.childId)
                            .child("tasks")
                            .child(task.taskId)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, R.string.parent_dashboard_task_deleted, Toast.LENGTH_SHORT).show();
                                FirebaseUser refreshedUser = getSignedInParentOrRedirect();
                                if (refreshedUser != null) {
                                    loadDashboardData(refreshedUser);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.error_with_details, e.getMessage()),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_logout_title)
                .setMessage(R.string.parent_dashboard_logout_message)
                .setPositiveButton(R.string.parent_dashboard_logout_confirm, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // כאן טוענים את תמונת הילד לכל כרטיס או שורה, עם fallback אם אין תמונה תקינה
    private String getTaskStatusLabel(AssignedTask task) {
        if (task.isDone) {
            return getString(R.string.parent_dashboard_task_status_done);
        }

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) {
            return getString(R.string.parent_dashboard_task_status_late);
        }
        if (daysLeft <= 2) {
            return getString(R.string.parent_dashboard_task_status_urgent);
        }
        return getString(R.string.parent_dashboard_task_status_waiting);
    }

    private String getDueLine(AssignedTask task) {
        if (task.isDone) {
            return getString(R.string.parent_dashboard_task_due_done, task.dueAt);
        }

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) {
            return getString(R.string.parent_dashboard_task_due_late, task.dueAt);
        }
        if (daysLeft <= 2) {
            return getString(R.string.parent_dashboard_task_due_urgent, task.dueAt);
        }
        return getString(R.string.parent_dashboard_task_due_regular, task.dueAt);
    }

    private int getDueLineColor(AssignedTask task) {
        if (task.isDone) return Color.parseColor("#2E7D32");

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) return Color.parseColor("#C62828");
        if (daysLeft <= 2) return Color.parseColor("#E65100");
        return Color.parseColor("#6B7280");
    }

    private int dpToPx(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

}
