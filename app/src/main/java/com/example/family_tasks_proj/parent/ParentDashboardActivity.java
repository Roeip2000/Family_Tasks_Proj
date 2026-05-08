package com.example.family_tasks_proj.parent;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// דשבורד ניהול להורה - מציג את כל המשימות של כל הילדים
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR, btnLogout;
    private TextView tvName, tvTotal, tvDone, tvUrgent, tvOverdue, tvNoTasks, tvFilterTitle;
    private View fOpen, fUrgent, fOverdue, fDone;
    private RecyclerView rvTasks;

    private final List<AssignedTask> allTasks = new ArrayList<>();
    private final List<AssignedTask> visibleTasks = new ArrayList<>();

    private ParentDashboardTaskAdapter taskAdapter;
    private FilterMode activeFilter = FilterMode.ALL;

    private enum FilterMode { ALL, ASSIGNED, COMPLETED, URGENT, OVERDUE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);
        initViews();
        setupActions();
        setupLists();
        setupFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null)
        {
            loadProfile(user);
            loadData(user);
        }
        else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

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
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupFilters() {
        View.OnClickListener click = new View.OnClickListener() {
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
        fOpen.setOnClickListener(click);
        fUrgent.setOnClickListener(click);
        fOverdue.setOnClickListener(click);
        fDone.setOnClickListener(click);
    }

    private void setFilter(FilterMode mode) {
        if (activeFilter == mode) {
            activeFilter = FilterMode.ALL;
        } else {
            activeFilter = mode;
        }
        updateFilterUI();
        refreshTaskList();
    }

    private void updateFilterUI() {
        // צובע את הפילטר הפעיל ברקע אפור שקוף ומאפס את השאר
        tintFilter(fOpen, activeFilter == FilterMode.ASSIGNED);
        tintFilter(fUrgent, activeFilter == FilterMode.URGENT);
        tintFilter(fOverdue, activeFilter == FilterMode.OVERDUE);
        tintFilter(fDone, activeFilter == FilterMode.COMPLETED);

        switch (activeFilter)
        {
            case ASSIGNED:
                tvFilterTitle.setText(R.string.parent_filter_open);
                break;
            case URGENT:
                tvFilterTitle.setText(R.string.parent_filter_urgent);
                break;
            case OVERDUE:
                tvFilterTitle.setText(R.string.parent_filter_overdue);
                break;
            case COMPLETED:
                tvFilterTitle.setText(R.string.parent_filter_completed);
                break;
            default:
                tvFilterTitle.setText(R.string.parent_filter_all);
                break;
        }
    }

    // עוזר לצביעת רקע של כפתור פילטר: אפור שקוף כשפעיל, שקוף לחלוטין אחרת
    private void tintFilter(View view, boolean active) {
        if (active) {
            view.setBackgroundColor(getColor(R.color.filter_selected_overlay));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

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
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
    }

    private void setupLists() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, visibleTasks);
        taskAdapter.setShowChildName(true);
        taskAdapter.setOnItemClickListener(new ParentDashboardTaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AssignedTask task, int position) {
                showTaskOptions(position);
            }
        });
        rvTasks.setAdapter(taskAdapter);
    }

    private void loadProfile(FirebaseUser user) {
        // טעינת שם ההורה נפרדת מטעינת המשימות, כי זה מידע פרופיל ולא מידע של רשימת המשימות.
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("firstName").getValue(String.class);
                String displayName;
                if (name != null) {
                    displayName = name;
                } else {
                    displayName = getString(R.string.default_parent_name);
                }
                tvName.setText(getString(R.string.parent_greeting, displayName));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadData(FirebaseUser user) {
        // הדשבורד של ההורה עובר על כל הילדים וכל המשימות שלהם.
        // הסיכומים מחושבים מהמשימות עצמן, ולא נשמרים כמונים נפרדים ב-Firebase.
        FirebaseDatabase.getInstance().getReference("parents").child(user.getUid()).child("children")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                int totalCount = 0, doneCount = 0, urgentCount = 0, lateCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot cSnap : snapshot.getChildren()) {

                        String cId = cSnap.getKey();
                        String cName = cSnap.child("firstName").getValue(String.class);
                        
                        for (DataSnapshot tSnap : cSnap.child("tasks").getChildren()) {
                            // כל משימה מה-Firebase הופכת לאובייקט Java פשוט להצגה ברשימה.
                            AssignedTask task = new AssignedTask();
                            task.setChildId(cId);
                            if (cName != null) {
                                task.setChildName(cName);
                            } else {
                                task.setChildName(getString(R.string.default_child_name_fallback));
                            }
                            task.setTaskId(tSnap.getKey());
                            task.setTitle(tSnap.child("title").getValue(String.class));
                            task.setDueAt(tSnap.child("dueAt").getValue(String.class));
                            task.setImageBase64(tSnap.child("imageBase64").getValue(String.class));
                            Boolean isDone = tSnap.child("isDone").getValue(Boolean.class);
                            task.setIsDone(isDone != null && isDone);

                            allTasks.add(task);
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
                
                tvTotal.setText(String.valueOf(totalCount));
                tvDone.setText(String.valueOf(doneCount));
                tvUrgent.setText(String.valueOf(urgentCount));
                tvOverdue.setText(String.valueOf(lateCount));

                refreshTaskList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void refreshTaskList() {
        visibleTasks.clear();
        for (AssignedTask task : allTasks) {
            // הפילטר קובע איזו משימה תוצג, בלי למחוק או לשנות את הנתונים המקוריים.
            boolean match = false;
            switch (activeFilter) {
                case ASSIGNED:
                    match = !task.getIsDone();
                    break;
                case COMPLETED:
                    match = task.getIsDone();
                    break;
                case URGENT:
                    match = !task.getIsDone() && DateUtils.isDueSoon(task.getDueAt());
                    break;
                case OVERDUE:
                    match = !task.getIsDone() && DateUtils.isOverdue(task.getDueAt());
                    break;
                case ALL:
                default:
                    match = true;
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

    private void showTaskOptions(int pos) {
        if (pos < 0 || pos >= visibleTasks.size()) {
            return;
        }
        final AssignedTask task = visibleTasks.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(task.getTitle())
                .setMessage(getString(R.string.parent_task_dialog_message, task.getChildName(), task.getDueAt()))
                .setNeutralButton(R.string.dialog_delete_task, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        deleteTask(task);
                    }
                }).setNegativeButton(R.string.dialog_close, null);

        if (!task.getIsDone()) {
            builder.setPositiveButton(R.string.dialog_option_edit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    showEditTaskDialog(task);
                }
            });
        }
        builder.show();
    }

    private void showEditTaskDialog(final AssignedTask task) {
        final EditText etTitle = new EditText(this);
        etTitle.setHint(R.string.hint_task_name);
        etTitle.setSingleLine(true);
        etTitle.setText(task.getTitle());

        final EditText etDueDate = new EditText(this);
        etDueDate.setHint(R.string.hint_select_date);
        etDueDate.setSingleLine(true);
        etDueDate.setFocusable(false);
        etDueDate.setInputType(InputType.TYPE_NULL);
        etDueDate.setText(task.getDueAt());
        etDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEditDatePicker(etDueDate);
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, 8, padding, 0);
        layout.addView(etTitle);
        layout.addView(etDueDate);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_task_title)
                .setView(layout)
                .setPositiveButton(R.string.dialog_save_task, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newTitle = etTitle.getText().toString().trim();
                        String newDueDate = etDueDate.getText().toString().trim();
                        if (newTitle.isEmpty() || newDueDate.isEmpty()) {
                            Toast.makeText(ParentDashboardActivity.this, R.string.error_task_missing_details, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateTask(task, newTitle, newDueDate);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private void openEditDatePicker(final EditText etDueDate) {
        Calendar calendar = getCalendarForDate(etDueDate.getText().toString());
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                etDueDate.setText(getString(R.string.date_slash_format, day, month + 1, year));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private Calendar getCalendarForDate(String date) {
        Calendar calendar = Calendar.getInstance();
        if (date == null) {
            return calendar;
        }

        String[] parts = date.split("/");
        if (parts.length != 3) {
            return calendar;
        }

        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);
            calendar.set(year, month, day);
        } catch (NumberFormatException ignored) {
        }
        return calendar;
    }

    private void updateTask(AssignedTask task, String title, String dueAt) {
        DatabaseReference taskReference = FirebaseDatabase.getInstance().getReference("parents")
                .child(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.getChildId()).child("tasks").child(task.getTaskId());

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", title);
        taskData.put("dueAt", dueAt);

        // מעדכנים רק את שדות המשימה שנערכו, בלי לגעת בסטטוס, כוכבים או תמונה.
        taskReference.updateChildren(taskData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ParentDashboardActivity.this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTask(AssignedTask task) {
        FirebaseDatabase.getInstance().getReference("parents").child(FirebaseAuth.getInstance().getUid())
                .child("children").child(task.getChildId()).child("tasks").child(task.getTaskId()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ParentDashboardActivity.this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.dialog_logout_title).setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_SKIP_AUTO_LOGIN, true);
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
    }
}
