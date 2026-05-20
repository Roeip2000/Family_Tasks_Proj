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

/**
 * Adapter (מתאם) שמחבר בין רשימת המשימות (AssignedTask) לבין כרטיסי התצוגה (item_parent_task)
 * בתוך ה-RecyclerView של דשבורד ההורה.
 */
public class ParentDashboardTaskAdapter extends RecyclerView.Adapter<ParentDashboardTaskAdapter.TaskViewHolder> {

    // הקשר האפליקציה - משמש לטעינת משאבים (כמו מחרוזות) וניפוח ה-XML
    private final Context context;
    // רשימת הנתונים שתיוצג על המסך
    private final List<AssignedTask> items;

    public ParentDashboardTaskAdapter(Context context, List<AssignedTask> items) {
        this.context = context;
        this.items = items;
    }

    /**
     * פונקציה הנקראת כאשר ה-RecyclerView צריך ליצור כרטיס חדש (ViewHolder).
     * כאן אנחנו "מנפחים" (Inflate) את קובץ ה-XML של פריט בודד והופכים אותו לאובייקט Java.
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_parent_task, parent, false);
        return new TaskViewHolder(view);
    }

    /**
     * פונקציה הנקראת כדי להציג נתונים במקום מסוים (Position) ברשימה.
     * היא מקשרת בין אובייקט הנתונים (Task) לבין רכיבי הממשק (UI) שבתוך ה-ViewHolder.
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        AssignedTask task = items.get(position);

        holder.tvTitle.setText(task.getTitle());
        // שימוש ב-Context כדי לגשת למחרוזת עם פרמטר (שם הילד)
        holder.tvOwner.setText(context.getString(R.string.task_assigned_to, task.getChildName()));
        
        // קריאה לפונקציית העזר להצגת התמונה
        showTaskImage(holder, task);
        
        holder.tvDue.setText(task.getDueAt());
        holder.tvStatus.setText(getStatusText(task));
    }

    /**
     * פונקציה המטפלת בהצגת תמונת המשימה (אם קיימת).
     * התמונה שמורה ב-Firebase כטקסט (Base64) ויש להמירה ל-Bitmap כדי להציגה ב-ImageView.
     */
    private void showTaskImage(TaskViewHolder holder, AssignedTask task) {
        String imageBase64 = task.getImageBase64();

        // אם אין תמונה למשימה, נסתיר את רכיב התצוגה כדי שלא יתפוס מקום ריק
        if (imageBase64 == null || imageBase64.isEmpty()) {
            holder.imgShell.setVisibility(View.GONE);
            return;
        }

        // המרת הטקסט חזרה לתמונה (Bitmap) בעזרת מחלקת עזר
        Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);
        if (bitmap == null) {
            holder.imgShell.setVisibility(View.GONE);
            return;
        }

        // הצגת התמונה ושינוי הנראות ל-Visible
        holder.imgShell.setVisibility(View.VISIBLE);
        holder.imgTask.setImageBitmap(bitmap);
    }

    /**
     * פונקציה הקובעת את הטקסט של סטטוס המשימה (בוצע, באיחור, דחוף או בהמתנה)
     * בהתאם למצב המשימה ולתאריך היעד שלה.
     */
    private String getStatusText(AssignedTask task) {
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

    /**
     * מחזירה את כמות הפריטים ברשימה. ה-RecyclerView משתמש בזה כדי לדעת כמה כרטיסים להציג.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * מחלקה פנימית המייצגת "מחזיק תצוגה" (ViewHolder).
     * תפקידה לשמור הפניות לרכיבי ה-UI של כרטיס בודד, כדי להימנע מחיפוש חוזר (findViewById)
     * בכל פעם שכרטיס ממוחזר, מה שמשפר משמעותית את ביצועי הגלילה.
     */
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
