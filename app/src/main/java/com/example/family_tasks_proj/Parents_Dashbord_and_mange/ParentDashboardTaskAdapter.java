package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;

import java.util.List;
import java.util.Map;

/**
 * אדפטר לרשימת המשימות בדשבורד ההורה.
 * תומך בשני סוגי שורות: כותרת קבוצה (header) ושורת משימה רגילה.
 * כל משימה מציגה כותרת, תאריך יעד, סטטוס (בוצע/דחוף/ממתין) ותמונת ילד.
 */
class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    private final LayoutInflater inflater;
    private final Map<String, Bitmap> childPhotoCache;

    ParentDashboardTaskAdapter(@NonNull Context context,
                               @NonNull List<TaskListItem> items,
                               @NonNull Map<String, Bitmap> childPhotoCache) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
        this.childPhotoCache = childPhotoCache;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        TaskListItem item = getItem(position);
        return item != null && item.isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_TASK;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == VIEW_TYPE_TASK;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TaskListItem item = getItem(position);
        if (item == null) {
            return convertView == null ? new View(getContext()) : convertView;
        }

        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            return bindHeaderView(convertView, parent, item);
        }

        return bindTaskView(convertView, parent, item.task);
    }

    private View bindHeaderView(View convertView, ViewGroup parent, TaskListItem item) {
        if (convertView == null || convertView.findViewById(R.id.tvTaskSectionGroupTitle) == null) {
            convertView = inflater.inflate(R.layout.item_parent_task_section_header, parent, false);
        }

        TextView tvGroupTitle = convertView.findViewById(R.id.tvTaskSectionGroupTitle);
        TextView tvGroupCount = convertView.findViewById(R.id.tvTaskSectionGroupCount);
        tvGroupTitle.setText(item.headerTitle);
        tvGroupCount.setText(String.valueOf(item.headerCount));
        return convertView;
    }

    private View bindTaskView(View convertView, ViewGroup parent, AssignedTask task) {
        if (convertView == null || convertView.findViewById(R.id.tvTaskTitleCard) == null) {
            convertView = inflater.inflate(R.layout.item_parent_task, parent, false);
        }
        if (task == null) {
            return convertView;
        }

        ImageView ivChildPhoto = convertView.findViewById(R.id.ivChildPhoto);
        TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvChildNameCard = convertView.findViewById(R.id.tvChildNameCard);
        TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);

        tvTaskTitleCard.setText(task.title == null || task.title.isEmpty()
                ? getContext().getString(R.string.default_task_name)
                : task.title);
        tvChildNameCard.setVisibility(View.GONE);
        tvDueDateCard.setText(getDueLine(task));
        tvDueDateCard.setTextColor(getDueLineColor(task));

        String statusText = getTaskStatusLabel(task);
        int chipBgColor;
        int chipTextColor;

        if (task.isDone) {
            chipBgColor = Color.parseColor("#E8F5E9");
            chipTextColor = Color.parseColor("#2E7D32");
        } else if (DateUtils.daysLeft(task.dueAt) < 0) {
            chipBgColor = Color.parseColor("#FFEBEE");
            chipTextColor = Color.parseColor("#C62828");
        } else if (isUrgentTask(task)) {
            chipBgColor = Color.parseColor("#FFF3E0");
            chipTextColor = Color.parseColor("#E65100");
        } else {
            chipBgColor = Color.parseColor("#EEF2F7");
            chipTextColor = Color.parseColor("#52606D");
        }

        tvStatusChip.setText(statusText);
        tvStatusChip.setTextColor(chipTextColor);

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(chipBgColor);
        chipBg.setCornerRadius(dpToPx(14));
        tvStatusChip.setBackground(chipBg);

        bindChildPhoto(ivChildPhoto, task.childId, task.childProfileBase64);
        return convertView;
    }

    private void bindChildPhoto(ImageView imageView, String childId, String base64) {
        imageView.setImageDrawable(null);

        if (base64 == null || base64.trim().isEmpty()) {
            return;
        }

        if (childPhotoCache.containsKey(childId)) {
            imageView.setImageBitmap(childPhotoCache.get(childId));
            return;
        }

        Bitmap raw = ImageHelper.base64ToBitmap(base64);
        if (raw == null) {
            return;
        }

        Bitmap circular = ImageHelper.getCircularBitmap(raw);
        childPhotoCache.put(childId, circular);
        imageView.setImageBitmap(circular);
    }

    private String getTaskStatusLabel(AssignedTask task) {
        if (task.isDone) {
            return getContext().getString(R.string.parent_dashboard_task_status_done);
        }

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) {
            return getContext().getString(R.string.parent_dashboard_task_status_late);
        }
        if (daysLeft <= 2) {
            return getContext().getString(R.string.parent_dashboard_task_status_urgent);
        }
        return getContext().getString(R.string.parent_dashboard_task_status_waiting);
    }

    private String getDueLine(AssignedTask task) {
        if (task.isDone) {
            return getContext().getString(R.string.parent_dashboard_task_due_done, task.dueAt);
        }

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) {
            return getContext().getString(R.string.parent_dashboard_task_due_late, task.dueAt);
        }
        if (daysLeft <= 2) {
            return getContext().getString(R.string.parent_dashboard_task_due_urgent, task.dueAt);
        }
        return getContext().getString(R.string.parent_dashboard_task_due_regular, task.dueAt);
    }

    private int getDueLineColor(AssignedTask task) {
        if (task.isDone) return Color.parseColor("#2E7D32");

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) return Color.parseColor("#C62828");
        if (daysLeft <= 2) return Color.parseColor("#E65100");
        return Color.parseColor("#6B7280");
    }

    private boolean isUrgentTask(AssignedTask task) {
        return task != null && !task.isDone && DateUtils.isDueSoon(task.dueAt);
    }

    private int dpToPx(int value) {
        return Math.round(getContext().getResources().getDisplayMetrics().density * value);
    }
}
