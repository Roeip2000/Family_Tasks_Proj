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

// Adapter שמציג את משימות ההורה בתוך RecyclerView
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<AssignedTask> items;

    // קבלת רשימת המשימות שתוצג בדשבורד
    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> items) {
        this.context = context;
        this.items = items;
    }

    // יצירת כרטיס משימה מתוך קובץ XML
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    // הכנסת נתוני משימה אחת לתוך הכרטיס
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        AssignedTask task = items.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));

        showTaskImage(holder, task);

        holder.tvDue.setText(task.getDueAt());
        holder.tvStatus.setText(getStatusText(task));
    }

    // הצגת תמונת המשימה אם קיימת
    private void showTaskImage(TaskViewHolder holder, AssignedTask task) {
        String base64Image = task.getImageBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            // התמונה נשמרת כ-Base64 ולכן ממירים אותה חזרה ל-Bitmap להצגה
            Bitmap bitmap = ImageHelper.base64ToBitmap(base64Image);
            if (bitmap != null) {
                holder.imgShell.setVisibility(View.VISIBLE);
                holder.imgTask.setImageBitmap(bitmap);
            } else {
                holder.imgShell.setVisibility(View.GONE);
            }
        } else {
            holder.imgShell.setVisibility(View.GONE);
        }
    }

    // קביעת סטטוס המשימה לפי ביצוע ותאריך יעד
    private String getStatusText(AssignedTask task) {
        // קודם בודקים אם המשימה בוצעה, ורק אחר כך בודקים תאריך
        if (task.getIsDone()) {
            return context.getString(R.string.parent_dashboard_task_status_done);
        } else if (DateUtils.isOverdue(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_late);
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            return context.getString(R.string.parent_dashboard_task_status_waiting);
        }
    }

    // מחזיר כמה משימות יש ברשימה
    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder שומר הפניות לרכיבים של כרטיס משימה אחד
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDue, tvStatus;
        View imgShell;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitleCard);
            tvOwner = itemView.findViewById(R.id.tvTaskOwner);
            tvDue = itemView.findViewById(R.id.tvDueDateCard);
            tvStatus = itemView.findViewById(R.id.tvStatusChip);
            imgShell = itemView.findViewById(R.id.imgTaskParentShell);
            imgTask = itemView.findViewById(R.id.imgTaskParent);
        }
    }
}
