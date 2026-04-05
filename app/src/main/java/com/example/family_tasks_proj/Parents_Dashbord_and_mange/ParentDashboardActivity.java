package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String FILTER_DEFAULT_TEXT_COLOR = "#355070";
    private static final String FILTER_SELECTED_TEXT_COLOR = "#102A43";
    private static final String FILTER_DEFAULT_STROKE_COLOR = "#D6E4F0";
    private static final String FILTER_SELECTED_STROKE_COLOR = "#FFFFFF";

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR, btnLogout;
    private TextView tvParentTotalTasks, tvParentCompleted, tvParentDueSoon, tvNoTasks;
    private ListView lvTasks;
    private ImageView ivParentProfile;
    private TextView tvParentName;
    private LinearLayout filterUrgent, filterCompleted, filterNotCompleted;

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

    private final List<AssignedTask> allAssignedTasks = new ArrayList<>();
    private final List<AssignedTask> filteredAssignedTasks = new ArrayList<>();
    private TaskItemAdapter taskAdapter;
    private FilterMode activeFilter = FilterMode.NOT_COMPLETED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        ivParentProfile = findViewById(R.id.ivParentProfile);
        tvParentName = findViewById(R.id.tvParentName);
        Button btnChangeProfilePic = findViewById(R.id.btnChangeProfilePic);
        btnChangeProfilePic.setOnClickListener(v -> profileImagePicker.launch("image/*"));

        tvParentTotalTasks = findViewById(R.id.tvParentTotalTasks);
        tvParentCompleted = findViewById(R.id.tvParentCompleted);
        tvParentDueSoon = findViewById(R.id.tvParentDueSoon);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        lvTasks = findViewById(R.id.lvTasks);
        filterUrgent = findViewById(R.id.filterUrgent);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterNotCompleted = findViewById(R.id.filterNotCompleted);

        taskAdapter = new TaskItemAdapter();
        lvTasks.setAdapter(taskAdapter);
        lvTasks.setOnItemClickListener((parent, view, position, id) -> showTaskOptionsDialog(position));

        filterUrgent.setOnClickListener(v -> setActiveFilter(FilterMode.URGENT));
        filterCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.COMPLETED));
        filterNotCompleted.setOnClickListener(v -> setActiveFilter(FilterMode.NOT_COMPLETED));
        updateFilterSelectionUi();

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
        loadDashboardData();
        loadParentProfile();
    }

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
                        if (base64 == null || base64.isEmpty()) return;

                        Bitmap bmp = ImageHelper.base64ToBitmap(base64);
                        if (bmp != null) {
                            ivParentProfile.setImageBitmap(bmp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

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
                        int done = 0;
                        int dueSoon = 0;
                        int notCompleted = 0;

                        allAssignedTasks.clear();
                        taskAdapter.clearPhotoCache();

                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String childId = childSnap.getKey();
                            if (childId == null || childId.isEmpty()) continue;

                            String firstName = childSnap.child("firstName").getValue(String.class);
                            String lastName = childSnap.child("lastName").getValue(String.class);
                            String childName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד");
                            String childProfileBase64 =
                                    childSnap.child("profileImageBase64").getValue(String.class);

                            for (DataSnapshot taskSnap : childSnap.child("tasks").getChildren()) {
                                String taskId = taskSnap.getKey();
                                if (taskId == null || taskId.isEmpty()) continue;

                                String title = taskSnap.child("title").getValue(String.class);
                                String dueAt = taskSnap.child("dueAt").getValue(String.class);
                                Boolean taskDone = taskSnap.child("isDone").getValue(Boolean.class);
                                boolean isDone = taskDone != null && taskDone;

                                if (isDone) {
                                    done++;
                                } else {
                                    notCompleted++;
                                    if (DateUtils.isDueSoon(dueAt)) {
                                        dueSoon++;
                                    }
                                }

                                AssignedTask assignedTask = new AssignedTask();
                                assignedTask.childId = childId;
                                assignedTask.childName = childName;
                                assignedTask.childProfileBase64 = childProfileBase64;
                                assignedTask.taskId = taskId;
                                assignedTask.title = title != null ? title : "";
                                assignedTask.dueAt = dueAt != null ? dueAt : "";
                                assignedTask.isDone = isDone;
                                allAssignedTasks.add(assignedTask);
                            }
                        }

                        tvParentTotalTasks.setText(String.valueOf(notCompleted));
                        tvParentCompleted.setText(String.valueOf(done));
                        tvParentDueSoon.setText(String.valueOf(dueSoon));
                        applyFilter();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                "שגיאה בטעינת נתונים: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setActiveFilter(FilterMode filterMode) {
        if (filterMode == null || activeFilter == filterMode) return;
        activeFilter = filterMode;
        updateFilterSelectionUi();
        applyFilter();
    }

    private void applyFilter() {
        filteredAssignedTasks.clear();

        for (AssignedTask task : allAssignedTasks) {
            if (task == null) continue;
            if (matchesActiveFilter(task)) {
                filteredAssignedTasks.add(task);
            }
        }

        updateEmptyStateText();
        taskAdapter.notifyDataSetChanged();

        if (filteredAssignedTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            lvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            lvTasks.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesActiveFilter(AssignedTask task) {
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

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (!(child instanceof TextView)) continue;

            TextView textView = (TextView) child;
            textView.setTextColor(textColor);
            textView.setAlpha(selected ? 1f : 0.86f);
        }
    }

    private void showTaskOptionsDialog(int position) {
        if (position < 0 || position >= filteredAssignedTasks.size()) return;
        AssignedTask task = filteredAssignedTasks.get(position);

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
        if (position < 0 || position >= filteredAssignedTasks.size()) return;
        AssignedTask task = filteredAssignedTasks.get(position);
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
        if (position < 0 || position >= filteredAssignedTasks.size()) return;
        AssignedTask task = filteredAssignedTasks.get(position);

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

    private class TaskItemAdapter extends ArrayAdapter<AssignedTask> {

        private final Map<String, Bitmap> childPhotoCache = new HashMap<>();

        TaskItemAdapter() {
            super(ParentDashboardActivity.this, 0, filteredAssignedTasks);
        }

        void clearPhotoCache() {
            childPhotoCache.clear();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_parent_task, parent, false);
            }

            AssignedTask task = getItem(position);
            if (task == null) return convertView;

            ImageView ivChildPhoto = convertView.findViewById(R.id.ivChildPhoto);
            TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
            TextView tvChildNameCard = convertView.findViewById(R.id.tvChildNameCard);
            TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
            TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);

            tvTaskTitleCard.setText(task.title);
            tvChildNameCard.setText("ילד  " + task.childName);

            long daysLeft = DateUtils.daysLeft(task.dueAt);
            if (task.isDone) {
                tvDueDateCard.setText("בוצע  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#4CAF50"));
            } else if (daysLeft < 0) {
                tvDueDateCard.setText("איחור - " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#E53935"));
            } else if (daysLeft <= 2) {
                tvDueDateCard.setText("דחוף  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvDueDateCard.setText("יעד  " + task.dueAt);
                tvDueDateCard.setTextColor(Color.parseColor("#888888"));
            }

            String statusText;
            int chipBgColor;
            int chipTextColor;

            if (task.isDone) {
                statusText = "בוצע";
                chipBgColor = Color.parseColor("#E8F5E9");
                chipTextColor = Color.parseColor("#2E7D32");
            } else if (daysLeft < 0) {
                statusText = "איחור";
                chipBgColor = Color.parseColor("#FFEBEE");
                chipTextColor = Color.parseColor("#C62828");
            } else if (daysLeft <= 2) {
                statusText = "דחוף";
                chipBgColor = Color.parseColor("#FFF3E0");
                chipTextColor = Color.parseColor("#E65100");
            } else {
                statusText = "ממתין";
                chipBgColor = Color.parseColor("#F5F5F5");
                chipTextColor = Color.parseColor("#666666");
            }

            tvStatusChip.setText(statusText);
            tvStatusChip.setTextColor(chipTextColor);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(chipBgColor);
            chipBg.setCornerRadius(20f);
            tvStatusChip.setBackground(chipBg);

            if (task.childProfileBase64 != null && !task.childProfileBase64.isEmpty()) {
                if (childPhotoCache.containsKey(task.childId)) {
                    ivChildPhoto.setImageBitmap(childPhotoCache.get(task.childId));
                } else {
                    Bitmap raw = ImageHelper.base64ToBitmap(task.childProfileBase64);
                    if (raw != null) {
                        Bitmap circular = ImageHelper.getCircularBitmap(raw);
                        childPhotoCache.put(task.childId, circular);
                        ivChildPhoto.setImageBitmap(circular);
                    } else {
                        ivChildPhoto.setImageBitmap(null);
                    }
                }
            } else {
                ivChildPhoto.setImageBitmap(null);
            }

            return convertView;
        }
    }

    private static class AssignedTask {
        String childId;
        String childName;
        String childProfileBase64;
        String taskId;
        String title;
        String dueAt;
        boolean isDone;
    }

    private enum FilterMode {
        NOT_COMPLETED,
        COMPLETED,
        URGENT
    }
}
