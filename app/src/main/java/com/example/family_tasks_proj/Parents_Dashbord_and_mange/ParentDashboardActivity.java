package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.app.DatePickerDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.family_tasks_proj.util.NameUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
 * בנוסף, מציג רשימת כל המשימות שהוקצו (מכל הילדים).
 * לחיצה על משימה → פרטים + אפשרות לשנות תאריך יעד או למחוק.
 *
 * Layout: activity_parent_dashboard.xml
 *
 * ===== הערות =====
 * TODO: להוסיף AlarmManager + Notification — התרעה להורה כשמשימה של ילד באיחור.
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR, btnLogout;
    private TextView tvParentTotalTasks, tvParentCompleted, tvParentDueSoon;
    private ListView lvTasks;
    private TextView tvNoTasks;

    // פרופיל ההורה — תמונה + שם
    private ImageView ivParentProfile;
    private TextView tvParentName;

    /**
     * בוחר תמונה מהגלריה לפרופיל ההורה.
     * אותו דפוס בדיוק כמו בParentTaskTemplateActivity.
     */
    private final ActivityResultLauncher<String> profileImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                // טוען ומתקן תמונה — EXIF + הקטנה — דרך ImageHelper שכבר קיים
                Bitmap bitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (bitmap == null) {
                    Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                    return;
                }
                // מציג מיד בלי לחכות ל-Firebase
                ivParentProfile.setImageBitmap(bitmap);
                // שומר ב-Firebase תחת /parents/{uid}/profileImageBase64
                saveProfileImage(bitmap);
            });

    /** כל המשימות מכל הילדים — לתצוגה ברשימה */
    private final List<AssignedTask> assignedTasks = new ArrayList<>();
    /** שורות טקסט להצגה ב-ListView */
    private final List<String> taskDisplayList = new ArrayList<>();
    private ArrayAdapter<String> taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור אלמנטי פרופיל ההורה
        ivParentProfile = findViewById(R.id.ivParentProfile);
        tvParentName = findViewById(R.id.tvParentName);
        Button btnChangeProfilePic = findViewById(R.id.btnChangeProfilePic);
        // לחיצה → פותח גלריה לבחירת תמונה (אותו מנגנון כמו בתבניות)
        btnChangeProfilePic.setOnClickListener(v -> profileImagePicker.launch("image/*"));

        // חיבור TextViews של סיכום משימות
        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);

        // חיבור רשימת משימות
        lvTasks = findViewById(R.id.lvTasks);
        tvNoTasks = findViewById(R.id.tvNoTasks);

        taskAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, taskDisplayList);
        lvTasks.setAdapter(taskAdapter);

        // לחיצה על משימה ברשימה → פרטים + שינוי תאריך / מחיקה
        lvTasks.setOnItemClickListener((parent, view, position, id) ->
                showTaskOptionsDialog(position));

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
        loadDashboardData();
        // טוען שם ותמונת פרופיל של ההורה
        loadParentProfile();
    }

    /**
     * טוען את שם ותמונת הפרופיל של ההורה מ-Firebase.
     * נתיב: /parents/{uid} — קורא firstName, lastName, profileImageBase64.
     * אותו מנגנון כמו loadDashboardData — ValueEventListener רגיל.
     */
    private void loadParentProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // שם ההורה
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה"));

                        // תמונת פרופיל — אם קיימת, מפענחת מ-Base64 ומציגה
                        String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            Bitmap bmp = ImageHelper.base64ToBitmap(base64);
                            if (bmp != null) {
                                ivParentProfile.setImageBitmap(bmp);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // לא מציגים שגיאה — הפרופיל פחות קריטי מרשימת המשימות
                    }
                });
    }

    /**
     * שומר תמונת פרופיל ב-Firebase.
     * משתמש ב-ImageHelper.bitmapToBase64 — אותה פונקציה כמו בשמירת תבניות.
     * נתיב: /parents/{uid}/profileImageBase64
     */
    private void saveProfileImage(Bitmap bitmap) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String base64 = ImageHelper.bitmapToBase64(bitmap);
        if (base64 == null) {
            Toast.makeText(this, "שגיאה בהמרת תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        // updateChildren שומר רק את השדה הזה בלי למחוק את שאר הנתונים של ההורה
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(user.getUid())
                .child("profileImageBase64")
                .setValue(base64)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "תמונת הפרופיל נשמרה!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * טוען את כל הנתונים של הדשבורד בקריאה אחת מ-Firebase:
     * 1. סיכום מספרי (סה"כ / הושלמו / דחופות)
     * 2. רשימת כל המשימות מכל הילדים
     *
     * נתיב: /parents/{uid}/children/ — עובר על כל ילד ועל כל המשימות שלו.
     */
    private void loadDashboardData() {
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
                        assignedTasks.clear();
                        taskDisplayList.clear();

                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String childId = childSnap.getKey();
                            if (childId == null) continue;

                            // שם הילד — לתצוגה ברשימת המשימות
                            String firstName = childSnap.child("firstName").getValue(String.class);
                            String lastName = childSnap.child("lastName").getValue(String.class);
                            String childName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד");

                            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                                total++;

                                String taskId = taskSnap.getKey();
                                String title = taskSnap.child("title").getValue(String.class);
                                String dueAt = taskSnap.child("dueAt").getValue(String.class);
                                Boolean taskDone = taskSnap.child("isDone").getValue(Boolean.class);
                                boolean isDone = taskDone != null && taskDone;

                                // סיכום מספרי
                                if (isDone) {
                                    done++;
                                } else {
                                    if (DateUtils.isDueSoon(dueAt)) dueSoon++;
                                }

                                // שמירה לרשימה
                                AssignedTask at = new AssignedTask();
                                at.childId = childId;
                                at.childName = childName;
                                at.taskId = taskId;
                                at.title = title != null ? title : "";
                                at.dueAt = dueAt != null ? dueAt : "";
                                at.isDone = isDone;
                                assignedTasks.add(at);

                                // שורת תצוגה: "כותרת — ילד | תאריך [סטטוס]"
                                String status = isDone ? "בוצע" : "ממתין";
                                taskDisplayList.add(
                                        at.title + "  —  " + childName
                                        + "\n" + at.dueAt + "  [" + status + "]");
                            }
                        }

                        // עדכון סיכום מספרי
                        tvParentTotalTasks.setText("משימות: " + total);
                        tvParentCompleted.setText("הושלמו: " + done);
                        tvParentDueSoon.setText("דחופות: " + dueSoon);

                        // עדכון רשימת משימות
                        taskAdapter.notifyDataSetChanged();
                        if (assignedTasks.isEmpty()) {
                            tvNoTasks.setVisibility(View.VISIBLE);
                            lvTasks.setVisibility(View.GONE);
                        } else {
                            tvNoTasks.setVisibility(View.GONE);
                            lvTasks.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                "שגיאה בטעינת נתונים: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =====================================================================
    //  פרטי משימה + שינוי תאריך + מחיקה
    // =====================================================================

    /**
     * לחיצה על משימה ברשימה — מציג דיאלוג עם הפרטים ואפשרויות.
     * ההורה יכול: לראות למי הוקצתה, מתי, ואם בוצעה.
     * וגם: לשנות תאריך יעד או למחוק את המשימה.
     */
    private void showTaskOptionsDialog(int position) {
        if (position < 0 || position >= assignedTasks.size()) return;
        AssignedTask task = assignedTasks.get(position);

        String status = task.isDone ? "בוצע" : "ממתין";
        String details = "משימה: " + task.title
                + "\nילד: " + task.childName
                + "\nתאריך יעד: " + task.dueAt
                + "\nסטטוס: " + status;

        new AlertDialog.Builder(this)
                .setTitle("פרטי משימה")
                .setMessage(details)
                .setPositiveButton("שנה תאריך", (d, w) -> showChangeDateDialog(position))
                .setNeutralButton("מחק משימה", (d, w) -> showDeleteTaskDialog(position))
                .setNegativeButton("סגור", null)
                .show();
    }

    /**
     * פותח DatePicker לשינוי תאריך יעד של משימה שכבר הוקצתה.
     * אחרי בחירת תאריך — מעדכן את dueAt ב-Firebase וטוען מחדש.
     */
    private void showChangeDateDialog(int position) {
        AssignedTask task = assignedTasks.get(position);
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            String newDate = day + "/" + (month + 1) + "/" + year;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // עדכון תאריך ב-Firebase
            FirebaseDatabase.getInstance()
                    .getReference("parents")
                    .child(user.getUid())
                    .child("children").child(task.childId)
                    .child("tasks").child(task.taskId)
                    .child("dueAt")
                    .setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "התאריך עודכן!", Toast.LENGTH_SHORT).show();
                        loadDashboardData(); // רענון
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * מבקש אישור ומוחק משימה מ-Firebase.
     */
    private void showDeleteTaskDialog(int position) {
        AssignedTask task = assignedTasks.get(position);

        new AlertDialog.Builder(this)
                .setTitle("מחיקת משימה")
                .setMessage("למחוק את \"" + task.title + "\"?")
                .setPositiveButton("מחק", (d, w) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

                    FirebaseDatabase.getInstance()
                            .getReference("parents")
                            .child(user.getUid())
                            .child("children").child(task.childId)
                            .child("tasks").child(task.taskId)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "המשימה נמחקה",
                                        Toast.LENGTH_SHORT).show();
                                loadDashboardData(); // רענון
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "שגיאה: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================

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

    // =====================================================================
    //  מחלקה פנימית — מידע על משימה שהוקצתה
    // =====================================================================

    /**
     * שומרת את כל הפרטים של משימה מהרשימה.
     * צריך את childId + taskId כדי לדעת איפה ב-Firebase לעדכן/למחוק.
     */
    private static class AssignedTask {
        String childId;
        String childName;
        String taskId;
        String title;
        String dueAt;
        boolean isDone;
    }
}
