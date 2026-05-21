package com.example.family_tasks_proj.parent.adapter;

import android.content.Context;
import android.graphics.Bitmap;
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

// אדפטר המציג את המשימות של ההורה ב-RecyclerView
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<AssignedTask> assignedTasks;

    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> assignedTasks) {
        this.context = context;
        this.assignedTasks = assignedTasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ניפוח ה-XML של שורת משימה
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position)
    {
        AssignedTask task = assignedTasks.get(position);

        holder.tvTitle.setText(task.getTitle());
        // הצגת שם הילד לו משויכת המשימה
        holder.tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));

        showTaskImage(holder, task);

        holder.tvDueDate.setText(task.getDueAt());
        holder.tvStatus.setText(getStatusText(task));
    }

    // הצגת תמונת המשימה
    private void showTaskImage(TaskViewHolder holder, AssignedTask task) {
        String imageBase64 = task.getImageBase64();
        Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);

        holder.imageContainer.setVisibility(View.VISIBLE);
        holder.imgTask.setImageBitmap(bitmap);
    }

    // קביעת טקסט הסטטוס (באיחור, דחוף וכו')
    private String getStatusText(AssignedTask task) {
        if (DateUtils.isOverdue(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_late);
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            return context.getString(R.string.parent_dashboard_task_status_waiting);
        }
    }

    @Override
    public int getItemCount() {
        return assignedTasks.size();
    }

    // ViewHolder שמחזיק את רכיבי ה-UI של שורה אחת
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDueDate, tvStatus;
        View imageContainer;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitleCard);
            tvOwner = itemView.findViewById(R.id.tvTaskOwner);
            tvDueDate = itemView.findViewById(R.id.tvDueDateCard);
            tvStatus = itemView.findViewById(R.id.tvStatusChip);
            imageContainer = itemView.findViewById(R.id.imgTaskParentShell);
            imgTask = itemView.findViewById(R.id.imgTaskParent);
        }
    }
}
