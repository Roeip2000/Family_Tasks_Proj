package com.example.family_tasks_proj.child;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.Class_child.ChildTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

/**
 * דשבורד הילד — מציג שם, כוכבים, וסיכום משימות.
 *
 * אחריות:
 * - מזהה את הילד מ-Intent extras (אחרי QR) או מ-SharedPreferences (סשן קודם).
 * - טוען כותרת (שם ילד) מ-/parents/{parentId}/children/{childId}.
 * - טוען משימות מ-.../tasks וסופר סה"כ, שבוצעו, ודחופות (עד יומיים).
 *
 * הערה: ה-RecyclerView (rvTasks) עדיין חסר Adapter — כרגע מציג רק סיכומים.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ChildDashboard";

    private static final String PREFS_SESSION = "child_session";
    private static final String EXTRA_PARENT_ID = "parentId";
    private static final String EXTRA_CHILD_ID = "childId";

    private static final String ROOT_PARENTS = "parents";
    private static final String NODE_CHILDREN = "children";
    private static final String NODE_TASKS = "tasks";

    private TextView tvChildName, tvStars, tvTotalTasks, tvCompleted, tvDueSoon;
    private RecyclerView rvTasks;

    private String parentId;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        resolveSession();
        if (isBlank(parentId) || isBlank(childId)) {
            Toast.makeText(this, "Missing QR session. Please scan again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "QR session parentId=" + parentId + ", childId=" + childId);

        loadChildHeader();
        loadTasksSummary();
    }

    /**
     * קובע parentId + childId: קודם מ-Intent, אם חסר — מ-SharedPreferences.
     * מאפשר לילד לחזור לדשבורד בלי לסרוק QR מחדש.
     */
    private void resolveSession() {
        Intent i = getIntent();
        if (i != null) {
            parentId = i.getStringExtra(EXTRA_PARENT_ID);
            childId = i.getStringExtra(EXTRA_CHILD_ID);
        }
        if (isBlank(parentId) || isBlank(childId)) {
            SharedPreferences sp = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
            parentId = sp.getString(EXTRA_PARENT_ID, null);
            childId = sp.getString(EXTRA_CHILD_ID, null);
        }
    }

    /** מחזיר reference ל-/parents/{parentId}/children/{childId}. */
    private DatabaseReference childRef() {
        return FirebaseDatabase.getInstance()
                .getReference(ROOT_PARENTS)
                .child(parentId)
                .child(NODE_CHILDREN)
                .child(childId);
    }

    /** טוען שם הילד מ-Firebase ומציג ב-tvChildName. */
    private void loadChildHeader() {
        String path = ROOT_PARENTS + "/" + parentId + "/" + NODE_CHILDREN + "/" + childId;
        Log.d(TAG, "Reading child from: " + path);

        childRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);

                String full = (firstName != null ? firstName : "");
                if (lastName != null && !lastName.trim().isEmpty()) full = full + " " + lastName;
                tvChildName.setText(full.trim().isEmpty() ? "Child" : full.trim());

                // TODO: כרגע אין שדה stars ב-DB — ברירת מחדל 0
                tvStars.setText("0");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** טוען משימות מ-Firebase וסופר total / done / dueSoon. */
    private void loadTasksSummary() {
        DatabaseReference tasksRef = childRef().child(NODE_TASKS);
        String path = ROOT_PARENTS + "/" + parentId + "/" + NODE_CHILDREN + "/" + childId + "/" + NODE_TASKS;
        Log.d(TAG, "Reading tasks from: " + path);

        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0, done = 0, dueSoon = 0;

                for (DataSnapshot s : snapshot.getChildren()) {
                    ChildTask t = s.getValue(ChildTask.class);
                    if (t == null) continue;
                    total++;
                    if (t.isDone) done++;
                    if (!t.isDone && isDueSoon(t.dueAt)) dueSoon++;
                }

                tvTotalTasks.setText(String.valueOf(total));
                tvCompleted.setText(String.valueOf(done));
                tvDueSoon.setText(String.valueOf(dueSoon));

                Log.d(TAG, "Tasks loaded count=" + total);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * בודק אם תאריך יעד הוא בתוך 0–2 ימים מהיום.
     *
     * @param dueAt תאריך בפורמט "d/M/yyyy"
     * @return true אם המשימה דחופה
     */
    private boolean isDueSoon(String dueAt) {
        if (isBlank(dueAt)) return false;
        String[] p = dueAt.trim().split("/");
        if (p.length != 3) return false;

        try {
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);

            Calendar due = Calendar.getInstance();
            due.set(y, m - 1, d, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);

            long diffDays = (due.getTimeInMillis() - now.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
            return diffDays >= 0 && diffDays <= 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
