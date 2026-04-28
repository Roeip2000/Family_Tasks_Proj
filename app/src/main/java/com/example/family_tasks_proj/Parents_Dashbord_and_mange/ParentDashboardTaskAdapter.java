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

/** מתאם עבור רשימת המשימות בדשבורד ההורה. מציג כותרות של קבוצות ומשימות רגילות. */
class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

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
    private boolean showChildName = false;

    ParentDashboardTaskAdapter(@NonNull Context context, @NonNull List<TaskListItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    public void setShowChildName(boolean showChildName) {
        this.showChildName = showChildName;
    }

    @Override
    public int getViewTypeCount() { return 2; }

    @Override
    public int getItemViewType(int position) {
        TaskListItem item = getItem(position);
        return (item != null && item.getIsHeader()) ? VIEW_TYPE_HEADER : VIEW_TYPE_TASK;
    }

    @Override
    public boolean isEnabled(int position) { return getItemViewType(position) == VIEW_TYPE_TASK; }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TaskListItem item = getItem(position);
        if (item == null) return convertView == null ? new View(getContext()) : convertView;

        if (getItemViewType(position) == VIEW_TYPE_HEADER) return bindHeaderView(convertView, parent, item);
        return bindTaskView(convertView, parent, item.getTask());
    }

    private View bindHeaderView(View convertView, ViewGroup parent, TaskListItem item) {
        if (convertView == null || convertView.findViewById(R.id.tvTaskSectionGroupTitle) == null) {
            convertView = inflater.inflate(R.layout.item_parent_task_section_header, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.tvTaskSectionGroupTitle)).setText(item.getHeaderTitle());
        ((TextView) convertView.findViewById(R.id.tvTaskSectionGroupCount)).setText(String.valueOf(item.getHeaderCount()));
        return convertView;
    }

    private View bindTaskView(View convertView, ViewGroup parent, AssignedTask task) {
        if (convertView == null || convertView.findViewById(R.id.tvTaskTitleCard) == null) {
            convertView = inflater.inflate(R.layout.item_parent_task, parent, false);
        }
        if (task == null) return convertView;

        Context context = getContext();
        TextView tvTitle = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvOwner = convertView.findViewById(R.id.tvTaskOwner);
        TextView tvDue = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatus = convertView.findViewById(R.id.tvStatusChip);
        View viewDot = convertView.findViewById(R.id.viewTaskDot);

        String taskTitle = task.getTitle();
        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = context.getString(R.string.task_default_title);
        }
        tvTitle.setText(taskTitle);

        if (showChildName && task.getChildName() != null && !task.getChildName().trim().isEmpty()) {
            tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));
            tvOwner.setVisibility(View.VISIBLE);
        } else {
            tvOwner.setVisibility(View.GONE);
        }

        long days = DateUtils.daysLeft(task.getDueAt());
        boolean urgent = DateUtils.isDueSoon(task.getDueAt());

        String duePrefix;
        if (task.getIsDone()) {
            duePrefix = context.getString(R.string.task_due_done_prefix);
        } else if (days < 0) {
            duePrefix = context.getString(R.string.task_due_overdue_prefix);
        } else if (urgent) {
            duePrefix = context.getString(R.string.task_due_urgent_prefix);
        } else {
            duePrefix = context.getString(R.string.task_due_regular_prefix);
        }
        tvDue.setText(duePrefix + task.getDueAt());

        int dueTextColor;
        if (task.getIsDone()) {
            dueTextColor = COLOR_DONE;
        } else if (days < 0) {
            dueTextColor = COLOR_OVERDUE;
        } else if (urgent) {
            dueTextColor = COLOR_URGENT;
        } else {
            dueTextColor = COLOR_REGULAR_DUE;
        }
        tvDue.setTextColor(dueTextColor);

        int bg;
        if (task.getIsDone()) {
            bg = COLOR_DONE_BG;
        } else if (days < 0) {
            bg = COLOR_OVERDUE_BG;
        } else if (urgent) {
            bg = COLOR_URGENT_BG;
        } else {
            bg = COLOR_REGULAR_BG;
        }

        int text;
        if (task.getIsDone()) {
            text = COLOR_DONE;
        } else if (days < 0) {
            text = COLOR_OVERDUE;
        } else if (urgent) {
            text = COLOR_URGENT;
        } else {
            text = COLOR_REGULAR_TEXT;
        }

        int dotColor;
        if (task.getIsDone()) {
            dotColor = COLOR_DONE;
        } else if (days < 0) {
            dotColor = COLOR_OVERDUE;
        } else if (urgent) {
            dotColor = COLOR_URGENT;
        } else {
            dotColor = COLOR_REGULAR_DOT;
        }

        String statusText;
        if (task.getIsDone()) {
            statusText = context.getString(R.string.parent_dashboard_task_status_done);
        } else if (days < 0) {
            statusText = context.getString(R.string.parent_dashboard_task_status_late);
        } else if (urgent) {
            statusText = context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            statusText = context.getString(R.string.parent_dashboard_task_status_waiting);
        }
        tvStatus.setText(statusText);
        tvStatus.setTextColor(text);
        
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(bg);
        chip.setCornerRadius(Math.round(getContext().getResources().getDisplayMetrics().density * 14));
        tvStatus.setBackground(chip);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(dotColor);
        viewDot.setBackground(dotBg);

        return convertView;
    }
}
