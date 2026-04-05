package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.app.DatePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * רשימת המשימות מציגה כרטיסים מעוצבים עם תמונת הילד, שם, תאריך וסטטוס.
 * לחיצה על משימה → פרטים + אפשרות לשנות תאריך יעד או למחוק.
 *
 * Layout: activity_parent_dashboard.xml
 * כרטיס משימה: item_parent_task.xml
 *
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
     * אותו דפוס בדיוק כמו ב-ParentTaskTemplateActivity.
     */
    private final ActivityResultLauncher<String> profileImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                Bitmap bitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (bitmap == null) {
                    Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                    return;
                }
                ivParentProfile.setImageBitmap(bitmap);
                saveProfileImage(bitmap);
            });

    /** כל המשימות מכל הילדים — הנתונים שה-Adapter מציג */
    private final List<AssignedTask> assignedTasks = new ArrayList<>();
    /** ה-Adapter המעוצב שמציג כרטיסים עם תמונה + שם + תאריך + סטטוס */
    private TaskItemAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור אלמנטי פרופיל ההורה
        ivParentProfile = findViewById(R.id.ivParentProfile);
        tvParentName = findViewById(R.id.tvParentName);
        Button btnChangeProfilePic = findViewById(R.id.btnChangeProfilePic);
        btnChangeProfilePic.setOnClickListener(v -> profileImagePicker.launch("image/*"));

        // חיבור TextViews של סיכום משימות
        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);

        // חיבור רשימת משימות + Adapter מעוצב
        lvTasks = findViewById(R.id.lvTasks);
        tvNoTasks = findViewById(R.id.tvNoTasks);

        taskAdapter = new TaskItemAdapter();
        lvTasks.setAdapter(taskAdapter);

        // לחיצה על משימה → פרטים + שינוי תאריך / מחיקה
        lvTasks.setOnItemClickListener((parent, view, position, id) ->
                showTaskOptionsDialog(position));

        // חיבור כפתורים — כל כפתור פותח Activity ייעודי
        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageChildren.setOnClickListener(v ->
                startActivity(new Intent(this, ManageChildrenActivity.class)));

        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnManageTemplates.setOnClickListener(v ->
                startActivity(new Intent(this, ParentTaskTemplateActivity.class)));

        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnAssignTaskToChild.setOnClickListener(v ->
                startActivity(new Intent(this, AssignTaskToChildActivity.class)));

        btnShowQR = findViewById(R.id.btnShowQR);
        btnShowQR.setOnClickListener(v ->
                startActivity(new Intent(this, GenerateQRActivity.class)));

        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // טוען מחדש בכל פעם שחוזרים למסך — כך המספרים תמיד מעודכנים
        loadDashboardData();
        loadParentProfile();
    }

    /**
     * טוען שם + תמונת פרופיל של ההורה מ-Firebase.
     * נתיב: /parents/{uid}
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
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        tvParentName.setText(NameUtils.fullNameOrDefault(firstName, lastName, "הורה"));

                        String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            Bitmap bmp = ImageHelper.base64ToBitmap(base64);
                            if (bmp != null) ivParentProfile.setImageBitmap(bmp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // פרופיל ההורה פחות קריטי — לא מציגים שגיאה
                    }
                });
    }

    /**
     * שומר תמונת פרופיל של ההורה ב-Firebase.
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
     * טוען את כל הנתונים של הדשבורד מ-Firebase:
     * — סיכום מספרי (סה"כ / הושלמו / דחופות)
     * — רשימת כל המשימות מכל הילדים + תמונת הפרופיל של כל ילד
     *
     * נתיב: /parents/{uid}/children/
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
                        // מנקה את מטמון התמונות כי הנתונים ייטענו מחדש
                        taskAdapter.clearPhotoCache();

                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String childId = childSnap.getKey();
                            if (childId == null) continue;

                            String firstName = childSnap.child("firstName").getValue(String.class);
                            String lastName = childSnap.child("lastName").getValue(String.class);
                            String childName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד");

                            // תמונת הפרופיל של הילד — אם קיימת, נשמרת בכל משימה שלו
                            String childProfileBase64 =
                                    childSnap.child("profileImageBase64").getValue(String.class);

                            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                                total++;

                                String taskId = taskSnap.getKey();
                                String title = taskSnap.child("title").getValue(String.class);
                                String dueAt = taskSnap.child("dueAt").getValue(String.class);
                                Boolean taskDone = taskSnap.child("isDone").getValue(Boolean.class);
                                boolean isDone = taskDone != null && taskDone;

                                if (isDone) {
                                    done++;
                                } else {
                                    if (DateUtils.isDueSoon(dueAt)) dueSoon++;
                                }

                                AssignedTask at = new AssignedTask();
                                at.childId = childId;
                                at.childName = childName;
                                at.childProfileBase64 = childProfileBase64;
                                at.taskId = taskId;
                                at.title = title != null ? title : "";
                                at.dueAt = dueAt != null ? dueAt : "";
                                at.isDone = isDone;
                                assignedTasks.add(at);
                            }
                        }

                        // עדכון סיכום מספרי
                        tvParentTotalTasks.setText("משימות: " + total);
                        tvParentCompleted.setText("הושלמו: " + done);
                        tvParentDueSoon.setText("דחופות: " + dueSoon);

                        // עדכון רשימת כרטיסים
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

    private void showChangeDateDialog(int position) {
        AssignedTask task = assignedTasks.get(position);
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            String newDate = day + "/" + (month + 1) + "/" + year;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseDatabase.getInstance()
                    .getReference("parents")
                    .child(user.getUid())
                    .child("children").child(task.childId)
                    .child("tasks").child(task.taskId)
                    .child("dueAt")
                    .setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "התאריך עודכן!", Toast.LENGTH_SHORT).show();
                        loadDashboardData();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

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
                                Toast.makeText(this, "המשימה נמחקה", Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "שגיאה: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("כן, התנתק", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ParentDashboardActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  Adapter מעוצב — מציג כרטיסי משימות עם תמונת ילד + שם + תאריך + סטטוס
    // =====================================================================

    /**
     * Adapter שמציג כרטיסי משימות מעוצבים ב-ListView של הדשבורד.
     *
     * כל כרטיס מכיל:
     * - תמונת הילד (עגולה, מ-Firebase אם קיימת)
     * - שם המשימה (bold)
     * - שם הילד
     * - תאריך יעד עם צבע לפי דחיפות
     * - תג סטטוס צבעוני (בוצע / ממתין / איחור)
     *
     * מטמון תמונות: כל Bitmap נטען פעם אחת ונשמר ב-childPhotoCache לפי childId.
     */
    private class TaskItemAdapter extends ArrayAdapter<AssignedTask> {

        /**
         * מטמון תמונות ילדים — מונע פענוח Base64 חוזר בכל גלילה.
         * ה-key הוא childId, ה-value הוא ה-Bitmap המעוצב לעיגול.
         */
        private final Map<String, Bitmap> childPhotoCache = new HashMap<>();

        TaskItemAdapter() {
            super(ParentDashboardActivity.this, 0, assignedTasks);
        }

        /** מנקה את המטמון — נקרא לפני כל טעינה מחדש מ-Firebase */
        void clearPhotoCache() {
            childPhotoCache.clear();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // שימוש חוזר בview אם קיים — שיפור ביצועים בגלילה
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_parent_task, parent, false);
            }

            AssignedTask task = getItem(position);
            if (task == null) return convertView;

            // --- חיבור Views מה-layout ---
            ImageView ivChildPhoto = convertView.findViewById(R.id.ivChildPhoto);
            TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
            TextView tvChildNameCard = convertView.findViewById(R.id.tvChildNameCard);
            TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
            TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);

            // --- כותרת ---
            tvTaskTitleCard.setText(task.title);

            // --- שם הילד ---
            tvChildNameCard.setText("👤  " + task.childName);

            // --- תאריך + צבע לפי דחיפות ---
            long daysLeft = DateUtils.daysLeft(task.dueAt);
            if (task.isDone) {
                tvDueDateCard.setText("✅  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#4CAF50"));
            } else if (daysLeft < 0) {
                tvDueDateCard.setText("⚠️  איחור — " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#E53935"));
            } else if (daysLeft <= 2) {
                tvDueDateCard.setText("⚡  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvDueDateCard.setText("📅  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#888888"));
            }

            // --- תג סטטוס עם רקע צבעוני עגול ---
            String statusText;
            int chipBgColor;
            int chipTextColor;

            if (task.isDone) {
                statusText   = "בוצע ✓";
                chipBgColor  = Color.parseColor("#E8F5E9");
                chipTextColor = Color.parseColor("#2E7D32");
            } else if (daysLeft < 0) {
                statusText   = "איחור!";
                chipBgColor  = Color.parseColor("#FFEBEE");
                chipTextColor = Color.parseColor("#C62828");
            } else if (daysLeft <= 2) {
                statusText   = "דחוף ⚡";
                chipBgColor  = Color.parseColor("#FFF3E0");
                chipTextColor = Color.parseColor("#E65100");
            } else {
                statusText   = "ממתין";
                chipBgColor  = Color.parseColor("#F5F5F5");
                chipTextColor = Color.parseColor("#666666");
            }

            tvStatusChip.setText(statusText);
            tvStatusChip.setTextColor(chipTextColor);

            // רקע עגול לתג — GradientDrawable, אותו דפוס כמו ב-ChildTaskAdapter
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(chipBgColor);
            chipBg.setCornerRadius(20f);
            tvStatusChip.setBackground(chipBg);

            // --- תמונת הילד (עגולה) ---
            if (task.childProfileBase64 != null && !task.childProfileBase64.isEmpty()) {
                // בדיקה במטמון קודם — כדי לא לפענח Base64 שוב בכל גלילה
                if (childPhotoCache.containsKey(task.childId)) {
                    ivChildPhoto.setImageBitmap(childPhotoCache.get(task.childId));
                } else {
                    Bitmap raw = ImageHelper.base64ToBitmap(task.childProfileBase64);
                    if (raw != null) {
                        // חיתוך עגול — ImageHelper.getCircularBitmap שהוספנו
                        Bitmap circular = ImageHelper.getCircularBitmap(raw);
                        childPhotoCache.put(task.childId, circular);
                        ivChildPhoto.setImageBitmap(circular);
                    } else {
                        // פענוח נכשל — מסתיר תמונה, נשאר רקע אפור
                        ivChildPhoto.setImageBitmap(null);
                    }
                }
            } else {
                // אין תמונה לילד — נשאר הרקע האפור הבהיר מה-XML
                ivChildPhoto.setImageBitmap(null);
            }

            return convertView;
        }
    }

    // =====================================================================
    //  מחלקה פנימית — מידע על משימה שהוקצתה
    // =====================================================================

    /**
     * מחזיקה את כל הנתונים הדרושים לתצוגת כרטיס משימה אחד.
     * כולל עכשיו גם תמונת הפרופיל של הילד (Base64).
     */
    private static class AssignedTask {
        String childId;
        String childName;
        /** תמונת הפרופיל של הילד — Base64. יכול להיות null אם לא הוגדרה */
        String childProfileBase64;
        String taskId;
        String title;
        String dueAt;
        boolean isDone;
    }
}
