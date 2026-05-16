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

    private TextView tvTotalTasks, tvNoTasks;
    private LinearLayout tasksContainer;

    private final List<ChildTask> openTasks = new ArrayList<>();
    private String parentId, childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);

        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tasksContainer = findViewById(R.id.tasksContainer);

        loadChildTasks();
    }

    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance().getReference("parents")
                .child(parentId).child("children").child(childId);
    }

    // טוען את המשימות של הילד ומציג רק משימות פתוחות.
    private void loadChildTasks() {
        childRef().child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openTasks.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChildTask task = snap.getValue(ChildTask.class);
                    task.setId(snap.getKey());
                    if (!task.getIsDone()) {
                        openTasks.add(task);
                    }
                }
                tvTotalTasks.setText(String.valueOf(openTasks.size()));
                renderTasks();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void renderTasks() {
        tasksContainer.removeAllViews();
        if (openTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            return;
        }
        tvNoTasks.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < openTasks.size(); i++) {
            ChildTask task = openTasks.get(i);
            View card = inflater.inflate(R.layout.item_child_task, tasksContainer, false);
            bindTaskView(card, task);
            tasksContainer.addView(card);
        }
    }

    private void bindTaskView(View card, final ChildTask task) {
        TextView tvTitle = card.findViewById(R.id.tvTaskTitle);
        TextView tvDue = card.findViewById(R.id.tvDueDate);
        Button btnDone = card.findViewById(R.id.btnDone);

        tvTitle.setText(task.getTitle());
        tvDue.setText(task.getDueAt());

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDoneDialog(task);
            }
        });
    }

    // מציג דיאלוג אישור לפני סימון המשימה כבוצעה.
    private void showDoneDialog(final ChildTask task) {
        new AlertDialog.Builder(this)
                .setTitle("סיום משימה")
                .setMessage("האם אתה בטוח שסיימת את המשימה?")
                .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        processMarkTaskAsDone(task);
                        Toast.makeText(ChildDashboardActivity.this,
                                "סיימת את המשימה, כל הכבוד!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("לא", null)
                .show();
    }

    // מסמן את המשימה כבוצעה: מעדכן ב-Firebase את isDone ל-true.
    private void processMarkTaskAsDone(final ChildTask task) {
        childRef().child("tasks").child(task.getId()).child("isDone").setValue(true);
    }
}
