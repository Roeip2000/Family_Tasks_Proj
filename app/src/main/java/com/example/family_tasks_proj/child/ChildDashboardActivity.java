package com.example.family_tasks_proj.child;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.auth.MainActivity;
import com.example.family_tasks_proj.child.Class_child.ChildTask;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * דשבורד הילד — מציג שם, כוכבים, סיכום משימות, ורשימת משימות מפורטת.
 *
 * אחריות:
 * - מזהה את הילד מ-Intent extras (אחרי בחירה) או מ-SharedPreferences (סשן קודם).
 * - טוען כותרת (שם ילד + תמונת פרופיל) מ-/parents/{parentId}/children/{childId}.
 * - טוען משימות מ-.../tasks — סופר סה"כ, שבוצעו, ודחופות (עד 2 ימים).
 * - מחשב סכום כוכבים ממשימות שבוצעו (isDone == true).
 * - מציג רשימת משימות ב-RecyclerView עם כרטיסים (ChildTaskAdapter).
 * - כפתור יציאה — מנקה סשן ומחזיר למסך הראשי עם AlertDialog אישור.
 *
 * Layout: activity_child_dashboard.xml
 */
public class ChildDashboardActivity extends AppCompatActivity {

    private static final String FILTER_DEFAULT_TEXT_COLOR = "#355070";
    private static final String FILTER_SELECTED_TEXT_COLOR = "#102A43";
    private static final String FILTER_DEFAULT_STROKE_COLOR = "#D6E4F0";
    private static final String FILTER_SELECTED_STROKE_COLOR = "#FFFFFF";

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
    private LinearLayout filterNotCompleted, filterCompleted, filterUrgent;
    /** תמונת הפרופיל של הילד — עגולה, עם אנימציית fade-in */
    private ImageView imgChildAvatar;

    // --- Data ---
    private String parentId;
    private String childId;
    /** רשימת המקור — כוללת את כל המשימות שנטענו מ-Firebase */
    private final List<ChildTask> allTasks = new ArrayList<>();
    /** הרשימה המוצגת כרגע ב-RecyclerView לפי הפילטר הפעיל */
    private final List<ChildTask> taskList = new ArrayList<>();
    /** אדפטר שמציג את כרטיסי המשימות */
    private ChildTaskAdapter adapter;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        // הגדרת רקע גרדיאנט סגול-כחול לדשבורד (מוגדר כאן כדי לא לצור קובץ drawable נפרד)
        ScrollView rootView = findViewById(R.id.scrollRootChildDashboard);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#7B61FF"), Color.parseColor("#4A90E2")}
        );
        rootView.setBackground(gradient);

        // חיבור views מה-layout
        tvChildName = findViewById(R.id.tvChildName);
        tvStars = findViewById(R.id.tvStars);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvDueSoon = findViewById(R.id.tvDueSoon);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        rvTasks = findViewById(R.id.rvTasks);
        btnLogout = findViewById(R.id.btnLogout);
        imgChildAvatar = findViewById(R.id.imgChildAvatar);
        filterNotCompleted = findViewById(R.id.filterNotCompleted);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterUrgent = findViewById(R.id.filterUrgent);

        // הגדרת RecyclerView עם Adapter + callback לסימון משימה כ-"בוצע"
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildTaskAdapter(taskList, this::markTaskDone);
        rvTasks.setAdapter(adapter);

        filterNotCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.NOT_COMPLETED));
        filterCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.COMPLETED));
        filterUrgent.setOnClickListener(v -> setActiveFilter(FilterMode.URGENT));

        // כפתור יציאה — מציג AlertDialog אישור לפני מחיקת סשן
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // זיהוי הילד — מ-Intent (אחרי בחירה) או מ-SharedPreferences (סשן קודם)
        resolveSession();
        if (isBlank(parentId) || isBlank(childId)) {
            Toast.makeText(this, "חסר סשן. חזור למסך בחירת ילד.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // טעינת נתונים מ-Firebase
        updateFilterSelectionUi();
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
     * טוען שם הילד ותמונת הפרופיל מ-Firebase.
     * אם יש תמונה — מחתך אותה לעיגול ומציג עם אנימציית fade-in.
     * אם אין — נשאר ה-placeholder ולא קורסת.
     */
    private void loadChildHeader() {
        childRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);

                // בניית שם מלא — משתמש ב-NameUtils למניעת שכפול
                String displayName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד");
                tvChildName.setText("שלום " + displayName + "!");

                // ניסיון לטעון תמונת פרופיל — אם אין, נשאר ה-placeholder
                String base64 = snapshot.child("profileImageBase64").getValue(String.class);
                if (base64 != null && !base64.isEmpty()) {
                    Bitmap raw = ImageHelper.base64ToBitmap(base64);
                    if (raw != null) {
                        // חיתוך עגול + אנימציית fade-in
                        Bitmap circular = ImageHelper.getCircularBitmap(raw);
                        imgChildAvatar.setImageBitmap(circular);
                        imgChildAvatar.setAlpha(0f);
                        imgChildAvatar.animate().alpha(1f).setDuration(400).start();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this,
                        "שגיאה בטעינת שם: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * טוען משימות מ-Firebase, מעדכן סיכומים, כוכבים, ורשימה.
     *
     * מחשב:
     * - notCompleted: משימות שעדיין לא הושלמו
     * - done: משימות שבוצעו (isDone == true)
     * - dueSoon: משימות דחופות (0-2 ימים, עדיין לא בוצעו)
     * - stars: סכום starsWorth של משימות שבוצעו
     */
    private void loadTasks() {
        DatabaseReference tasksRef = childRef().child(NODE_TASKS);

        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();

                int done = 0, dueSoon = 0, notCompleted = 0;
                long stars = 0;

                for (DataSnapshot s : snapshot.getChildren()) {
                    ChildTask t = s.getValue(ChildTask.class);
                    if (t == null) continue;

                    // מוודא שה-id קיים (Firebase key)
                    if (t.id == null || t.id.isEmpty()) {
                        t.id = s.getKey();
                    }

                    // סיכום מכל המשימות (כולל שבוצעו)
                    if (t.isDone) {
                        done++;
                        stars += t.starsWorth;
                    } else {
                        notCompleted++;
                        if (DateUtils.isDueSoon(t.dueAt)) dueSoon++;
                    }

                    allTasks.add(t);
                }

                // עדכון UI סיכומים
                tvTotalTasks.setText(String.valueOf(notCompleted));
                tvCompleted.setText(String.valueOf(done));
                tvDueSoon.setText(String.valueOf(dueSoon));
                tvStars.setText(stars + " ⭐");
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildDashboardActivity.this,
                        "שגיאה בטעינת משימות: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback מ-ChildTaskAdapter — נקרא כשהילד לוחץ "בוצע" על משימה.
     *
     * מציג AlertDialog לאישור, ואם מאושר:
     * 1. מעדכן isDone = true ב-Firebase.
     * 2. טוען מחדש את רשימת המשימות (loadTasks) — כולל עדכון סיכומים וכוכבים.
     *
     * @param task המשימה שהילד סימן
     * @param position המיקום ברשימה (לא בשימוש כרגע — refresh מלא)
     */
    private void markTaskDone(ChildTask task, int position) {
        if (task.id == null || task.id.isEmpty()) {
            Toast.makeText(this, "שגיאה: חסר מזהה משימה", Toast.LENGTH_SHORT).show();
            return;
        }

        // אישור מהילד לפני סימון
        new AlertDialog.Builder(this)
                .setTitle("סימון משימה")
                .setMessage("לסמן את \"" + task.title + "\" כבוצע?")
                .setPositiveButton("כן, סיימתי! ✓", (dialog, which) -> {
                    // עדכון isDone ב-Firebase
                    DatabaseReference taskRef = childRef()
                            .child(NODE_TASKS)
                            .child(task.id)
                            .child("isDone");

                    taskRef.setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ChildDashboardActivity.this,
                                        "כל הכבוד! 🌟", Toast.LENGTH_SHORT).show();

                                task.isDone = true;
                                applyFilter();

                                // טוענים מחדש בשביל לעדכן סיכומים ולסנכרן מול Firebase
                                loadTasks();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChildDashboardActivity.this,
                                        "שגיאה בעדכון: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("עוד לא", null)
                .show();
    }

    /**
     * מציג AlertDialog לאישור יציאה.
     * אם המשתמש מאשר — מנקה סשן מ-SharedPreferences ומחזיר למסך הראשי.
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

    /** בודק אם מחרוזת ריקה או null. */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void setActiveFilter(FilterMode filterMode) {
        if (filterMode == null || activeFilter == filterMode) return;
        activeFilter = filterMode;
        updateFilterSelectionUi();
        applyFilter();
    }

    private void applyFilter() {
        taskList.clear();

        for (ChildTask task : allTasks) {
            if (task == null) continue;
            if (matchesActiveFilter(task)) {
                taskList.add(task);
            }
        }

        updateEmptyStateText();

        if (taskList.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private boolean matchesActiveFilter(ChildTask task) {
        switch (activeFilter) {
            case COMPLETED:
                return task.isDone;
            case URGENT:
                return !task.isDone && DateUtils.isDueSoon(task.dueAt);
            case NOT_COMPLETED:
            default:
                return !task.isDone;
        }
    }

    private void updateFilterSelectionUi() {
        updateFilterBlock(filterUrgent, activeFilter == FilterMode.URGENT, "#FFF4E5", "#FFD199");
        updateFilterBlock(filterCompleted, activeFilter == FilterMode.COMPLETED, "#ECF8F1", "#BDE7C9");
        updateFilterBlock(filterNotCompleted, activeFilter == FilterMode.NOT_COMPLETED, "#EAF4FF", "#B8DBFF");
    }

    private void updateFilterBlock(LinearLayout layout, boolean selected, String defaultColor, String selectedColor) {
        if (layout == null) return;

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(Color.parseColor(selected ? selectedColor : defaultColor));
        background.setStroke(selected ? 4 : 2,
                Color.parseColor(selected ? FILTER_SELECTED_STROKE_COLOR : FILTER_DEFAULT_STROKE_COLOR));

        layout.setSelected(selected);
        layout.setBackground(background);
        layout.setAlpha(1f);
        layout.setScaleX(selected ? 1.02f : 1f);
        layout.setScaleY(selected ? 1.02f : 1f);
        layout.setElevation(selected ? 6f : 0f);
        updateFilterTexts(layout, selected);
    }

    private void updateFilterTexts(LinearLayout layout, boolean selected) {
        int textColor = Color.parseColor(selected ? FILTER_SELECTED_TEXT_COLOR : FILTER_DEFAULT_TEXT_COLOR);
        int countColor = textColor;

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (!(child instanceof TextView)) continue;

            TextView textView = (TextView) child;
            Object tag = textView.getTag();
            boolean isCount = "count".equals(String.valueOf(tag));
            textView.setTextColor(isCount ? countColor : textColor);
            textView.setAlpha(selected ? 1f : 0.86f);
        }
    }

    private void updateEmptyStateText() {
        if (tvNoTasks == null) return;

        switch (activeFilter) {
            case COMPLETED:
                tvNoTasks.setText("אין משימות שהושלמו כרגע.");
                break;
            case URGENT:
                tvNoTasks.setText("אין משימות דחופות כרגע.");
                break;
            case NOT_COMPLETED:
            default:
                tvNoTasks.setText("אין משימות שלא הושלמו כרגע.");
                break;
        }
    }

    private enum FilterMode {
        NOT_COMPLETED,
        COMPLETED,
        URGENT
    }
}
