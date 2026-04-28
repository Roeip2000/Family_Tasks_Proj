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

        TextView tvTitle = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvOwner = convertView.findViewById(R.id.tvTaskOwner);
        TextView tvDue = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatus = convertView.findViewById(R.id.tvStatusChip);
        View viewDot = convertView.findViewById(R.id.viewTaskDot);

        tvTitle.setText(task.getTitle().isEmpty() ? "משימה" : task.getTitle());

        if (showChildName && task.getChildName() != null && !task.getChildName().trim().isEmpty()) {
            tvOwner.setText("מיועד ל: " + task.getChildName());
            tvOwner.setVisibility(View.VISIBLE);
        } else tvOwner.setVisibility(View.GONE);

        long days = DateUtils.daysLeft(task.getDueAt());
        boolean urgent = DateUtils.isDueSoon(task.getDueAt());

        tvDue.setText((task.getIsDone() ? "בוצע בתאריך: " : (days < 0 ? "באיחור: " : (urgent ? "דחוף: " : "תאריך יעד: "))) + task.getDueAt());
        tvDue.setTextColor(task.getIsDone() ? COLOR_DONE : (days < 0 ? COLOR_OVERDUE : (urgent ? COLOR_URGENT : COLOR_REGULAR_DUE)));

        int bg = task.getIsDone() ? COLOR_DONE_BG : (days < 0 ? COLOR_OVERDUE_BG : (urgent ? COLOR_URGENT_BG : COLOR_REGULAR_BG));
        int text = task.getIsDone() ? COLOR_DONE : (days < 0 ? COLOR_OVERDUE : (urgent ? COLOR_URGENT : COLOR_REGULAR_TEXT));
        int dotColor = task.getIsDone() ? COLOR_DONE : (days < 0 ? COLOR_OVERDUE : (urgent ? COLOR_URGENT : COLOR_REGULAR_DOT));

        tvStatus.setText(task.getIsDone() ? "בוצע" : (days < 0 ? "באיחור" : (urgent ? "דחוף" : "ממתין")));
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
