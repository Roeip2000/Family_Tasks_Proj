package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.util.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * דשבורד ראשי של ההורה — מסך Hub.
 *
 * זהו המסך המרכזי אחרי התחברות ההורה.
 * מציג כפתורי ניווט:
 * 1. ניהול ילדים — הוספת ילדים חדשים (ManageChildrenActivity)
 * 2. מאגר תבניות — יצירת תבניות משימה עם תמונה (ParentTaskTemplateActivity)
 * 3. הקצאת משימה — שליחת משימה מהמאגר לילד ספציפי (AssignTaskToChildActivity)
 * 4. QR לילדים — הצגת קוד QR לכל ילד, הילד סורק כדי להתחבר (GenerateQRActivity)
 * 5. התנתקות — מנתק מ-FirebaseAuth עם AlertDialog אישור ומחזיר ל-MainActivity
 *
 * Layout: activity_parent_dashboard.xml
 *
 * ===== הערות =====
 * TODO: להוסיף AlarmManager + Notification — התרעה להורה כשמשימה של ילד באיחור.
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR, btnLogout;
    private TextView tvParentTotalTasks, tvParentCompleted, tvParentDueSoon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור TextViews של סיכום משימות
        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);

        // חיבור כפתורים — כל כפתור פותח Activity ייעודי
        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageChildren.setOnClickListener(v ->
        {
            // ניהול ילדים — הוספת ילד חדש
            startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
        });

        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnManageTemplates.setOnClickListener(v ->
        {
            // מאגר תבניות — יצירת תבנית משימה חדשה עם תמונה
            startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
        });

        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnAssignTaskToChild.setOnClickListener(v ->
        {
            // הקצאת משימה — בחירת תבנית + ילד + תאריך
            startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
        });

        btnShowQR = findViewById(R.id.btnShowQR);
        btnShowQR.setOnClickListener(v ->
        {
            // QR לילדים — Spinner לבחירת ילד + הצגת QR
            startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
        });

        // כפתור התנתקות — מציג AlertDialog אישור לפני ניתוק
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // טוען סיכום כל פעם שחוזרים למסך — כך אחרי הקצאת משימה המספרים מתעדכנים
        loadParentStats();
    }

    /**
     * טוען סיכום משימות מכל הילדים של ההורה.
     *
     * סורק /parents/{uid}/children/ — לכל ילד עובר על tasks/ וסופר:
     * - total: סה"כ משימות מכל הילדים
     * - done: משימות שבוצעו (isDone == true)
     * - dueSoon: משימות דחופות (0-2 ימים, לא בוצעו)
     *
     * משתמש ב-DateUtils.isDueSoon() — אותה לוגיקה כמו ChildDashboardActivity.
     */
    private void loadParentStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int total = 0, done = 0, dueSoon = 0;

                        // עוברים על כל הילדים ועל המשימות של כל ילד
                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                                total++;
                                Boolean isDone = taskSnap.child("isDone").getValue(Boolean.class);
                                if (isDone != null && isDone) {
                                    done++;
                                } else {
                                    String dueAt = taskSnap.child("dueAt").getValue(String.class);
                                    if (DateUtils.isDueSoon(dueAt)) dueSoon++;
                                }
                            }
                        }

                        // עדכון TextViews בסיכום
                        tvParentTotalTasks.setText("משימות: " + total);
                        tvParentCompleted.setText("הושלמו: " + done);
                        tvParentDueSoon.setText("ד��ופות: " + dueSoon);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                "שגיאה בטעינת סיכום: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מציג AlertDialog לאישור התנתקות.
     * אם המשתמש מאשר — מנתק מ-FirebaseAuth ומחזיר למסך הראשי (MainActivity).
     * אם מבטל — לא קורה כלום.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("כן, התנתק", (dialog, which) -> {
                    // ניתוק מ-Firebase Auth
                    FirebaseAuth.getInstance().signOut();

                    // חזרה למסך הראשי — מנקה את כל ה-Activity stack
                    Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
