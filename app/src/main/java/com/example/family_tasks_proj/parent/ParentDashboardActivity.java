package com.example.family_tasks_proj.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.parent.adapter.ParentDashboardTaskAdapter;
import com.example.family_tasks_proj.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש (UI Elements)
    private Button btnManageChildren, btnManageTemplates, btnAssignTask, btnQR;
    private TextView tvParentGreeting, tvOpenTasksCount, tvDoneTasksCount, tvUrgentTasksCount, tvOverdueTasksCount, tvNoTasks, tvTaskSectionTitle;
    private RecyclerView rvTasks;

    // רשימה שתכיל רק את המשימות הפתוחות (שטרם בוצעו) להצגה במסך
    private final List<AssignedTask> openTasks = new ArrayList<>();

    // מתאם (Adapter) לקישור בין רשימת המשימות ל-RecyclerView
    private ParentDashboardTaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור רכיבי הדשבורד מה-XML לקוד (Binding)
        tvParentGreeting = findViewById(R.id.tvParentName);
        tvParentGreeting.setText(R.string.parent_greeting);
        tvOpenTasksCount = findViewById(R.id.tvParentTotalTasks);
        tvDoneTasksCount = findViewById(R.id.tvParentCompleted);
        tvUrgentTasksCount = findViewById(R.id.tvParentDueSoon);
        tvOverdueTasksCount = findViewById(R.id.tvParentOverdue);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        tvTaskSectionTitle = findViewById(R.id.tvTaskSectionTitle);
        rvTasks = findViewById(R.id.rvTasks);

        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnAssignTask = findViewById(R.id.btnAssignTaskToChild);
        btnQR = findViewById(R.id.btnShowQR);

        // קביעת כותרת למקטע המשימות הפתוחות
        tvTaskSectionTitle.setText(R.string.parent_open_tasks_title);

        // כפתור למעבר למסך ניהול הילדים (הוספה ועריכה של ילדים)
        btnManageChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
            }
        });

        // כפתור למעבר למסך ניהול תבניות (יצירת משימות קבועות מראש)
        btnManageTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
            }
        });

        // כפתור למעבר למסך שיוך משימה (בחירת ילד ושליחת משימה אליו)
        btnAssignTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
            }
        });

        // כפתור למעבר למסך הפקת קוד QR (מאפשר לילד להתחבר לאפליקציה על ידי סריקה)
        btnQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
            }
        });

        // הגדרת ה-RecyclerView: קביעת פריסה אנכית וחיבור המתאם (Adapter)
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new ParentDashboardTaskAdapter(this, openTasks);
        rvTasks.setAdapter(taskAdapter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // בדיקה האם יש משתמש (הורה) מחובר כעת
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null)
        {
            // אם המשתמש מחובר, נטען את הנתונים שלו מ-Firebase
            loadData(user);
        }
        else
        {
            // אם אף אחד לא מחובר, נחזיר את המשתמש למסך הראשי/התחברות
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * פונקציה הטוענת את כל נתוני הילדים והמשימות שלהם מה-Firebase
     * ומעדכנת את רכיבי המסך בהתאם.
     */
    private void loadData(FirebaseUser user)
    {
        // איפוס הרשימה והסתרת הודעת ה-"אין משימות" לפני טעינה חדשה
        openTasks.clear();
        tvNoTasks.setVisibility(View.GONE);
        taskAdapter.notifyDataSetChanged();

        // הפניה לנתיב ב-Firebase שבו שמורים הילדים של ההורה הנוכחי
        DatabaseReference childrenReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("children");

        // האזנה לשינויים בנתונים (קריאה חד פעמית של כל ענף הילדים)
        childrenReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                openTasks.clear();

                // משתנים לספירת סטטיסטיקות עבור כרטיסי הסיכום בראש המסך
                int openTasksCount = 0;
                int doneTasksCount = 0;
                int urgentTasksCount = 0;
                int overdueTasksCount = 0;

                // מעבר על כל הילדים שנמצאו בנתיב
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    String childName = childSnapshot.child("firstName").getValue(String.class);
                    DataSnapshot tasksSnapshot = childSnapshot.child("tasks");

                    // מעבר על כל המשימות של כל ילד
                    for (DataSnapshot taskSnapshot : tasksSnapshot.getChildren())
                    {
                        AssignedTask task = new AssignedTask();

                        task.setChildName(childName);
                        task.setTitle(taskSnapshot.child("title").getValue(String.class));
                        task.setDueAt(taskSnapshot.child("dueAt").getValue(String.class));

                        // קריאת תמונת המשימה (בפורמט Base64) להצגה בדשבורד
                        task.setImageBase64(taskSnapshot.child("imageBase64").getValue(String.class));
                        
                        Boolean isDone = taskSnapshot.child("isDone").getValue(Boolean.class);
                        task.setIsDone(isDone != null && isDone);

                        // בדיקה האם המשימה בוצעה או שהיא עדיין פתוחה
                        if (task.getIsDone())
                        {
                            doneTasksCount++;
                        }
                        else
                        {
                            openTasksCount++;
                            openTasks.add(task); // הוספה לרשימת התצוגה רק אם המשימה פתוחה

                            // בדיקה האם המשימה באיחור או דחופה (לפי תאריך היעד)
                            if (DateUtils.isOverdue(task.getDueAt()))
                            {
                                overdueTasksCount++;
                            }
                            else if (DateUtils.isDueSoon(task.getDueAt()))
                            {
                                urgentTasksCount++;
                            }
                        }
                    }
                }

                // עדכון מספרי הסיכום בטקסטים שבראש המסך
                tvOpenTasksCount.setText(String.valueOf(openTasksCount));
                tvDoneTasksCount.setText(String.valueOf(doneTasksCount));
                tvUrgentTasksCount.setText(String.valueOf(urgentTasksCount));
                tvOverdueTasksCount.setText(String.valueOf(overdueTasksCount));

                // אם אין משימות פתוחות בכלל, נציג הודעה מתאימה למשתמש
                if (openTasks.isEmpty())
                {
                    tvNoTasks.setVisibility(View.VISIBLE);
                }
                else
                {
                    tvNoTasks.setVisibility(View.GONE);
                }

                // עדכון המתאם שהנתונים השתנו כדי שירענן את ה-RecyclerView
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                // טיפול במקרה של שגיאה בגישה ל-Firebase (כרגע ללא שינוי במסך)
            }
        });
    }
}
