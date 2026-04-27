package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskListItem;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.DateUtils;

import java.util.List;
import java.util.Map;

// אדפטר לרשימת המשימות בדשבורד ההורה: כותרות קבוצה ומשימות
class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    // צבעי סטטוס משותפים — מוגדרים פעם אחת במקום parseColor חוזר
    private static final int COLOR_DONE = Color.parseColor("#2E7D32");
    private static final int COLOR_DONE_BG = Color.parseColor("#E8F5E9");
    private static final int COLOR_OVERDUE = Color.parseColor("#C62828");
    private static final int COLOR_OVERDUE_BG = Color.parseColor("#FFEBEE");
    private static final int COLOR_URGENT = Color.parseColor("#E65100");
    private static final int COLOR_URGENT_BG = Color.parseColor("#FFF3E0");
    private static final int COLOR_REGULAR_BG = Color.parseColor("#EEF2F7");
    private static final int COLOR_REGULAR_TEXT = Color.parseColor("#52606D");
    private static final int COLOR_REGULAR_DOT = Color.parseColor("#2F80ED");
    private static final int COLOR_REGULAR_DUE = Color.parseColor("#6B7280");

    private final LayoutInflater inflater;
    private final Map<String, Bitmap> childPhotoCache;
    private boolean showChildName = false;

    ParentDashboardTaskAdapter(@NonNull Context context,
                               @NonNull List<TaskListItem> items,
                               @NonNull Map<String, Bitmap> childPhotoCache) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
        this.childPhotoCache = childPhotoCache;
    }

    public void setShowChildName(boolean showChildName) {
        this.showChildName = showChildName;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        TaskListItem item = getItem(position);
        if (item != null && item.isHeader) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_TASK;
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
            if (convertView == null) {
                return new View(getContext());
            }
            return convertView;
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

        TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvTaskOwner = convertView.findViewById(R.id.tvTaskOwner);
        TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);
        View viewTaskDot = convertView.findViewById(R.id.viewTaskDot);

        if (task.title == null || task.title.isEmpty()) {
            tvTaskTitleCard.setText(getContext().getString(R.string.default_task_name));
        } else {
            tvTaskTitleCard.setText(task.title);
        }

        if (showChildName && task.childName != null && !task.childName.trim().isEmpty()) {
            tvTaskOwner.setText(getContext().getString(R.string.parent_dashboard_task_owner_label, task.childName));
            tvTaskOwner.setVisibility(View.VISIBLE);
        } else {
            tvTaskOwner.setVisibility(View.GONE);
        }

        long daysLeft = DateUtils.daysLeft(task.dueAt);

        tvDueDateCard.setText(getContext().getString(task.isDone ? R.string.parent_dashboard_task_due_done : (daysLeft < 0 ? R.string.parent_dashboard_task_due_late : (daysLeft <= 2 ? R.string.parent_dashboard_task_due_urgent : R.string.parent_dashboard_task_due_regular)), task.dueAt));
        tvDueDateCard.setTextColor(task.isDone ? COLOR_DONE : (daysLeft < 0 ? COLOR_OVERDUE : (daysLeft <= 2 ? COLOR_URGENT : COLOR_REGULAR_DUE)));

        int chipBgColor = task.isDone ? COLOR_DONE_BG : (daysLeft < 0 ? COLOR_OVERDUE_BG : (daysLeft <= 2 ? COLOR_URGENT_BG : COLOR_REGULAR_BG));
        int chipTextColor = task.isDone ? COLOR_DONE : (daysLeft < 0 ? COLOR_OVERDUE : (daysLeft <= 2 ? COLOR_URGENT : COLOR_REGULAR_TEXT));
        int dotColor = task.isDone ? COLOR_DONE : (daysLeft < 0 ? COLOR_OVERDUE : (daysLeft <= 2 ? COLOR_URGENT : COLOR_REGULAR_DOT));

        tvStatusChip.setText(getContext().getString(task.isDone ? R.string.parent_dashboard_task_status_done : (daysLeft < 0 ? R.string.parent_dashboard_task_status_late : (daysLeft <= 2 ? R.string.parent_dashboard_task_status_urgent : R.string.parent_dashboard_task_status_waiting))));
        tvStatusChip.setTextColor(chipTextColor);

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(chipBgColor);
        chipBg.setCornerRadius(Math.round(getContext().getResources().getDisplayMetrics().density * 14));
        tvStatusChip.setBackground(chipBg);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(dotColor);
        viewTaskDot.setBackground(dotBg);
        return convertView;
    }
}
