package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.android.material.card.MaterialCardView;
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

    private static final String CHIP_DEFAULT_TEXT_COLOR = "#355070";
    private static final String CHIP_SELECTED_TEXT_COLOR = "#FFFFFF";
    private static final String CHIP_DEFAULT_BG_COLOR = "#EFF4FA";
    private static final String CHIP_SELECTED_BG_COLOR = "#2F80ED";
    private static final String CHILD_CARD_DEFAULT_BG = "#FFFFFF";
    private static final String CHILD_CARD_SELECTED_BG = "#F3F8FF";
    private static final String CHILD_CARD_DEFAULT_STROKE = "#D9E2EC";
    private static final String CHILD_CARD_SELECTED_STROKE = "#2F80ED";

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
    private final List<AssignedTask> filteredAssignedTasks = new ArrayList<>();
    private final List<ChildSummary> childSummaries = new ArrayList<>();
    private final Map<String, Bitmap> childPhotoCache = new HashMap<>();

    private TaskItemAdapter taskAdapter;
    private ChildSummaryAdapter childSummaryAdapter;
    private FilterMode activeFilter = FilterMode.ALL;
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
        childSummaryAdapter = new ChildSummaryAdapter();
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new TaskItemAdapter();
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener((parent, view, position, id) -> showTaskOptionsDialog(position));
    }

    private void setupTaskFilters() {
        filterAllTasks.setOnClickListener(v -> setActiveFilter(FilterMode.ALL));
        filterOpenTasks.setOnClickListener(v -> setActiveFilter(FilterMode.OPEN));
        filterCompletedTasks.setOnClickListener(v -> setActiveFilter(FilterMode.COMPLETED));
        filterUrgentTasks.setOnClickListener(v -> setActiveFilter(FilterMode.URGENT));
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
                        int open = 0;

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

                                if (task.isDone) {
                                    done++;
                                    summary.completedCount++;
                                } else {
                                    open++;
                                    summary.openCount++;
                                    if (DateUtils.isDueSoon(task.dueAt)) {
                                        urgent++;
                                        summary.urgentCount++;
                                    }
                                }

                                allAssignedTasks.add(task);
                            }

                            childSummaries.add(summary);
                        }

                        ensureSelectedChild();
                        updateTaskFilterSelectionUi();
                        childSummaryAdapter.notifyDataSetChanged();
                        updateChildrenVisibility();

                        tvParentTotalTasks.setText(String.valueOf(open));
                        tvParentCompleted.setText(String.valueOf(done));
                        tvParentDueSoon.setText(String.valueOf(urgent));

                        updateSelectedChildSection();
                        applyFilter();
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

    private void setActiveFilter(FilterMode filterMode) {
        if (filterMode == null || activeFilter == filterMode) return;

        activeFilter = filterMode;
        updateTaskFilterSelectionUi();
        applyFilter();
    }

    private void updateTaskFilterSelectionUi() {
        updateFilterChip(filterAllTasks, activeFilter == FilterMode.ALL);
        updateFilterChip(filterOpenTasks, activeFilter == FilterMode.OPEN);
        updateFilterChip(filterCompletedTasks, activeFilter == FilterMode.COMPLETED);
        updateFilterChip(filterUrgentTasks, activeFilter == FilterMode.URGENT);

        boolean enabled = selectedChildId != null;
        filterAllTasks.setEnabled(enabled);
        filterOpenTasks.setEnabled(enabled);
        filterCompletedTasks.setEnabled(enabled);
        filterUrgentTasks.setEnabled(enabled);

        float alpha = enabled ? 1f : 0.5f;
        filterAllTasks.setAlpha(alpha);
        filterOpenTasks.setAlpha(alpha);
        filterCompletedTasks.setAlpha(alpha);
        filterUrgentTasks.setAlpha(alpha);
    }

    private void updateFilterChip(TextView textView, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(18));
        background.setColor(Color.parseColor(selected ? CHIP_SELECTED_BG_COLOR : CHIP_DEFAULT_BG_COLOR));
        background.setStroke(dpToPx(selected ? 2 : 1),
                Color.parseColor(selected ? CHIP_SELECTED_BG_COLOR : "#D6E4F0"));

        textView.setBackground(background);
        textView.setTextColor(Color.parseColor(selected ? CHIP_SELECTED_TEXT_COLOR : CHIP_DEFAULT_TEXT_COLOR));
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
                        selectedChild.openCount,
                        selectedChild.completedCount,
                        selectedChild.urgentCount));
    }

    private void applyFilter() {
        filteredAssignedTasks.clear();

        ChildSummary selectedChild = getSelectedChildSummary();
        if (selectedChild == null) {
            tvNoTasks.setText(R.string.parent_dashboard_selected_child_empty_subtitle);
            tvNoTasks.setVisibility(View.VISIBLE);
            lvTasks.setVisibility(View.GONE);
            taskAdapter.notifyDataSetChanged();
            return;
        }

        // הסינון מתבצע רק על הילד שנבחר, כדי לא לערבב משימות של כל הבית
        for (AssignedTask task : allAssignedTasks) {
            if (task == null) continue;
            if (!selectedChild.childId.equals(task.childId)) continue;
            if (matchesActiveFilter(task)) {
                filteredAssignedTasks.add(task);
            }
        }

        updateEmptyStateText(selectedChild.displayName);
        taskAdapter.notifyDataSetChanged();

        if (filteredAssignedTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            lvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            lvTasks.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesActiveFilter(AssignedTask task) {
        switch (activeFilter) {
            case OPEN:
                return !task.isDone;
            case COMPLETED:
                return task.isDone;
            case URGENT:
                return !task.isDone && DateUtils.isDueSoon(task.dueAt);
            case ALL:
            default:
                return true;
        }
    }

    private void updateEmptyStateText(String childName) {
        switch (activeFilter) {
            case OPEN:
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_open, childName));
                break;
            case COMPLETED:
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_completed, childName));
                break;
            case URGENT:
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_urgent, childName));
                break;
            case ALL:
            default:
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_all, childName));
                break;
        }
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
        childSummaryAdapter.notifyDataSetChanged();
        updateSelectedChildSection();
        updateTaskFilterSelectionUi();
        applyFilter();
    }

    private void showTaskOptionsDialog(int position) {
        if (position < 0 || position >= filteredAssignedTasks.size()) return;

        AssignedTask task = filteredAssignedTasks.get(position);
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
                        (dialog, which) -> showChangeDateDialog(position))
                .setNeutralButton(R.string.parent_dashboard_delete_task,
                        (dialog, which) -> showDeleteTaskDialog(position))
                .setNegativeButton(R.string.parent_dashboard_close, null)
                .show();
    }

    private void showChangeDateDialog(int position) {
        if (position < 0 || position >= filteredAssignedTasks.size()) return;

        AssignedTask task = filteredAssignedTasks.get(position);
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
                            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDeleteTaskDialog(int position) {
        if (position < 0 || position >= filteredAssignedTasks.size()) return;

        AssignedTask task = filteredAssignedTasks.get(position);

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
                                    Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
    private void bindChildPhoto(ImageView imageView, String childId, String base64) {
        imageView.setImageDrawable(null);

        if (base64 == null || base64.trim().isEmpty()) {
            return;
        }

        if (childPhotoCache.containsKey(childId)) {
            imageView.setImageBitmap(childPhotoCache.get(childId));
            return;
        }

        Bitmap raw = ImageHelper.base64ToBitmap(base64);
        if (raw == null) {
            return;
        }

        Bitmap circular = ImageHelper.getCircularBitmap(raw);
        childPhotoCache.put(childId, circular);
        imageView.setImageBitmap(circular);
    }

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

    private class TaskItemAdapter extends ArrayAdapter<AssignedTask> {

        TaskItemAdapter() {
            super(ParentDashboardActivity.this, 0, filteredAssignedTasks);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_parent_task, parent, false);
            }

            AssignedTask task = getItem(position);
            if (task == null) return convertView;

            ImageView ivChildPhoto = convertView.findViewById(R.id.ivChildPhoto);
            TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
            TextView tvChildNameCard = convertView.findViewById(R.id.tvChildNameCard);
            TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
            TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);

            tvTaskTitleCard.setText(task.title.isEmpty()
                    ? getString(R.string.default_task_name)
                    : task.title);
            tvChildNameCard.setText(task.childName);
            tvDueDateCard.setText(getDueLine(task));
            tvDueDateCard.setTextColor(getDueLineColor(task));

            String statusText = getTaskStatusLabel(task);
            int chipBgColor;
            int chipTextColor;

            if (task.isDone) {
                chipBgColor = Color.parseColor("#E8F5E9");
                chipTextColor = Color.parseColor("#2E7D32");
            } else if (DateUtils.daysLeft(task.dueAt) < 0) {
                chipBgColor = Color.parseColor("#FFEBEE");
                chipTextColor = Color.parseColor("#C62828");
            } else if (DateUtils.isDueSoon(task.dueAt)) {
                chipBgColor = Color.parseColor("#FFF3E0");
                chipTextColor = Color.parseColor("#E65100");
            } else {
                chipBgColor = Color.parseColor("#EEF2F7");
                chipTextColor = Color.parseColor("#52606D");
            }

            tvStatusChip.setText(statusText);
            tvStatusChip.setTextColor(chipTextColor);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(chipBgColor);
            chipBg.setCornerRadius(dpToPx(14));
            tvStatusChip.setBackground(chipBg);

            bindChildPhoto(ivChildPhoto, task.childId, task.childProfileBase64);
            return convertView;
        }
    }

    private class ChildSummaryAdapter extends RecyclerView.Adapter<ChildSummaryAdapter.ChildSummaryViewHolder> {

        @NonNull
        @Override
        public ChildSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_parent_child_summary, parent, false);
            return new ChildSummaryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChildSummaryViewHolder holder, int position) {
            ChildSummary childSummary = childSummaries.get(position);
            boolean isSelected = childSummary.childId.equals(selectedChildId);

            holder.tvChildSummaryName.setText(childSummary.displayName);
            holder.tvChildSummaryCounts.setText(
                    getString(R.string.parent_dashboard_child_counts,
                            childSummary.openCount,
                            childSummary.completedCount,
                            childSummary.urgentCount));

            holder.cardChildSummary.setCardBackgroundColor(
                    Color.parseColor(isSelected ? CHILD_CARD_SELECTED_BG : CHILD_CARD_DEFAULT_BG));
            holder.cardChildSummary.setStrokeColor(
                    Color.parseColor(isSelected ? CHILD_CARD_SELECTED_STROKE : CHILD_CARD_DEFAULT_STROKE));
            holder.cardChildSummary.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
            holder.itemView.setContentDescription(
                    getString(R.string.parent_dashboard_child_content_description, childSummary.displayName));

            bindChildPhoto(holder.ivChildSummaryPhoto,
                    childSummary.childId,
                    childSummary.childProfileBase64);

            holder.itemView.setOnClickListener(v -> selectChild(childSummary.childId));
        }

        @Override
        public int getItemCount() {
            return childSummaries.size();
        }

        class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
            private final MaterialCardView cardChildSummary;
            private final ImageView ivChildSummaryPhoto;
            private final TextView tvChildSummaryName;
            private final TextView tvChildSummaryCounts;

            ChildSummaryViewHolder(@NonNull View itemView) {
                super(itemView);
                cardChildSummary = itemView.findViewById(R.id.cardChildSummary);
                ivChildSummaryPhoto = itemView.findViewById(R.id.ivChildSummaryPhoto);
                tvChildSummaryName = itemView.findViewById(R.id.tvChildSummaryName);
                tvChildSummaryCounts = itemView.findViewById(R.id.tvChildSummaryCounts);
            }
        }
    }

    private static class AssignedTask {
        String childId;
        String childName;
        String childProfileBase64;
        String taskId;
        String title;
        String dueAt;
        boolean isDone;
    }

    private static class ChildSummary {
        String childId;
        String displayName;
        String childProfileBase64;
        int openCount;
        int completedCount;
        int urgentCount;
    }

    private enum FilterMode {
        ALL,
        OPEN,
        COMPLETED,
        URGENT
    }
}
