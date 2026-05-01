package com.example.family_tasks_proj.parent.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.models.AssignedTask;
import com.example.family_tasks_proj.utils.DateUtils;
import com.example.family_tasks_proj.utils.ImageHelper;

import java.util.List;

/** מתאם עבור רשימת המשימות בדשבורד ההורה (RecyclerView). */
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<AssignedTask> items;
    private boolean showChildName = false;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AssignedTask task, int position);
    }

    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> items) {
        this.context = context;
        this.items = items;
    }

    public void setShowChildName(boolean showChildName) {
        this.showChildName = showChildName;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        AssignedTask task = items.get(position);
        
        String titleText = task.getTitle();
        if (titleText == null || titleText.isEmpty()) {
            titleText = context.getString(R.string.task_default_title);
        }
        holder.tvTitle.setText(titleText);

        if (showChildName) {
            String ownerName = task.getChildName();
            if (ownerName == null || ownerName.trim().isEmpty()) {
                ownerName = context.getString(R.string.default_child_name);
            }
            holder.tvOwner.setText(context.getString(R.string.task_assigned_to, ownerName));
            holder.tvOwner.setVisibility(View.VISIBLE);
        } else {
            holder.tvOwner.setVisibility(View.GONE);
        }
        
        String base64 = task.getImageBase64();
        if (base64 != null && !base64.trim().isEmpty()) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(base64);
            if (bitmap != null) {
                holder.imgShell.setVisibility(View.VISIBLE);
                holder.imgTask.setImageBitmap(bitmap);
            } else {
                holder.imgShell.setVisibility(View.GONE);
            }
        } else {
            holder.imgShell.setVisibility(View.GONE);
        }

        long days = DateUtils.daysLeft(task.getDueAt());
        boolean urgent = DateUtils.isDueSoon(task.getDueAt());

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
        holder.tvDue.setText(context.getString(R.string.task_due_display, duePrefix, task.getDueAt()));
        holder.tvDue.setTextColor(context.getColor(dueTextColor));

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

        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(context.getColor(text));
        
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(context.getColor(bg));
        chip.setCornerRadius(context.getResources().getDisplayMetrics().density * 14);
        holder.tvStatus.setBackground(chip);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(context.getColor(dotColor));
        holder.viewDot.setBackground(dotBg);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    // המשתמש הקליד על הפריט - פותח אפשרויות ניהול ב-Activity
                    listener.onItemClick(task, holder.getBindingAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDue, tvStatus;
        View viewDot, imgShell;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitleCard);
            tvOwner = itemView.findViewById(R.id.tvTaskOwner);
            tvDue = itemView.findViewById(R.id.tvDueDateCard);
            tvStatus = itemView.findViewById(R.id.tvStatusChip);
            viewDot = itemView.findViewById(R.id.viewTaskDot);
            imgShell = itemView.findViewById(R.id.imgTaskParentShell);
            imgTask = itemView.findViewById(R.id.imgTaskParent);
        }
    }
}
