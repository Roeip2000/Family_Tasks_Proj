package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** מסך ראשי להורה. מציג סיכום של משימות הבית, רשימת ילדים וניהול משימות. */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR, btnLogout;
    private TextView tvParentName, tvParentTotalTasks, tvParentCompleted, tvParentDueSoon;
    private TextView tvNoTasks, tvTaskSectionTitle, tvTaskSectionSubtitle, tvNoChildren;
    private TextView filterAllTasks, filterOpenTasks, filterCompletedTasks, filterUrgentTasks, filterOverdueTasks;
    private ImageView ivParentProfile;
    private ListView lvTasks;
    private RecyclerView rvChildren;

    private final List<AssignedTask> allAssignedTasks = new ArrayList<>();
    private final List<TaskListItem> visibleTaskItems = new ArrayList<>();
    private final List<ChildSummary> childSummaries = new ArrayList<>();
    private final Map<String, Bitmap> childPhotoCache = new HashMap<>();

    private int houseAssigned, houseDone, houseUrgent, houseOverdue, houseTotal;
    private ParentDashboardTaskAdapter taskAdapter;
    private ParentDashboardChildSummaryAdapter childSummaryAdapter;
    private String selectedChildId;
    private FilterMode activeFilter = FilterMode.ASSIGNED;

    private enum FilterMode { ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE }

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
        if (user != null) {
            loadParentProfile(user);
            loadDashboardData(user);
        }
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
        filterOverdueTasks = findViewById(R.id.filterOverdueTasks);
        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnShowQR = findViewById(R.id.btnShowQR);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void bindActions() {
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScreen(ManageChildrenActivity.class); }
        });
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScreen(ParentTaskTemplateActivity.class); }
        });
        btnAssignTaskToChild.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScreen(AssignTaskToChildActivity.class); }
        });
        btnShowQR.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScreen(GenerateQRActivity.class); }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showLogoutDialog(); }
        });
    }

    private void openScreen(Class<?> target) {
        if (target != null) startActivity(new Intent(this, target));
    }

    // הגדרת רשימת הילדים האופקית
    private void setupChildrenList() {
        rvChildren.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(this, childSummaries, childPhotoCache, new ParentDashboardChildSummaryAdapter.OnChildSelectedListener() {
            @Override public void onChildSelected(String childId) { selectChild(childId); }
        });
        rvChildren.setAdapter(childSummaryAdapter);
    }

    // הגדרת רשימת המשימות המרכזית
    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showTaskOptionsDialog(position);
            }
        });
    }

    private void setupTaskFilters() {
        filterAllTasks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.ALL); } });
        filterOpenTasks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.ASSIGNED); } });
        filterCompletedTasks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.COMPLETED); } });
        filterUrgentTasks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.URGENT); } });
        filterOverdueTasks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setActiveFilter(FilterMode.OVERDUE); } });
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
        if (user != null) return user;
        Toast.makeText(this, "הסשן פג, אנא התחבר מחדש", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return null;
    }

    // טוען את פרטי הפרופיל של ההורה
    private void loadParentProfile(@NonNull FirebaseUser user) {
        parentRef(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה"));
                String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
                if (bitmap == null) ivParentProfile.setImageResource(R.drawable.ic_avatar_placeholder);
                else ivParentProfile.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // טוען את כל נתוני הילדים והמשימות
    private void loadDashboardData(@NonNull FirebaseUser user) {
        parentRef(user.getUid()).child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                parseDashboardData(snapshot);
                refreshDashboardUi();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentDashboardActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מפרק את הנתונים מהשרת לאובייקטים מקומיים
    private void parseDashboardData(DataSnapshot snapshot) {
        allAssignedTasks.clear();
        childSummaries.clear();
        childPhotoCache.clear();
        houseAssigned = houseDone = houseUrgent = houseOverdue = houseTotal = 0;

        for (DataSnapshot childSnap : snapshot.getChildren()) {
            String childId = childSnap.getKey();
            if (childId == null || childId.trim().isEmpty()) continue;

            ChildSummary summary = new ChildSummary();
            summary.setChildId(childId);
            summary.setDisplayName(NameUtils.fullNameOrDefault(childSnap.child("firstName").getValue(String.class), childSnap.child("lastName").getValue(String.class), "ילד"));
            summary.setChildProfileBase64(childSnap.child("profileImageBase64").getValue(String.class));

            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                AssignedTask task = parseTask(taskSnap, summary);
                if (task == null) continue;
                allAssignedTasks.add(task);
                countTaskInto(task, summary);
            }
            childSummaries.add(summary);
        }
        houseTotal = allAssignedTasks.size();
        if (!childSummaries.isEmpty()) {
            ChildSummary all = new ChildSummary();
            all.setChildId(ParentDashboardChildSummaryAdapter.ALL_CHILDREN_ID);
            all.setDisplayName("כל הילדים");
            all.setTotalCount(houseTotal);
            all.setAssignedCount(houseAssigned);
            all.setCompletedCount(houseDone);
            all.setUrgentCount(houseUrgent);
            all.setOverdueCount(houseOverdue);
            childSummaries.add(0, all);
        }
    }

    private AssignedTask parseTask(DataSnapshot snap, ChildSummary ownerSummary) {
        String taskId = snap.getKey();
        if (taskId == null || taskId.trim().isEmpty()) return null;
        AssignedTask task = new AssignedTask();
        task.setChildId(ownerSummary.getChildId());
        task.setChildName(ownerSummary.getDisplayName());
        task.setChildProfileBase64(ownerSummary.getChildProfileBase64());
        task.setTaskId(taskId);
        task.setTitle(safeText(snap.child("title").getValue(String.class)));
        task.setDueAt(safeText(snap.child("dueAt").getValue(String.class)));
        Boolean isDone = snap.child("isDone").getValue(Boolean.class);
        task.setIsDone(isDone != null && isDone);
        return task;
    }

    // סופר משימות לסיכום הכללי ולסיכום של כל ילד
    private void countTaskInto(AssignedTask task, ChildSummary summary) {
        summary.setTotalCount(summary.getTotalCount() + 1);
        if (task.getIsDone()) {
            summary.setCompletedCount(summary.getCompletedCount() + 1);
            houseDone++;
            return;
        }
        summary.setAssignedCount(summary.getAssignedCount() + 1);
        houseAssigned++;
        if (DateUtils.isOverdue(task.getDueAt())) {
            summary.setOverdueCount(summary.getOverdueCount() + 1);
            houseOverdue++;
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            summary.setUrgentCount(summary.getUrgentCount() + 1);
            houseUrgent++;
        }
    }

    private void refreshDashboardUi() {
        ensureSelectedChild();
        childSummaryAdapter.setSelectedChildId(selectedChildId);
        childSummaryAdapter.notifyDataSetChanged();
        boolean hasChildren = !childSummaries.isEmpty();
        rvChildren.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
        tvNoChildren.setVisibility(hasChildren ? View.GONE : View.VISIBLE);
        tvParentTotalTasks.setText(String.valueOf(houseAssigned));
        tvParentCompleted.setText(String.valueOf(houseDone));
        tvParentDueSoon.setText(String.valueOf(houseUrgent));
        updateTaskFilterSelectionUi();
        updateSelectedChildSection();
        buildSelectedChildTaskList();
    }

    private void ensureSelectedChild() {
        if (childSummaries.isEmpty()) { selectedChildId = null; return; }
        for (ChildSummary summary : childSummaries) { if (summary.getChildId().equals(selectedChildId)) return; }
        selectedChildId = childSummaries.get(0).getChildId();
    }

    private void updateTaskFilterSelectionUi() {
        boolean enabled = getSelectedChildSummary() != null;
        bindTaskTab(filterAllTasks, activeFilter == FilterMode.ALL, enabled);
        bindTaskTab(filterOpenTasks, activeFilter == FilterMode.ASSIGNED, enabled);
        bindTaskTab(filterCompletedTasks, activeFilter == FilterMode.COMPLETED, enabled);
        bindTaskTab(filterUrgentTasks, activeFilter == FilterMode.URGENT, enabled);
        bindTaskTab(filterOverdueTasks, activeFilter == FilterMode.OVERDUE, enabled);
    }

    private void bindTaskTab(TextView tv, boolean selected, boolean enabled) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(getColor(selected ? R.color.primary : android.R.color.transparent));
        tv.setBackground(bg);
        tv.setTextColor(getColor(selected ? R.color.white : R.color.text_secondary));
        tv.setEnabled(enabled);
        tv.setAlpha(enabled ? 1f : 0.45f);
    }

    private void updateSelectedChildSection() {
        ChildSummary summary = getSelectedChildSummary();
        if (summary == null) {
            tvTaskSectionTitle.setText("אין ילדים רשומים");
            tvTaskSectionSubtitle.setText("הוסף ילד כדי להתחיל");
            return;
        }
        if (ParentDashboardChildSummaryAdapter.ALL_CHILDREN_ID.equals(summary.getChildId())) {
            tvTaskSectionTitle.setText("כל המשימות בבית");
        } else {
            tvTaskSectionTitle.setText("משימות של " + summary.getDisplayName());
        }
        tvTaskSectionSubtitle.setText(String.format("פתוחות: %d | בוצעו: %d | דחופות: %d | באיחור: %d", summary.getAssignedCount(), summary.getCompletedCount(), summary.getUrgentCount(), summary.getOverdueCount()));
    }

    // בונה את רשימת המשימות לתצוגה לפי הילד והפילטר שנבחרו
    private void buildSelectedChildTaskList() {
        visibleTaskItems.clear();
        ChildSummary selectedChild = getSelectedChildSummary();
        if (selectedChild == null) { showTaskListVisibility(false); return; }

        boolean isAll = ParentDashboardChildSummaryAdapter.ALL_CHILDREN_ID.equals(selectedChild.getChildId());
        taskAdapter.setShowChildName(isAll);

        List<AssignedTask> overdue = new ArrayList<>(), urgent = new ArrayList<>(), open = new ArrayList<>(), completed = new ArrayList<>();
        for (AssignedTask task : allAssignedTasks) {
            if (!isAll && !selectedChild.getChildId().equals(task.getChildId())) continue;
            if (task.getIsDone()) completed.add(task);
            else if (DateUtils.isOverdue(task.getDueAt())) overdue.add(task);
            else if (DateUtils.isDueSoon(task.getDueAt())) urgent.add(task);
            else open.add(task);
        }

        populateByActiveFilter(overdue, urgent, open, completed);
        taskAdapter.notifyDataSetChanged();
        updateListViewHeight(lvTasks);
        showTaskListVisibility(!visibleTaskItems.isEmpty());
    }

    private void populateByActiveFilter(List<AssignedTask> overdue, List<AssignedTask> urgent, List<AssignedTask> open, List<AssignedTask> completed) {
        switch (activeFilter) {
            case ASSIGNED: addTasksWithHeader(null, open); tvNoTasks.setText("אין משימות פתוחות"); break;
            case COMPLETED: addTasksWithHeader(null, completed); tvNoTasks.setText("אין משימות שבוצעו"); break;
            case URGENT: addTasksWithHeader(null, urgent); tvNoTasks.setText("אין משימות דחופות"); break;
            case OVERDUE: addTasksWithHeader(null, overdue); tvNoTasks.setText("אין משימות באיחור"); break;
            case ALL:
            default:
                addTasksWithHeader("באיחור", overdue); addTasksWithHeader("דחוף", urgent);
                addTasksWithHeader("פתוח", open); addTasksWithHeader("בוצע", completed);
                tvNoTasks.setText("אין משימות להצגה"); break;
        }
    }

    private void addTasksWithHeader(String header, List<AssignedTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        if (header != null) visibleTaskItems.add(TaskListItem.createHeader(header, tasks.size()));
        for (AssignedTask task : tasks) visibleTaskItems.add(TaskListItem.createTask(task));
    }

    private void showTaskListVisibility(boolean hasTasks) {
        tvNoTasks.setVisibility(hasTasks ? View.GONE : View.VISIBLE);
        lvTasks.setVisibility(hasTasks ? View.VISIBLE : View.GONE);
    }

    private ChildSummary getSelectedChildSummary() {
        if (selectedChildId == null) return null;
        for (ChildSummary s : childSummaries) { if (s.getChildId().equals(selectedChildId)) return s; }
        return null;
    }

    private void selectChild(String childId) {
        if (childId == null || childId.equals(selectedChildId)) return;
        selectedChildId = childId;
        childSummaryAdapter.setSelectedChildId(selectedChildId);
        childSummaryAdapter.notifyDataSetChanged();
        updateSelectedChildSection();
        updateTaskFilterSelectionUi();
        buildSelectedChildTaskList();
    }

    // פותח דיאלוג עם פרטי המשימה ואפשרות מחיקה
    private void showTaskOptionsDialog(int position) {
        if (position < 0 || position >= visibleTaskItems.size()) return;
        TaskListItem item = visibleTaskItems.get(position);
        if (item.getIsHeader()) return;
        final AssignedTask task = item.getTask();

        String details = String.format("משימה: %s\nילד: %s\nתאריך: %s\nסטטוס: %s", task.getTitle(), task.getChildName(), task.getDueAt(), getTaskStatusLabel(task));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("פרטי משימה");

        if (task.getIsDone()) {
            builder.setMessage(details + "\n\nמשימה שבוצעה לא ניתן לערוך.").setNegativeButton("סגור", null);
        } else {
            builder.setMessage(details).setNeutralButton("מחק משימה", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { showDeleteTaskDialog(task); }
            }).setNegativeButton("סגור", null);
        }
        builder.show();
    }

    private void showDeleteTaskDialog(final AssignedTask task) {
        new AlertDialog.Builder(this).setTitle("מחיקת משימה").setMessage("האם למחוק את המשימה?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { deleteTaskFromFirebase(task); }
                }).setNegativeButton("ביטול", null).show();
    }

    private void deleteTaskFromFirebase(AssignedTask task) {
        parentRef(FirebaseAuth.getInstance().getUid()).child("children").child(task.getChildId()).child("tasks").child(task.getTaskId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override public void onSuccess(Void unused) {
                Toast.makeText(ParentDashboardActivity.this, "המשימה נמחקה", Toast.LENGTH_SHORT).show();
                loadDashboardData(FirebaseAuth.getInstance().getCurrentUser());
            }
        });
    }

    private DatabaseReference parentRef(String uid) {
        return FirebaseDatabase.getInstance().getReference("parents").child(uid);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("התנתק", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { logoutParent(); }
                }).setNegativeButton("ביטול", null).show();
    }

    private void logoutParent() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getTaskStatusLabel(AssignedTask task) {
        if (task.getIsDone()) return "בוצע";
        long daysLeft = DateUtils.daysLeft(task.getDueAt());
        if (daysLeft < 0) return "באיחור";
        if (DateUtils.isDueSoon(task.getDueAt())) return "דחוף";
        return "ממתין";
    }

    private int dpToPx(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    private String safeText(String text) { return text == null ? "" : text.trim(); }

    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) return;
        int listWidth = listView.getWidth() > 0 ? listView.getWidth() : getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(0, adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
