package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.ChildSummary;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskListItem;
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

    // מצב הפילטר הנוכחי — איזה סוג משימות מוצג ברשימה
    private FilterMode activeFilter = FilterMode.ASSIGNED;

    private enum FilterMode {
        ALL, ASSIGNED, COMPLETED, URGENT
    }

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
        rvChildren.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(
                this,
                childSummaries,
                childPhotoCache,
                this::selectChild);
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener((parent, view, position, id) -> showTaskOptionsDialog(position));
    }

    // כאן מגדירים את הפילטרים — לחיצה על צ'יפ משנה את סוג המשימות שמוצג
    private void setupTaskFilters() {
        filterAllTasks.setOnClickListener(v -> setActiveFilter(FilterMode.ALL));
        filterOpenTasks.setOnClickListener(v -> setActiveFilter(FilterMode.ASSIGNED));
        filterCompletedTasks.setOnClickListener(v -> setActiveFilter(FilterMode.COMPLETED));
        filterUrgentTasks.setOnClickListener(v -> setActiveFilter(FilterMode.URGENT));
        updateTaskFilterSelectionUi();
    }

    private void setActiveFilter(FilterMode mode) {
        if (mode == activeFilter) return;
        activeFilter = mode;
        updateTaskFilterSelectionUi();
        buildSelectedChildTaskList();
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

    // טוען את כל הנתונים מ-Firebase ומרענן את הדשבורד
    private void loadDashboardData(@NonNull FirebaseUser user) {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allAssignedTasks.clear();
                        childSummaries.clear();
                        childPhotoCache.clear();

                        // סופרים את כל המשימות לסיכום הבית הכללי
                        int[] counts = parseDashboardData(snapshot);

                        refreshDashboardUi(counts[0], counts[1], counts[2]);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                getString(R.string.error_loading_data, error.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מפענח את כל נתוני הילדים והמשימות מה-Snapshot.
     * מחזיר מערך [assigned, done, urgent] — ספירות כלליות לסיכום הבית.
     */
    private int[] parseDashboardData(DataSnapshot snapshot) {
        int done = 0;
        int urgent = 0;
        int assigned = 0;

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

            // עוברים על כל המשימות של הילד הזה
            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                AssignedTask task = parseTask(taskSnap, childId, childName, childProfileBase64);
                if (task == null) continue;

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

        return new int[]{assigned, done, urgent};
    }

    // מפענח משימה בודדת מ-DataSnapshot
    private AssignedTask parseTask(DataSnapshot taskSnap, String childId,
                                    String childName, String childProfileBase64) {
        String taskId = taskSnap.getKey();
        if (taskId == null || taskId.trim().isEmpty()) return null;

        AssignedTask task = new AssignedTask();
        task.childId = childId;
        task.childName = childName;
        task.childProfileBase64 = childProfileBase64;
        task.taskId = taskId;
        task.title = safeText(taskSnap.child("title").getValue(String.class));
        task.dueAt = safeText(taskSnap.child("dueAt").getValue(String.class));

        Boolean isDoneValue = taskSnap.child("isDone").getValue(Boolean.class);
        task.isDone = isDoneValue != null && isDoneValue;
        return task;
    }

    // מרענן את כל חלקי הממשק אחרי טעינת נתונים
    private void refreshDashboardUi(int assigned, int done, int urgent) {
        ensureSelectedChild();
        childSummaryAdapter.setSelectedChildId(selectedChildId);
        childSummaryAdapter.notifyDataSetChanged();

        boolean hasChildren = !childSummaries.isEmpty();
        rvChildren.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
        tvNoChildren.setVisibility(hasChildren ? View.GONE : View.VISIBLE);

        tvParentTotalTasks.setText(String.valueOf(assigned));
        tvParentCompleted.setText(String.valueOf(done));
        tvParentDueSoon.setText(String.valueOf(urgent));

        updateTaskFilterSelectionUi();
        updateSelectedChildSection();
        buildSelectedChildTaskList();
    }

    // מוודא שיש ילד נבחר תקף — אם לא, בוחר את הראשון ברשימה
    private void ensureSelectedChild() {
        if (childSummaries.isEmpty()) {
            selectedChildId = null;
            return;
        }
        for (ChildSummary s : childSummaries) {
            if (s.childId.equals(selectedChildId)) return;
        }
        selectedChildId = childSummaries.get(0).childId;
    }

    private void updateTaskFilterSelectionUi() {
        boolean enabled = getSelectedChildSummary() != null;
        bindTaskTab(filterOpenTasks, activeFilter == FilterMode.ASSIGNED, enabled);
        bindTaskTab(filterCompletedTasks, activeFilter == FilterMode.COMPLETED, enabled);
        bindTaskTab(filterUrgentTasks, activeFilter == FilterMode.URGENT, enabled);
    }

    private void bindTaskTab(TextView tv, boolean selected, boolean enabled) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(14));
        background.setColor(getColor(selected ? R.color.bg_card : android.R.color.transparent));
        background.setStroke(dpToPx(selected ? 1 : 0),
                getColor(selected ? R.color.primary : android.R.color.transparent));

        tv.setBackground(background);
        tv.setTextColor(getColor(selected ? R.color.primary_dark : R.color.text_secondary));
        tv.setEnabled(enabled);
        tv.setAlpha(enabled ? 1f : 0.45f);
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

        // כאן מחלקים את המשימות של הילד שנבחר לשלוש קבוצות פשוטות וברורות
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

        // כאן הפילטר הפעיל קובע אילו קבוצות להציג ברשימה
        switch (activeFilter) {
            case ASSIGNED:
                addTasks(assignedTasks);
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_open, selectedChild.displayName));
                break;
            case COMPLETED:
                addTasks(completedTasks);
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_completed, selectedChild.displayName));
                break;
            case URGENT:
                addTasks(urgentTasks);
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_urgent, selectedChild.displayName));
                break;
            case ALL:
            default:
                addTaskSection(getString(R.string.parent_dashboard_group_urgent), urgentTasks);
                addTaskSection(getString(R.string.parent_dashboard_group_assigned), assignedTasks);
                addTaskSection(getString(R.string.parent_dashboard_group_completed), completedTasks);
                tvNoTasks.setText(getString(R.string.parent_dashboard_no_tasks_all, selectedChild.displayName));
                break;
        }
        taskAdapter.notifyDataSetChanged();
        updateListViewHeight(lvTasks);

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
        addTasks(tasks);
    }

    private void addTasks(List<AssignedTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

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
            taskRef(task).child("dueAt").setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.parent_dashboard_date_updated, Toast.LENGTH_SHORT).show();
                        reloadDashboard();
                    })
                    .addOnFailureListener(e -> showError(e));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDeleteTaskDialog(AssignedTask task) {
        if (task == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_delete_task_title)
                .setMessage(getString(R.string.parent_dashboard_delete_task_message, task.title))
                .setPositiveButton(R.string.parent_dashboard_delete_task, (dialog, which) ->
                        taskRef(task).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, R.string.parent_dashboard_task_deleted, Toast.LENGTH_SHORT).show();
                                    reloadDashboard();
                                })
                                .addOnFailureListener(e -> showError(e)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מחזיר reference לנתיב משימה ספציפית ב-Firebase
    private com.google.firebase.database.DatabaseReference taskRef(AssignedTask task) {
        return FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.childId)
                .child("tasks").child(task.taskId);
    }

    // טוען מחדש את הנתונים אחרי שינוי (מחיקה/עדכון תאריך)
    private void reloadDashboard() {
        FirebaseUser user = getSignedInParentOrRedirect();
        if (user != null) loadDashboardData(user);
    }

    private void showError(Exception e) {
        Toast.makeText(this, getString(R.string.error_with_details, e.getMessage()),
                Toast.LENGTH_SHORT).show();
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

    // מחזיר תיאור סטטוס בעברית — משמש בדיאלוג פרטי משימה
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

    private int dpToPx(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    // מתאים את גובה הרשימה לכמות המשימות, כדי שלא יישאר שטח ריק גדול במסך.
    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int listWidth = listView.getWidth();
        if (listWidth <= 0) {
            listWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        }

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);
        int totalHeight = 0;

        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(adapter.getCount() - 1, 0));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

}
