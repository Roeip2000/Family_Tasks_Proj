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

// Adapter שמחבר בין רשימת המשימות לבין ה-RecyclerView בדשבורד ההורה
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    // context דרוש כדי לגשת ל-layout, צבעים וטקסטים מתוך resources
    private final Context context;

    // הרשימה שמכילה את המשימות הפתוחות שמוצגות להורה
    private final List<AssignedTask> assignedTasks;

    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> assignedTasks) {
        this.context = context;
        this.assignedTasks = assignedTasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת כרטיס משימה חדש מתוך קובץ ה-XML של פריט ברשימה
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // קבלת המשימה לפי המיקום שלה ברשימה
        AssignedTask task = assignedTasks.get(position);

        // הצגת פרטי המשימה בכרטיס
        holder.tvTitle.setText(task.getTitle());
        holder.tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));

        showTaskImage(holder, task);

        holder.tvDueDate.setText(task.getDueAt());
        
        // קביעת סטטוס המשימה לפי תאריך היעד
        if (DateUtils.isOverdue(task.getDueAt())) {
            holder.tvStatus.setText(context.getString(R.string.parent_dashboard_task_status_late));
            holder.tvStatus.setTextColor(context.getColor(R.color.danger));
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            holder.tvStatus.setText(context.getString(R.string.parent_dashboard_task_status_urgent));
            holder.tvStatus.setTextColor(context.getColor(R.color.urgent));
        } else {
            holder.tvStatus.setText(context.getString(R.string.parent_dashboard_task_status_waiting));
            holder.tvStatus.setTextColor(context.getColor(R.color.text_secondary));
        }
    }

    private void showTaskImage(TaskViewHolder holder, AssignedTask task) {
        // התמונה נשמרת ב-Firebase כטקסט Base64, וכאן ממירים אותה חזרה ל-Bitmap
        String imageBase64 = task.getImageBase64();
        Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);
        holder.imageContainer.setVisibility(View.VISIBLE);
        holder.imgTask.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        // RecyclerView צריך לדעת כמה פריטים יש כדי להציג את כולם
        return assignedTasks.size();
    }

    // ViewHolder שומר הפניות לרכיבי ה-XML של כרטיס משימה אחד
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDueDate, tvStatus;
        View imageContainer;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // חיבור רכיבי הכרטיס מה-XML לקוד
            tvTitle = itemView.findViewById(R.id.tvTaskTitleCard);
            tvOwner = itemView.findViewById(R.id.tvTaskOwner);
            tvDueDate = itemView.findViewById(R.id.tvDueDateCard);
            tvStatus = itemView.findViewById(R.id.tvStatusChip);
            imageContainer = itemView.findViewById(R.id.imgTaskParentShell);
            imgTask = itemView.findViewById(R.id.imgTaskParent);
        }
    }
}
