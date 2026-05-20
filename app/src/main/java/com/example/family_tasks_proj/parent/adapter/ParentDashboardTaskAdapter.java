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

// Adapter שמקבל רשימת AssignedTask מדשבורד ההורה ומציג כל משימה ככרטיס ב-RecyclerView
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    // ה-Context נחוץ כדי לטעון מחרוזות מ-strings.xml ולנפח את ה-XML של הכרטיס
    private final Context context;

    // רשימת המשימות שהדשבורד הכין מראש ושאותן יש להציג ברשימה
    private final List<AssignedTask> assignedTasks;

    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> assignedTasks) {
        this.context = context;
        this.assignedTasks = assignedTasks;
    }

    // יוצר כרטיס חדש: מנפח את ה-XML של פריט בודד ועוטף אותו ב-ViewHolder
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    // ממלא כרטיס קיים בנתונים של המשימה שבמיקום position ברשימה
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        AssignedTask task = assignedTasks.get(position);

        holder.tvTitle.setText(task.getTitle());

        // המחרוזת task_assigned_to מקבלת פרמטר - שם הילד שאליו שויכה המשימה
        holder.tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));

        // הצגת תמונת המשימה בכרטיס
        showTaskImage(holder, task);

        holder.tvDue.setText(task.getDueAt());
        holder.tvStatus.setText(getStatusText(task));
    }

    // מציג את תמונת המשימה בכרטיס
    private void showTaskImage(TaskViewHolder holder, AssignedTask task) {
        String imageBase64 = task.getImageBase64();
        Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);

        holder.imageContainer.setVisibility(View.VISIBLE);
        holder.imgTask.setImageBitmap(bitmap);
    }

    // קביעת סטטוס למשימה פתוחה: באיחור, דחופה או ממתינה
    private String getStatusText(AssignedTask task) {
        if (DateUtils.isOverdue(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_late);
        } else if (DateUtils.isDueSoon(task.getDueAt())) {
            return context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            return context.getString(R.string.parent_dashboard_task_status_waiting);
        }
    }

    // מספר הכרטיסים שה-RecyclerView יצטרך להציג - לפי אורך הרשימה
    @Override
    public int getItemCount() {
        return assignedTasks.size();
    }

    // ViewHolder - מחזיק הפניות לרכיבי ה-UI של כרטיס אחד.
    // שמירת ההפניות חוסכת קריאות חוזרות ל-findViewById בזמן גלילה ומשפרת ביצועים.
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDue, tvStatus;
        View imageContainer;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitleCard);
            tvOwner = itemView.findViewById(R.id.tvTaskOwner);
            tvDue = itemView.findViewById(R.id.tvDueDateCard);
            tvStatus = itemView.findViewById(R.id.tvStatusChip);
            imageContainer = itemView.findViewById(R.id.imgTaskParentShell);
            imgTask = itemView.findViewById(R.id.imgTaskParent);
        }
    }
}
