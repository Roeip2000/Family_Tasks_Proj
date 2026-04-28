package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.TaskListItem;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.DateUtils;

import java.util.List;

/** מתאם עבור רשימת המשימות בדשבורד ההורה. מציג כותרות של קבוצות ומשימות רגילות. */
public class ParentDashboardTaskAdapter extends ArrayAdapter<TaskListItem> {

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

    public ParentDashboardTaskAdapter(@NonNull Context context, @NonNull List<TaskListItem> items) {
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
        if (item != null && item.getIsHeader()) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_TASK;
    }

    @Override
    public boolean isEnabled(int position) {
        TaskListItem item = getItem(position);
        return item != null && !item.getIsHeader();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TaskListItem item = getItem(position);
        if (item == null) {
            return new View(getContext());
        }

        if (item.getIsHeader()) {
            return bindHeaderView(convertView, parent, item);
        }
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
        if (task == null) {
            return convertView;
        }

        Context context = getContext();
        TextView tvTitle = convertView.findViewById(R.id.tvTaskTitleCard);
        TextView tvOwner = convertView.findViewById(R.id.tvTaskOwner);
        TextView tvDue = convertView.findViewById(R.id.tvDueDateCard);
        TextView tvStatus = convertView.findViewById(R.id.tvStatusChip);
        View viewDot = convertView.findViewById(R.id.viewTaskDot);
        
        View imgShell = convertView.findViewById(R.id.imgTaskParentShell);
        ImageView imgTask = convertView.findViewById(R.id.imgTaskParent);

        String titleText = task.getTitle();
        if (titleText == null || titleText.isEmpty()) {
            titleText = context.getString(R.string.task_default_title);
        }
        tvTitle.setText(titleText);

        if (showChildName) {
            String ownerName = task.getChildName();
            if (ownerName == null || ownerName.trim().isEmpty()) {
                ownerName = context.getString(R.string.default_child_name);
            }
            tvOwner.setText(context.getString(R.string.task_assigned_to, ownerName));
            tvOwner.setVisibility(View.VISIBLE);
        } else {
            tvOwner.setVisibility(View.GONE);
        }
        
        // --- נושא במחוון: Image/Base64 ---
        String base64 = task.getImageBase64();
        if (base64 != null && !base64.trim().isEmpty()) {
            android.graphics.Bitmap bitmap = com.example.family_tasks_proj.util.ImageHelper.base64ToBitmap(base64);
            if (bitmap != null) {
                imgShell.setVisibility(View.VISIBLE);
                imgTask.setImageBitmap(bitmap);
            } else {
                imgShell.setVisibility(View.GONE);
            }
        } else {
            imgShell.setVisibility(View.GONE);
        }

        long days = DateUtils.daysLeft(task.getDueAt());
        boolean urgent = DateUtils.isDueSoon(task.getDueAt());

        String duePrefix;
        int dueTextColor;
        if (task.getIsDone()) {
            duePrefix = context.getString(R.string.task_due_done_prefix);
            dueTextColor = COLOR_DONE;
        } else if (days < 0) {
            duePrefix = context.getString(R.string.task_due_overdue_prefix);
            dueTextColor = COLOR_OVERDUE;
        } else if (urgent) {
            duePrefix = context.getString(R.string.task_due_urgent_prefix);
            dueTextColor = COLOR_URGENT;
        } else {
            duePrefix = context.getString(R.string.task_due_regular_prefix);
            dueTextColor = COLOR_REGULAR_DUE;
        }
        tvDue.setText(duePrefix + task.getDueAt());
        tvDue.setTextColor(dueTextColor);

        int bg, text, dotColor;
        String statusText;

        if (task.getIsDone()) {
            bg = COLOR_DONE_BG;
            text = COLOR_DONE;
            dotColor = COLOR_DONE;
            statusText = context.getString(R.string.parent_dashboard_task_status_done);
        } else if (days < 0) {
            bg = COLOR_OVERDUE_BG;
            text = COLOR_OVERDUE;
            dotColor = COLOR_OVERDUE;
            statusText = context.getString(R.string.parent_dashboard_task_status_late);
        } else if (urgent) {
            bg = COLOR_URGENT_BG;
            text = COLOR_URGENT;
            dotColor = COLOR_URGENT;
            statusText = context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            bg = COLOR_REGULAR_BG;
            text = COLOR_REGULAR_TEXT;
            dotColor = COLOR_REGULAR_DOT;
            statusText = context.getString(R.string.parent_dashboard_task_status_waiting);
        }

        tvStatus.setText(statusText);
        tvStatus.setTextColor(text);
        
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(bg);
        chip.setCornerRadius(context.getResources().getDisplayMetrics().density * 14);
        tvStatus.setBackground(chip);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(dotColor);
        viewDot.setBackground(dotBg);

        return convertView;
    }
}
