package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.child.Class_child.ChildTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * דשבורד הילד — מציג שם, כוכבים, סיכום משימות, ורשימת משימות מפורטת.
 *
 * אחריות:
 * - מזהה את הילד מ-Intent extras (אחרי בחירה) או מ-SharedPreferences (סשן קודם).
 * - טוען כותרת (שם ילד) מ-/parents/{parentId}/children/{childId}.
 * - טוען משימות מ-.../tasks — סופר סה"כ, שבוצעו, ודחופות (עד 2 ימים).
 * - מחשב סכום כוכבים ממשימות שבוצעו (isDone == true).
 * - מציג רשימת משימות ב-RecyclerView עם כרטיסים (ChildTaskAdapter).
 * - כפתור יציאה — מנקה סשן ומחזיר למסך הראשי עם AlertDialog אישור.
 *
 * Layout: activity_child_dashboard.xml
 *
 * ===== ניווט =====
 * כניסה מ: ChildSelectionActivity (אחרי בחירת הורה + ילד)
 *
 * ===== נתיבי Firebase =====
 * קריאה מ: /parents/{parentId}/children/{childId} — שם הילד
 * קריאה מ: /parents/{parentId}/children/{childId}/tasks/ — רשימת משימות
 *
 * ===== הערות =====
 * TODO: להוסיף AlarmManager + Notification — התרעה לילד כשמשימה מתקרבת לתאריך יעד או באיחור.
 * TODO: להוסיף כפתור "בוצע" בכל כרטיס משימה שמעדכן isDone ב-Firebase.
 */
public class ChildDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ChildDashboard";

    // מפתחות לזיהוי הסשן
    private static final String PREFS_SESSION = "child_session";
    private static final String EXTRA_PARENT_ID = "parentId";
    private static final String EXTRA_CHILD_ID = "childId";

    // נתיבי Firebase
    private static final String ROOT_PARENTS = "parents";
    private static final String NODE_CHILDREN = "children";
    private static final String NODE_TASKS = "tasks";

    // --- Views ---
    private TextView tvChildName, tvStars, tvTotalTasks, tvCompleted, tvDueSoon;
    private TextView tvNoTasks;
    private RecyclerView rvTasks;
    private Button btnLogout;

    // --- Data ---
    private String parentId;
    private String childId;
    /** רשימת המשימות — מתמלאת מ-Firebase ומוצגת ב-RecyclerView */
    private final List<ChildTask> taskList = new ArrayList<>();
    /** אדפטר שמציג את כרטיסי המשימות */
    private ChildTaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // חיבור views מה-layout
        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        rvTasks = findViewById(R.id.rvTasks);
        btnLogout = findViewById(R.id.btnLogout);

        // הגדרת RecyclerView עם Adapter
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(taskList);
        rvTasks.setAdapter(adapter);

        // כפתור יציאה — מציג AlertDialog אישור לפני מחיקת סשן
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // זיהוי הילד — מ-Intent (אחרי בחירה) או מ-SharedPreferences (סשן קודם)
        resolveSession();
        if (isBlank(parentId) || isBlank(childId)) {
            Toast.makeText(this, "חסר סשן. חזור למסך בחירת ילד.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Session: parentId=" + parentId + ", childId=" + childId);

        // טעינת נתונים מ-Firebase
        loadChildHeader();
        loadTasks();
    }

    /**
     * קובע parentId + childId: קודם מ-Intent, אם חסר — מ-SharedPreferences.
     * מאפשר לילד לחזור לדשבורד בלי לבחור שוב.
     */
    private void resolveSession() {
        Intent i = getIntent();
        if (i != null) {
            parentId = i.getStringExtra(EXTRA_PARENT_ID);
            childId = i.getStringExtra(EXTRA_CHILD_ID);
        }
        // fallback ל-SharedPreferences אם ה-Intent ריק
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

    /**
     * טוען שם הילד מ-Firebase ומציג ב-tvChildName.
     * הכוכבים מחושבים ב-loadTasks (מסכום starsWorth של משימות שבוצעו).
     */
    private void loadChildHeader() {
        String path = ROOT_PARENTS + "/" + parentId + "/" + NODE_CHILDREN + "/" + childId;
        Log.d(TAG, "Reading child from: " + path);

        childRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);

                // בניית שם מלא
                String full = (firstName != null ? firstName : "");
                if (lastName != null && !lastName.trim().isEmpty()) full = full + " " + lastName;
                String displayName = full.trim().isEmpty() ? "ילד" : full.trim();

                tvChildName.setText("שלום " + displayName + "!");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load child header: " + error.getMessage());
                Toast.makeText(ChildDashboardActivity.this,
                        "שגיאה בטעינת שם: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * טוען משימות מ-Firebase, מעדכן סיכומים, כוכבים, ורשימה.
     *
     * מחשב:
     * - total: סה"כ משימות
     * - done: משימות שבוצעו (isDone == true)
     * - dueSoon: משימות דחופות (0-2 ימים, עדיין לא בוצעו)
     * - stars: סכום starsWorth של משימות שבוצעו
     *
     * ממלא את ה-RecyclerView ברשימת המשימות דרך ChildTaskAdapter.
     */
    private void loadTasks() {
        DatabaseReference tasksRef = childRef().child(NODE_TASKS);
        String path = ROOT_PARENTS + "/" + parentId + "/" + NODE_CHILDREN + "/" + childId + "/" + NODE_TASKS;
        Log.d(TAG, "Reading tasks from: " + path);

        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0, done = 0, dueSoon = 0;
                long stars = 0;
                taskList.clear();

                // עוברים על כל המשימות ובונים רשימה + סיכומים
                for (DataSnapshot s : snapshot.getChildren()) {
                    ChildTask t = s.getValue(ChildTask.class);
                    if (t == null) continue;

                    // מוודא שה-id קיים (Firebase key)
                    if (t.id == null || t.id.isEmpty()) {
                        t.id = s.getKey();
                    }

                    taskList.add(t);
                    total++;

                    if (t.isDone) {
                        done++;
                        stars += t.starsWorth; // סכום כוכבים ממשימות שבוצעו
                    }

                    if (!t.isDone && isDueSoon(t.dueAt)) {
                        dueSoon++;
                    }
                }

                // עדכון סיכומים
                tvTotalTasks.setText(String.valueOf(total));
                tvCompleted.setText(String.valueOf(done));
                tvDueSoon.setText(String.valueOf(dueSoon));
                tvStars.setText(stars + " ⭐");

                // הצגת/הסתרת הודעת "אין משימות"
                if (taskList.isEmpty()) {
                    tvNoTasks.setVisibility(View.VISIBLE);
                    rvTasks.setVisibility(View.GONE);
                } else {
                    tvNoTasks.setVisibility(View.GONE);
                    rvTasks.setVisibility(View.VISIBLE);
                }

                // עדכון ה-Adapter
                adapter.notifyDataSetChanged();

                Log.d(TAG, "Tasks loaded: total=" + total + " done=" + done
                        + " dueSoon=" + dueSoon + " stars=" + stars);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tasks: " + error.getMessage());
                Toast.makeText(ChildDashboardActivity.this,
                        "שגיאה בטעינת משימות: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * מציג AlertDialog לאישור יציאה.
     * אם המשתמש מאשר — מנקה סשן מ-SharedPreferences ומחזיר למסך הראשי.
     * אם מבטל — לא קורה כלום.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("יציאה")
                .setMessage("האם אתה בטוח שברצונך לצאת?")
                .setPositiveButton("כן, צא", (dialog, which) -> {
                    // מחיקת סשן מ-SharedPreferences — בפעם הבאה יצטרך לבחור שוב
                    SharedPreferences sp = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
                    sp.edit().clear().apply();

                    // חזרה למסך הראשי — מנקה את כל ה-Activity stack
                    Intent intent = new Intent(ChildDashboardActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * בודק אם תאריך יעד הוא בתוך 0–2 ימים מהיום (דחוף).
     *
     * @param dueAt תאריך בפורמט "d/M/yyyy"
     * @return true אם המשימה דחופה (0-2 ימים מהיום)
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
            due.set(y, m - 1, d, 0, 0, 0); // חודשים ב-Calendar מתחילים מ-0
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

    /** בודק אם מחרוזת ריקה או null. */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
