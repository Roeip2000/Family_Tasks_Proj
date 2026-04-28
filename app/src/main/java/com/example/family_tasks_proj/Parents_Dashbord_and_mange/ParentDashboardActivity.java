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

// === מסך: דשבורד הורה ===
// תפקיד: מציג ילדים, סיכום בית ומשימות, ומאפשר סינון וניהול
// מחלקות קשורות: ParentDashboardTaskAdapter, ParentDashboardChildSummaryAdapter
// Firebase path: parents/{uid}/children/{childId}/tasks
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

    // ספירות סיכום הבית — מעודכנות בזמן parseDashboardData
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

    // טוען מחדש את הדשבורד בכל חזרה למסך כדי שהורה יראה נתונים עדכניים
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
        filterOverdueTasks = findViewById(R.id.filterOverdueTasks);
        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnShowQR = findViewById(R.id.btnShowQR);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void bindActions() {
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openScreen(ManageChildrenActivity.class); }
        });
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openScreen(ParentTaskTemplateActivity.class); }
        });
        btnAssignTaskToChild.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openScreen(AssignTaskToChildActivity.class); }
        });
        btnShowQR.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openScreen(GenerateQRActivity.class); }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showLogoutDialog(); }
        });
    }

    // פותח מסך פעולה של ההורה בלי לשנות את הדשבורד הנוכחי
    private void openScreen(Class<?> target) {
        if (target == null) return;
        startActivity(new Intent(this, target));
    }

    private void setupChildrenList() {
        rvChildren.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        childSummaryAdapter = new ParentDashboardChildSummaryAdapter(
                this, childSummaries, childPhotoCache, new ParentDashboardChildSummaryAdapter.OnChildSelectedListener() {
            @Override
            public void onChildSelected(String childId) {
                selectChild(childId);
            }
        });
        rvChildren.setAdapter(childSummaryAdapter);
    }

    private void setupTaskList() {
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTaskItems);
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> adapterView, View view, int position, long id) {
                showTaskOptionsDialog(position);
            }
        });
    }

    // לחיצה על צ'יפ משנה את סוג המשימות שמוצג ברשימה
    private void setupTaskFilters() {
        filterAllTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.ALL);
            }
        });
        filterOpenTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.ASSIGNED);
            }
        });
        filterCompletedTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.COMPLETED);
            }
        });
        filterUrgentTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.URGENT);
            }
        });
        filterOverdueTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setActiveFilter(FilterMode.OVERDUE);
            }
        });
        updateTaskFilterSelectionUi();
    }

    private void setActiveFilter(FilterMode mode) {
        if (mode == activeFilter) {
            return;
        }
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
        DatabaseReference profileRef = parentRef(user.getUid());
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה"));
                // Base64 = קידוד שהופך תמונה למחרוזת טקסט כדי לשמור ב-Firebase
                String base64 = snapshot.child("profileImageBase64").getValue(String.class);

                Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
                if (bitmap == null) {
                    ivParentProfile.setImageResource(R.drawable.ic_avatar_placeholder);
                } else {
                    ivParentProfile.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // תחנה 1: טעינת כל הילדים והמשימות מ-Firebase
    private void loadDashboardData(@NonNull FirebaseUser user) {
        DatabaseReference childrenRef = parentRef(user.getUid()).child("children");
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        parseDashboardData(snapshot);
                        refreshDashboardUi();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                getString(R.string.error_loading_data, error.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // תחנה 2: פיענוח הנתונים בעזרת לולאה על ילדים ולולאה על המשימות שלהם
    private void parseDashboardData(DataSnapshot snapshot) {
        allAssignedTasks.clear();
        childSummaries.clear();
        childPhotoCache.clear();
        houseAssigned = houseDone = houseUrgent = houseOverdue = houseTotal = 0;

        for (DataSnapshot childSnap : snapshot.getChildren()) {
            String childId = childSnap.getKey();
            if (childId == null || childId.trim().isEmpty()) {
                continue;
            }

            ChildSummary summary = new ChildSummary();
            summary.childId = childId;
            summary.displayName = NameUtils.fullNameOrDefault(
                    childSnap.child("firstName").getValue(String.class),
                    childSnap.child("lastName").getValue(String.class), "ילד");
            summary.childProfileBase64 = childSnap.child("profileImageBase64").getValue(String.class);

            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                AssignedTask task = parseTask(taskSnap, summary);
                if (task == null) {
                    continue;
                }
                allAssignedTasks.add(task);
                countTaskInto(task, summary);
            }
            childSummaries.add(summary);
        }

        houseTotal = allAssignedTasks.size();

        // הוספת אפשרות "כל הילדים" לתפריט הבחירה
        if (!childSummaries.isEmpty()) {
            ChildSummary all = new ChildSummary();
            all.childId = ParentDashboardChildSummaryAdapter.ALL_CHILDREN_ID;
            all.displayName = getString(R.string.parent_dashboard_all_children);
            all.totalCount = houseTotal;
            all.assignedCount = houseAssigned;
            all.completedCount = houseDone;
            all.urgentCount = houseUrgent;
            all.overdueCount = houseOverdue;
            childSummaries.add(0, all);
        }
    }

    private AssignedTask parseTask(DataSnapshot snap, ChildSummary ownerSummary) {
        String taskId = snap.getKey();
        if (taskId == null || taskId.trim().isEmpty()) {
            return null;
        }

        AssignedTask task = new AssignedTask();
        task.childId = ownerSummary.childId;
        task.childName = ownerSummary.displayName;
        task.childProfileBase64 = ownerSummary.childProfileBase64;
        task.taskId = taskId;
        task.title = safeText(snap.child("title").getValue(String.class));
        task.dueAt = safeText(snap.child("dueAt").getValue(String.class));
        Boolean isDone = snap.child("isDone").getValue(Boolean.class);
        task.isDone = isDone != null && isDone;
        return task;
    }

    private void countTaskInto(AssignedTask task, ChildSummary summary) {
        summary.totalCount++;
        if (task.isDone) {
            summary.completedCount++;
            houseDone++;
            return;
        }
        summary.assignedCount++;
        houseAssigned++;
        if (DateUtils.isOverdue(task.dueAt)) {
            summary.overdueCount++;
            houseOverdue++;
        } else if (DateUtils.isDueSoon(task.dueAt)) {
            summary.urgentCount++;
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

    // אם אין ילד נבחר תקף, בוחרים את הראשון ברשימה
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

    private void updateTaskFilterSelectionUi() {
        boolean enabled = getSelectedChildSummary() != null;
        bindTaskTab(filterOpenTasks, activeFilter == FilterMode.ASSIGNED, enabled);
        bindTaskTab(filterCompletedTasks, activeFilter == FilterMode.COMPLETED, enabled);
        bindTaskTab(filterUrgentTasks, activeFilter == FilterMode.URGENT, enabled);
        bindTaskTab(filterOverdueTasks, activeFilter == FilterMode.OVERDUE, enabled);
    }

    // הטאב הפעיל נצבע במלא כדי שיהיה ברור מה נבחר; הלא-נבחר שקוף
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
            tvTaskSectionTitle.setText(R.string.parent_dashboard_selected_child_empty_title);
            tvTaskSectionSubtitle.setText(R.string.parent_dashboard_selected_child_empty_subtitle);
            return;
        }
        // בתצוגת "כל הילדים" הכותרת קבועה, אחרת מציגים את שם הילד הנבחר
        if (ParentDashboardChildSummaryAdapter.ALL_CHILDREN_ID.equals(summary.childId)) {
            tvTaskSectionTitle.setText(R.string.parent_dashboard_all_children_title);
        } else {
            tvTaskSectionTitle.setText(
                    getString(R.string.parent_dashboard_selected_child_title, summary.displayName));
        }
        tvTaskSectionSubtitle.setText(
                getString(R.string.parent_dashboard_selected_child_stats,
                        summary.assignedCount, summary.completedCount,
                        summary.urgentCount, summary.overdueCount));
    }

    // בונה את רשימת המשימות של הילד הנבחר לפי הפילטר הפעיל
    private void buildSelectedChildTaskList() {
        visibleTaskItems.clear();
        ChildSummary selectedChild = getSelectedChildSummary();
        if (selectedChild == null) {
            tvNoTasks.setText(R.string.parent_dashboard_selected_child_empty_subtitle);
            showTaskListVisibility(false);
            return;
        }

        boolean isAll = ParentDashboardChildSummaryAdapter
                .ALL_CHILDREN_ID.equals(selectedChild.childId);
        // במצב "כל הילדים" כל שורת משימה מציגה לאיזה ילד היא שייכת
        taskAdapter.setShowChildName(isAll);

        // מצב כל הילדים לא משנה Firebase; הוא רק מסנן את הרשימה שכבר נטענה
        // חלוקה לקבוצות כדי שהילד/הסטטוס יהיה קל לקריאה
        List<AssignedTask> overdue = new ArrayList<>();
        List<AssignedTask> urgent = new ArrayList<>();
        List<AssignedTask> open = new ArrayList<>();
        List<AssignedTask> completed = new ArrayList<>();
        for (AssignedTask task : allAssignedTasks) {
            if (task == null) {
                continue;
            }
            if (!isAll && !selectedChild.childId.equals(task.childId)) {
                continue;
            }
            addTaskToCorrectGroup(task, overdue, urgent, open, completed);
        }

        int emptyTextRes = populateByActiveFilter(overdue, urgent, open, completed, isAll);
        // ההודעה הריקה בתצוגת "כל הילדים" גנרית, ובתצוגת ילד יחיד עם שמו
        if (isAll) {
            tvNoTasks.setText(getString(emptyTextRes));
        } else {
            tvNoTasks.setText(getString(emptyTextRes, selectedChild.displayName));
        }
        taskAdapter.notifyDataSetChanged();
        updateListViewHeight(lvTasks);
        showTaskListVisibility(!visibleTaskItems.isEmpty());
    }

    // מכניס משימה לקבוצה אחת לפי סטטוס ותאריך
    private void addTaskToCorrectGroup(AssignedTask task,
                                       List<AssignedTask> overdue,
                                       List<AssignedTask> urgent,
                                       List<AssignedTask> open,
                                       List<AssignedTask> completed) {
        if (task.isDone) {
            completed.add(task);
        } else if (DateUtils.isOverdue(task.dueAt)) {
            overdue.add(task);
        } else if (DateUtils.isDueSoon(task.dueAt)) {
            urgent.add(task);
        } else {
            open.add(task);
        }
    }

    // ממלא את visibleTaskItems לפי הפילטר, ומחזיר את מזהה הטקסט במקרה שהתוצאה ריקה
    private int populateByActiveFilter(List<AssignedTask> overdue,
                                       List<AssignedTask> urgent,
                                       List<AssignedTask> open,
                                       List<AssignedTask> completed,
                                       boolean isAll) {
        switch (activeFilter) {
            case ASSIGNED:
                addTasksWithHeader(null, open);
                if (isAll) {
                    return R.string.parent_dashboard_all_no_tasks_open;
                }
                return R.string.parent_dashboard_no_tasks_open;
            case COMPLETED:
                addTasksWithHeader(null, completed);
                if (isAll) {
                    return R.string.parent_dashboard_all_no_tasks_completed;
                }
                return R.string.parent_dashboard_no_tasks_completed;
            case URGENT:
                addTasksWithHeader(null, urgent);
                if (isAll) {
                    return R.string.parent_dashboard_all_no_tasks_urgent;
                }
                return R.string.parent_dashboard_no_tasks_urgent;
            case OVERDUE:
                addTasksWithHeader(null, overdue);
                if (isAll) {
                    return R.string.parent_dashboard_all_no_tasks_overdue;
                }
                return R.string.parent_dashboard_no_tasks_overdue;
            case ALL:
            default:
                addTasksWithHeader(getString(R.string.parent_dashboard_group_overdue), overdue);
                addTasksWithHeader(getString(R.string.parent_dashboard_group_urgent), urgent);
                addTasksWithHeader(getString(R.string.parent_dashboard_group_assigned), open);
                addTasksWithHeader(getString(R.string.parent_dashboard_group_completed), completed);
                if (isAll) {
                    return R.string.parent_dashboard_all_no_tasks_all;
                }
                return R.string.parent_dashboard_no_tasks_all;
        }
    }

    // מוסיף כותרת קבוצה (אם ניתנה) ואז את כל המשימות שבקבוצה
    private void addTasksWithHeader(String header, List<AssignedTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        if (header != null) {
            visibleTaskItems.add(TaskListItem.createHeader(header, tasks.size()));
        }
        for (AssignedTask task : tasks) {
            visibleTaskItems.add(TaskListItem.createTask(task));
        }
    }

    private void showTaskListVisibility(boolean hasTasks) {
        tvNoTasks.setVisibility(hasTasks ? View.GONE : View.VISIBLE);
        lvTasks.setVisibility(hasTasks ? View.VISIBLE : View.GONE);
    }

    private ChildSummary getSelectedChildSummary() {
        if (selectedChildId == null) {
            return null;
        }
        for (ChildSummary summary : childSummaries) {
            if (summary.childId.equals(selectedChildId)) {
                return summary;
            }
        }
        return null;
    }

    private void selectChild(String childId) {
        if (childId == null || childId.equals(selectedChildId)) {
            return;
        }
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
        if (task == null) {
            return;
        }

        String details = getString(R.string.parent_dashboard_task_details,
                safeText(task.title), safeText(task.childName),
                safeText(task.dueAt), getTaskStatusLabel(task));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_task_details_title);

        if (task.isDone) {
            // משימה שהושלמה — לקריאה בלבד, בלי עריכה
            builder.setMessage(details + "\n\n"
                            + getString(R.string.parent_dashboard_task_completed_readonly))
                    .setNegativeButton(R.string.parent_dashboard_close, null);
        } else {
            builder.setMessage(details)
                    .setNeutralButton(R.string.parent_dashboard_delete_task,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showDeleteTaskDialog(task);
                                }
                            })
                    .setNegativeButton(R.string.parent_dashboard_close, null);
        }
        builder.show();
    }

    private void showDeleteTaskDialog(AssignedTask task) {
        if (task == null || task.isDone) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_delete_task_title)
                .setMessage(getString(R.string.parent_dashboard_delete_task_message, task.title))
                .setPositiveButton(R.string.parent_dashboard_delete_task, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTaskFromFirebase(task);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מוחק משימה מ-Firebase: /parents/{uid}/children/{childId}/tasks/{taskId}
    private void deleteTaskFromFirebase(AssignedTask task) {
        DatabaseReference deleteRef = taskRef(task);
        Task<Void> deleteTask = deleteRef.removeValue();
        writeAndReload(deleteTask, R.string.parent_dashboard_task_deleted);
    }

    // פעולה מול Firebase + הצגת Toast + טעינה מחדש של הדשבורד
    private void writeAndReload(Task<Void> op, int successMessageRes) {
        op.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentDashboardActivity.this, successMessageRes, Toast.LENGTH_SHORT).show();
                reloadDashboard();
            }
        });
        op.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ParentDashboardActivity.this,
                        getString(R.string.error_with_details, exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private DatabaseReference parentRef(String uid) {
        // מפנה ל-FirebaseDatabase.getInstance() — נקודת הגישה ל-DB
        return FirebaseDatabase.getInstance().getReference("parents").child(uid);
    }

    private DatabaseReference taskRef(AssignedTask task) {
        // עריכת תאריך/מחיקה נוגעת רק במשימה הספציפית של הילד
        return parentRef(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.childId)
                .child("tasks").child(task.taskId);
    }

    private void reloadDashboard() {
        FirebaseUser user = getSignedInParentOrRedirect();
        if (user != null) {
            loadDashboardData(user);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.parent_dashboard_logout_title)
                .setMessage(R.string.parent_dashboard_logout_message)
                .setPositiveButton(R.string.parent_dashboard_logout_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logoutParent();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // מנתק את ההורה מ-FirebaseAuth וחוזר למסך הראשי
    private void logoutParent() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // סטטוס בעברית למסך פרטי המשימה
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
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    // מתודת עזר — מחשבת גובה ל-ListView בתוך ScrollView
    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }
        int listWidth;
        if (listView.getWidth() > 0) {
            listWidth = listView.getWidth();
        } else {
            listWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        }
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                listWidth,
                View.MeasureSpec.AT_MOST);
        int totalHeight = listView.getDividerHeight() * Math.max(adapter.getCount() - 1, 0);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
