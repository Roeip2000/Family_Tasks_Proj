package com.example.family_tasks_proj.child;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.models.ChildTask;
import com.example.family_tasks_proj.utils.DateUtils;
import com.example.family_tasks_proj.utils.ImageHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
        
        if (parentId == null || childId == null) {
            returnToMainAfterMissingChild();
            return;
        }

        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tasksContainer = findViewById(R.id.tasksContainer);
        
        loadChildProfile();
        loadChildTasks();
    }

    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance().getReference("parents")
                .child(parentId).child("children").child(childId);
    }

    // מאזין שבודק שהילד עדיין קיים ב-Firebase. אם הילד נמחק - חוזרים למסך הראשי.
    private void loadChildProfile() {
        childRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    returnToMainAfterMissingChild();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // טוען את המשימות של הילד ומסנן רק את המשימות הפתוחות
    private void loadChildTasks() {
        childRef().child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                openTasks.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        ChildTask task = snap.getValue(ChildTask.class);
                        if (task == null) {
                            continue;
                        }
                        task.setId(snap.getKey());
                        if (!task.getIsDone()) {
                            openTasks.add(task);
                        }
                    }
                }
                tvTotalTasks.setText(String.valueOf(openTasks.size()));
                renderTasks();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // בונה את רשימת המשימות בקוד עם LayoutInflater במקום להשתמש ב-RecyclerView
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

    private static final int DAYS_IN_WEEK = 7;

    private void bindTaskView(View card, final ChildTask task) {
        TextView tvTitle = card.findViewById(R.id.tvTaskTitle);
        TextView tvDue = card.findViewById(R.id.tvDueDate);
        Button btnDone = card.findViewById(R.id.btnDone);
        View dotView = card.findViewById(R.id.viewStatusDot);
        View imgShell = card.findViewById(R.id.imgTaskImageShell);
        ImageView imgTask = card.findViewById(R.id.imgTaskImage);

        tvTitle.setText(task.getTitle());

        long days = DateUtils.daysLeft(task.getDueAt());
        boolean overdue = DateUtils.isOverdue(task.getDueAt());
        boolean dueSoon = DateUtils.isDueSoon(task.getDueAt());

        String dueText;
        // אם DateUtils מחזיר NO_VALID_DATE, אין תאריך תקין להצגה
        if (days == DateUtils.NO_VALID_DATE) {
            dueText = getString(R.string.child_due_no_date);
        } else if (overdue) {
            int absoluteDays;
            if (days < 0) {
                absoluteDays = (int)(-days);
            } else {
                absoluteDays = (int)days;
            }
            dueText = getString(R.string.child_due_late, absoluteDays);
        } else if (days == 0) {
            dueText = getString(R.string.child_due_today);
        } else if (days == 1) {
            dueText = getString(R.string.child_due_tomorrow);
        } else if (days <= DAYS_IN_WEEK) {
            dueText = getString(R.string.child_due_days_left, (int) days);
        } else {
            dueText = task.getDueAt();
        }
        tvDue.setText(dueText);

        int dueColor;
        int dotColor;
        if (overdue) {
            dueColor = R.color.danger;
            dotColor = R.color.danger;
        } else if (dueSoon) {
            dueColor = R.color.urgent;
            dotColor = R.color.urgent;
        } else {
            dueColor = R.color.text_secondary;
            dotColor = R.color.text_hint;
        }
        tvDue.setTextColor(getColor(dueColor));

        // יוצר עיגול צבעוני קטן שמראה את מצב התאריך
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(getColor(dotColor));
        dotView.setBackground(dot);

        String imageBase64 = task.getImageBase64();
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);
            if (bitmap != null) {
                imgShell.setVisibility(View.VISIBLE);
                imgTask.setImageBitmap(bitmap);
            } else {
                imgShell.setVisibility(View.GONE);
            }
        } else {
            imgShell.setVisibility(View.GONE);
        }

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processMarkTaskAsDone(task);
            }
        });
    }

    // מעדכן את הסטטוס ב-Firebase ל-isDone=true כשילד מסיים משימה
    private void processMarkTaskAsDone(final ChildTask task) {
        if (task.getIsDone()) {
            return;
        }
        task.setIsDone(true);

        childRef().child("tasks").child(task.getId()).child("isDone").setValue(true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ChildDashboardActivity.this, R.string.child_task_mark_done_success, Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        task.setIsDone(false);
                        Toast.makeText(ChildDashboardActivity.this, "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void returnToMainAfterMissingChild() {
        Toast.makeText(this, R.string.error_child_missing, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
