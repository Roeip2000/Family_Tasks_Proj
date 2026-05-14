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

/**
 * דשבורד הילד. מציג את המשימות הפתוחות של הילד ומאפשר לסמן משימה כבוצעה.
 * המסך פשוט במכוון: רשימה אחת של משימות פתוחות, בלי פילטרים.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "parentId";
    public static final String EXTRA_CHILD_ID = "childId";

    // משתני ממשק
    private TextView tvChildName, tvTotalTasks, tvNoTasks;
    private LinearLayout tasksContainer;

    // משתני נתונים
    private final List<ChildTask> openTasks = new ArrayList<>();
    private String parentId, childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // שלב א: קבלת מזהי ההורה והילד דרך Intent extras
        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (parentId == null || childId == null) {
            returnToMainAfterMissingChild();
            return;
        }

        // שלב ב: אתחול תצוגה וטעינת נתונים מ-Firebase
        initViews();
        loadChildProfile();
        loadChildTasks();
    }

    private void initViews() {
        tvChildName = findViewById(R.id.tvChildName);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvNoTasks = findViewById(R.id.tvNoTasksChild);
        tasksContainer = findViewById(R.id.tasksContainer);
    }

    // ההפניה הבסיסית של הילד ב-Firebase: parents/{parentId}/children/{childId}
    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance().getReference("parents")
                .child(parentId).child("children").child(childId);
    }

    // טעינת פרטי הילד: בודקים שהילד עדיין קיים ב-Firebase ומציגים ברכה
    private void loadChildProfile() {
        childRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    returnToMainAfterMissingChild();
                    return;
                }
                tvChildName.setText(R.string.child_greeting);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // טעינת המשימות של הילד מ-Firebase ועדכון המסך בכל שינוי
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
                        // הילד רואה רק משימות פתוחות. אחרי סימון "בוצע" המשימה נעלמת מהמסך.
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
                Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // לולאה פשוטה: עבור כל משימה פתוחה יוצרים כרטיס מתוך item_child_task.xml ומוסיפים למסך
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

    // ממלא את שדות הכרטיס בנתוני המשימה ומחבר את כפתור "בוצע"
    private void bindTaskView(View card, final ChildTask task) {
        TextView tvTitle = card.findViewById(R.id.tvTaskTitle);
        TextView tvDue = card.findViewById(R.id.tvDueDate);
        Button btnDone = card.findViewById(R.id.btnDone);
        View dotView = card.findViewById(R.id.viewStatusDot);
        View imgShell = card.findViewById(R.id.imgTaskImageShell);
        ImageView imgTask = card.findViewById(R.id.imgTaskImage);

        tvTitle.setText(task.getTitle());

        // טקסט תאריך בעברית לפי המצב: היום, מחר, עוד X ימים, באיחור, או ללא תאריך
        long days = DateUtils.daysLeft(task.getDueAt());
        boolean overdue = DateUtils.isOverdue(task.getDueAt());
        boolean dueSoon = DateUtils.isDueSoon(task.getDueAt());

        String dueText;
        if (days == Long.MAX_VALUE) {
            dueText = getString(R.string.child_due_no_date);
        } else if (overdue) {
            dueText = getString(R.string.child_due_late, (int) Math.abs(days));
        } else if (days == 0) {
            dueText = getString(R.string.child_due_today);
        } else if (days == 1) {
            dueText = getString(R.string.child_due_tomorrow);
        } else if (days <= 7) {
            dueText = getString(R.string.child_due_days_left, (int) days);
        } else {
            dueText = task.getDueAt();
        }
        tvDue.setText(dueText);

        // צבע טקסט וצבע נקודת סטטוס לפי דחיפות המשימה
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

        // יצירת נקודת סטטוס עגולה צבעונית בקוד (במקום XML נפרד לכל צבע)
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(getColor(dotColor));
        dotView.setBackground(dot);

        // הצגת תמונת המשימה אם קיימת (התמונה נשמרה ב-Firebase כמחרוזת Base64)
        String imageBase64 = task.getImageBase64();
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);
            if (bitmap != null) {
                imgShell.setVisibility(View.VISIBLE);
                imgTask.setVisibility(View.VISIBLE);
                imgTask.setImageBitmap(bitmap);
            } else {
                imgShell.setVisibility(View.GONE);
                imgTask.setVisibility(View.GONE);
            }
        } else {
            imgShell.setVisibility(View.GONE);
            imgTask.setVisibility(View.GONE);
        }

        // לחיצה על "בוצע" - מסמנת את המשימה ככבוצעה ב-Firebase
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processMarkTaskAsDone(task);
            }
        });
    }

    // סימון משימה כבוצעה: כתיבת isDone=true ל-Firebase. המאזין יטען מחדש את הרשימה.
    private void processMarkTaskAsDone(final ChildTask task) {
        // הגנה מפני לחיצה כפולה: אם המשימה כבר סומנה - לא נכתוב שוב
        if (task.getIsDone()) {
            return;
        }
        // סימון מקומי מיד כדי שלחיצה נוספת תיחסם. אם הכתיבה נכשלת - מחזירים את הסטטוס.
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
                        Toast.makeText(ChildDashboardActivity.this, getString(R.string.error_with_details, exception.getMessage()), Toast.LENGTH_SHORT).show();
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
