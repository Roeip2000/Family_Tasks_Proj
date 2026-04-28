package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
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

// אדפטר לרשימת המשימות בדשבורד ההורה: כותרות קבוצה ומשימות
class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    // צבעי סטטוס מרוכזים כדי שיהיה קל לשנות צבע בזמן הבחינה
    private static final String HEX_DONE = "#2E7D32";
    private static final String HEX_DONE_BG = "#E8F5E9";
    private static final String HEX_OVERDUE = "#C62828";
    private static final String HEX_OVERDUE_BG = "#FFEBEE";
    private static final String HEX_URGENT = "#E65100";
    private static final String HEX_URGENT_BG = "#FFF3E0";
    private static final String HEX_REGULAR_BG = "#EEF2F7";
    private static final String HEX_REGULAR_TEXT = "#52606D";
    private static final String HEX_REGULAR_DOT = "#2F80ED";
    private static final String HEX_REGULAR_DUE = "#6B7280";
    private static final int COLOR_DONE = Color.parseColor(HEX_DONE);
    private static final int COLOR_DONE_BG = Color.parseColor(HEX_DONE_BG);
    private static final int COLOR_OVERDUE = Color.parseColor(HEX_OVERDUE);
    private static final int COLOR_OVERDUE_BG = Color.parseColor(HEX_OVERDUE_BG);
    private static final int COLOR_URGENT = Color.parseColor(HEX_URGENT);
    private static final int COLOR_URGENT_BG = Color.parseColor(HEX_URGENT_BG);
    private static final int COLOR_REGULAR_BG = Color.parseColor(HEX_REGULAR_BG);
    private static final int COLOR_REGULAR_TEXT = Color.parseColor(HEX_REGULAR_TEXT);
    private static final int COLOR_REGULAR_DOT = Color.parseColor(HEX_REGULAR_DOT);
    private static final int COLOR_REGULAR_DUE = Color.parseColor(HEX_REGULAR_DUE);

    private final LayoutInflater inflater;
    private boolean showChildName = false;

    ParentDashboardTaskAdapter(@NonNull Context context,
                               @NonNull List<TaskListItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
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
