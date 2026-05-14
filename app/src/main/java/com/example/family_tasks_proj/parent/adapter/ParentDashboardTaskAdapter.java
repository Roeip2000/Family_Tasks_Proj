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

/** מתאם (Adapter) עבור רשימת המשימות בדשבורד ההורה. */
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<AssignedTask> items; // רשימת המשימות להצגה
    private boolean showChildName = false;
    private OnItemClickListener listener;

    // ממשק לטיפול בלחיצות על פריט ברשימה
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
        // ניפוח (Inflate) של ה-Layout עבור פריט בודד ברשימה
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // חיבור הנתונים של משימה ספציפית לרכיבי התצוגה (ViewHolder)
        AssignedTask task = items.get(position);
        
        // הגדרת כותרת המשימה
        String title = task.getTitle();
        if (title == null || title.isEmpty()) {
            title = context.getString(R.string.task_default_title);
        }
        holder.tvTitle.setText(title);

        // הצגת שם הילד אם הוגדר
        if (showChildName) {
            String childName = task.getChildName();
            if (childName == null || childName.isEmpty()) {
                childName = context.getString(R.string.default_child_name);
            }
            holder.tvOwner.setText(context.getString(R.string.task_assigned_to, childName));
            holder.tvOwner.setVisibility(View.VISIBLE);
        } else {
            holder.tvOwner.setVisibility(View.GONE);
        }
        
        // הצגת תמונה אם קיימת (Base64)
        String base64Image = task.getImageBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
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

        // חישוב ימים שנותרו וסטטוס המשימה
        long daysLeft = DateUtils.daysLeft(task.getDueAt());
        boolean isUrgent = DateUtils.isDueSoon(task.getDueAt());

        // קביעת טקסט התאריך וצבעו
        String datePrefix;
        int dateColor;

        if (task.getIsDone()) {
            datePrefix = context.getString(R.string.task_due_done_prefix);
            dateColor = R.color.success;
        } else if (daysLeft < 0) {
            datePrefix = context.getString(R.string.task_due_overdue_prefix);
            dateColor = R.color.danger;
        } else if (isUrgent) {
            datePrefix = context.getString(R.string.task_due_urgent_prefix);
            dateColor = R.color.urgent;
        } else {
            datePrefix = context.getString(R.string.task_due_regular_prefix);
            dateColor = R.color.regular_due;
        }

        holder.tvDue.setText(context.getString(R.string.task_due_display, datePrefix, task.getDueAt()));
        holder.tvDue.setTextColor(context.getColor(dateColor));

        // קביעת עיצוב הסטטוס (Status Chip)
        int statusBgColor, statusTextColor, dotColor;
        String statusText;

        if (task.getIsDone()) {
            statusBgColor = R.color.success_light;
            statusTextColor = R.color.success;
            dotColor = R.color.success;
            statusText = context.getString(R.string.parent_dashboard_task_status_done);
        } else if (daysLeft < 0) {
            statusBgColor = R.color.danger_light;
            statusTextColor = R.color.danger;
            dotColor = R.color.danger;
            statusText = context.getString(R.string.parent_dashboard_task_status_late);
        } else if (isUrgent) {
            statusBgColor = R.color.urgent_light;
            statusTextColor = R.color.urgent;
            dotColor = R.color.urgent;
            statusText = context.getString(R.string.parent_dashboard_task_status_urgent);
        } else {
            statusBgColor = R.color.regular_task_bg;
            statusTextColor = R.color.regular_task_text;
            dotColor = R.color.regular_task_dot;
            statusText = context.getString(R.string.parent_dashboard_task_status_waiting);
        }

        // עדכון התצוגה של הסטטוס
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(context.getColor(statusTextColor));
        
        // יצירת רקע מעוגל לסטטוס בצורה דינמית
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(context.getColor(statusBgColor));
        shape.setCornerRadius(28f); // עיגול פינות
        holder.tvStatus.setBackground(shape);

        // עדכון צבע הנקודה
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(context.getColor(dotColor));
        holder.viewDot.setBackground(dot);

        // הגדרת מאזין ללחיצה על כל הפריט
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(task, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder - מחלקה שמחזיקה את כל רכיבי ה-View של פריט אחד
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
