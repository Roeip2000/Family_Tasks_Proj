package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
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

/**
 * אדפטר לרשימת המשימות בדשבורד ההורה.
 * תומך בשני סוגי שורות: כותרת קבוצה (header) ושורת משימה רגילה.
 * כל משימה מציגה כותרת, תאריך יעד וסטטוס ברור בצד.
 */
class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    private final LayoutInflater inflater;

    ParentDashboardTaskAdapter(@NonNull Context context,
                               @NonNull List<TaskListItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
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

        TextView tvTaskTitleCard = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvDueDateCard = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatusChip = convertView.findViewById(R.id.tvStatusChip);
        View viewTaskDot = convertView.findViewById(R.id.viewTaskDot);

        tvTaskTitleCard.setText(task.title == null || task.title.isEmpty()
                ? getContext().getString(R.string.default_task_name)
                : task.title);
        tvDueDateCard.setText(getDueLine(task));
        tvDueDateCard.setTextColor(getDueLineColor(task));

        String statusText = getTaskStatusLabel(task);
        int chipBgColor;
        int chipTextColor;
        int dotColor;

        if (task.isDone) {
            chipBgColor = getContext().getColor(R.color.accent_light);
            chipTextColor = getContext().getColor(R.color.success_dark);
            dotColor = chipTextColor;
        } else if (DateUtils.daysLeft(task.dueAt) < 0) {
            chipBgColor = getContext().getColor(R.color.danger_light);
            chipTextColor = getContext().getColor(R.color.danger);
            dotColor = chipTextColor;
        } else if (isUrgentTask(task)) {
            chipBgColor = getContext().getColor(R.color.urgent_light);
            chipTextColor = getContext().getColor(R.color.warning_dark);
            dotColor = chipTextColor;
        } else {
            chipBgColor = getContext().getColor(R.color.surface_muted);
            chipTextColor = getContext().getColor(R.color.text_secondary);
            dotColor = getContext().getColor(R.color.primary);
        }

        tvStatusChip.setText(statusText);
        tvStatusChip.setTextColor(chipTextColor);

        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(chipBgColor);
        chipBg.setCornerRadius(dpToPx(14));
        tvStatusChip.setBackground(chipBg);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(dotColor);
        viewTaskDot.setBackground(dotBg);
        return convertView;
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
        if (task.isDone) return getContext().getColor(R.color.success_dark);

        long daysLeft = DateUtils.daysLeft(task.dueAt);
        if (daysLeft < 0) return getContext().getColor(R.color.danger);
        if (daysLeft <= 2) return getContext().getColor(R.color.warning_dark);
        return getContext().getColor(R.color.text_secondary);
    }

    private boolean isUrgentTask(AssignedTask task) {
        return task != null && !task.isDone && DateUtils.isDueSoon(task.dueAt);
    }

    private int dpToPx(int value) {
        return Math.round(getContext().getResources().getDisplayMetrics().density * value);
    }
}
