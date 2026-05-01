package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.AssignedTask;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.DateUtils;

import java.util.List;

/** מתאם עבור רשימת המשימות בדשבורד ההורה. */
public class ParentDashboardTaskAdapter extends ArrayAdapter<AssignedTask> {

    private final LayoutInflater inflater;
    private boolean showChildName = false;

    public ParentDashboardTaskAdapter(@NonNull Context context, @NonNull List<AssignedTask> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    public void setShowChildName(boolean showChildName) {
        this.showChildName = showChildName;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        AssignedTask task = getItem(position);
        return bindTaskView(convertView, parent, task);
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

        // צבעים וטקסטים משתנים לפי מצב המשימה: בוצעה, באיחור, דחופה או רגילה.
        String duePrefix;
        int dueTextColor;
        if (task.getIsDone()) {
            duePrefix = context.getString(R.string.task_due_done_prefix);
            dueTextColor = R.color.success;
        } else if (days < 0) {
            duePrefix = context.getString(R.string.task_due_overdue_prefix);
            dueTextColor = R.color.danger;
        } else if (urgent) {
            duePrefix = context.getString(R.string.task_due_urgent_prefix);
            dueTextColor = R.color.urgent;
        } else {
            duePrefix = context.getString(R.string.task_due_regular_prefix);
            dueTextColor = R.color.regular_due;
        }
        tvDue.setText(context.getString(R.string.task_due_display, duePrefix, task.getDueAt()));
        tvDue.setTextColor(context.getColor(dueTextColor));

        int bg, text, dotColor;
        String statusText;

        if (task.getIsDone()) {
            bg = R.color.success_light;
            text = R.color.success;
            dotColor = R.color.success;
            statusText = context.getString(R.string.parent_dashboard_task_status_done);
        } else if (days < 0) {
            bg = R.color.danger_light;
            text = R.color.danger;
            dotColor = R.color.danger;
            statusText = context.getString(R.string.parent_dashboard_task_status_late);
        } else if (urgent) {
            bg = R.color.urgent_light;
            text = R.color.urgent;
            dotColor = R.color.urgent;
            statusText = context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            bg = R.color.regular_task_bg;
            text = R.color.regular_task_text;
            dotColor = R.color.regular_task_dot;
            statusText = context.getString(R.string.parent_dashboard_task_status_waiting);
        }

        tvStatus.setText(statusText);
        tvStatus.setTextColor(context.getColor(text));
        
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(context.getColor(bg));
        chip.setCornerRadius(context.getResources().getDisplayMetrics().density * 14);
        tvStatus.setBackground(chip);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(context.getColor(dotColor));
        viewDot.setBackground(dotBg);

        return convertView;
    }
}
