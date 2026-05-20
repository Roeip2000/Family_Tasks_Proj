package com.example.family_tasks_proj.child;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.models.ChildTask;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChildDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "parentId";
    public static final String EXTRA_CHILD_ID = "childId";

    private TextView tvOpenTasksCount, tvNoTasks;
    private LinearLayout tasksContainer;

    private final List<ChildTask> openTasks = new ArrayList<>();
    private String parentId, childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // קבלת מזהים מהמסך הקודם
        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);

        tvOpenTasksCount = findViewById(R.id.tvTotalTasks);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tasksContainer = findViewById(R.id.tasksContainer);

        loadChildTasks();
    }

    // מחזיר את הנתיב לילד ב-Firebase
    private DatabaseReference getChildReference() {
        return FirebaseDatabase.getInstance().getReference("parents")
                .child(parentId).child("children").child(childId);
    }

    // טעינת משימות הילד מ-Firebase
    private void loadChildTasks() {
        getChildReference().child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openTasks.clear();

                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    ChildTask task = taskSnapshot.getValue(ChildTask.class);
                    if (task == null) {
                        continue;
                    }
                    task.setId(taskSnapshot.getKey());

                    // שמירת משימות פתוחות בלבד
                    if (!task.getIsDone()) {
                        openTasks.add(task);
                    }
                }
                tvOpenTasksCount.setText(String.valueOf(openTasks.size()));
                renderTasks();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // הצגת המשימות על המסך
    private void renderTasks() {
        tasksContainer.removeAllViews();
        if (openTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            return;
        }
        tvNoTasks.setVisibility(View.GONE);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ChildTask task : openTasks) {
            View card = inflater.inflate(R.layout.item_child_task, tasksContainer, false);
            bindTaskView(card, task);
            tasksContainer.addView(card);
        }
    }

    // מילוי נתוני משימה בכרטיס
    private void bindTaskView(View card, final ChildTask task) {
        TextView tvTitle = card.findViewById(R.id.tvTaskTitle);
        TextView tvDueDate = card.findViewById(R.id.tvDueDate);
        TextView tvStatus = card.findViewById(R.id.tvStatus);
        Button btnDone = card.findViewById(R.id.btnDone);

        tvTitle.setText(task.getTitle());
        tvDueDate.setText(task.getDueAt());

        // קביעת סטטוס המשימה (באיחור, דחוף וכו')
        if (DateUtils.isOverdue(task.getDueAt())) {
            tvStatus.setText(getString(R.string.parent_dashboard_task_status_late));
            tvStatus.setTextColor(getColor(R.color.danger));
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            tvStatus.setText(getString(R.string.parent_dashboard_task_status_urgent));
            tvStatus.setTextColor(getColor(R.color.urgent));
        } else {
            tvStatus.setText(getString(R.string.parent_dashboard_task_status_waiting));
            tvStatus.setTextColor(getColor(R.color.text_secondary));
        }

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDoneDialog(task);
            }
        });
    }

    // הצגת דיאלוג אישור לפני סיום משימה
    private void showDoneDialog(final ChildTask task) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_task_done_title)
                .setMessage(R.string.dialog_task_done_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        markTaskAsDone(task);
                        Toast.makeText(ChildDashboardActivity.this, R.string.toast_task_done, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    // עדכון המשימה כבוצעה ב-Firebase
    private void markTaskAsDone(final ChildTask task) {
        getChildReference().child("tasks").child(task.getId()).child("isDone").setValue(true);
    }
}
