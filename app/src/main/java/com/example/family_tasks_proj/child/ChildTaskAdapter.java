package com.example.family_tasks_proj.child;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.Class_child.ChildTask;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;

import java.util.List;

/**
 * אדפטר ל-RecyclerView של רשימת המשימות בדשבורד הילד.
 *
 * אחריות:
 * - מציג כל משימה ככרטיס (CardView) עם כותרת, זמן שנותר, סטטוס, וכוכבים.
 * - צובע את נקודת הסטטוס: ירוק (בוצע), כתום (דחוף — עד 2 ימים), אפור (רגיל).
 * - מחשב ומציג "היום", "מחר", או את התאריך — לפי הפרש ימים מהיום.
 * - משימה שבוצעה מקבלת קו חוצה על הכותרת.
 *
 * Layout: item_child_task.xml
 *
 * ===== שימוש =====
 * נוצר ב-ChildDashboardActivity ומוגדר ל-rvTasks (RecyclerView).
 */
public class ChildTaskAdapter extends RecyclerView.Adapter<ChildTaskAdapter.TaskViewHolder> {

    /** רשימת המשימות להצגה */
    private final List<ChildTask> tasks;

    /**
     * יוצר אדפטר חדש.
     *
     * @param tasks רשימת המשימות — מגיעה מ-Firebase דרך ChildDashboardActivity
     */
    public ChildTaskAdapter(List<ChildTask> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יוצר View מה-layout של כרטיס משימה בודד
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ChildTask task = tasks.get(position);

        // --- כותרת ---
        holder.tvTaskTitle.setText(task.title != null ? task.title : "");

        // קו חוצה אם בוצע
        if (task.isDone) {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#999999"));
        } else {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#222222"));
        }

        // --- זמן שנותר (משתמש ב-DateUtils למניעת שכפול) ---
        long daysLeft = DateUtils.daysLeft(task.dueAt);
        String dueText = formatDueText(task.dueAt, daysLeft);
        holder.tvDueDate.setText(dueText);

        // צבע טקסט: אדום אם עבר, כתום אם דחוף, אפור אם רגיל
        if (daysLeft < 0 && !task.isDone) {
            holder.tvDueDate.setTextColor(Color.parseColor("#E53935")); // אדום — עבר זמן
        } else if (daysLeft >= 0 && daysLeft <= 2 && !task.isDone) {
            holder.tvDueDate.setTextColor(Color.parseColor("#FF9800")); // כתום — דחוף
        } else {
            holder.tvDueDate.setTextColor(Color.parseColor("#888888")); // אפור — רגיל
        }

        // --- נקודת סטטוס (עיגול צבעוני) ---
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(14, 14);

        if (task.isDone) {
            dot.setColor(Color.parseColor("#4CAF50")); // ירוק — בוצע
        } else if (daysLeft >= 0 && daysLeft <= 2) {
            dot.setColor(Color.parseColor("#FF9800")); // כתום — דחוף
        } else if (daysLeft < 0) {
            dot.setColor(Color.parseColor("#E53935")); // אדום — עבר זמן
        } else {
            dot.setColor(Color.parseColor("#BDBDBD")); // אפור — רגיל
        }
        holder.viewStatusDot.setBackground(dot);

        // --- תמונת משימה ---
        if (task.imageBase64 != null && !task.imageBase64.isEmpty()) {
            Bitmap bmp = ImageHelper.base64ToBitmap(task.imageBase64);
            if (bmp != null) {
                holder.imgTaskImage.setImageBitmap(bmp);
                holder.imgTaskImage.setVisibility(View.VISIBLE);
            } else {
                holder.imgTaskImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgTaskImage.setVisibility(View.GONE);
        }

        // --- כוכבים ---
        holder.tvTaskStars.setText(task.starsWorth > 0 ? task.starsWorth + " ⭐" : "");
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    // =====================================================================
    //  עיצוב טקסט זמן שנותר
    // =====================================================================

    /**
     * מחזיר טקסט תצוגה לזמן שנותר.
     * "היום", "מחר", "עוד X ימים", "איחור (X ימים)", או התאריך עצמו.
     *
     * @param dueAt תאריך בפורמט "d/M/yyyy"
     * @param daysLeft תוצאה מ-calcDaysLeft
     * @return מחרוזת תצוגה בעברית
     */
    private String formatDueText(String dueAt, long daysLeft) {
        if (daysLeft == Long.MAX_VALUE) {
            // אין תאריך יעד
            return "ללא תאריך";
        }
        if (daysLeft < 0) {
            return "איחור (" + Math.abs(daysLeft) + " ימים)";
        }
        if (daysLeft == 0) {
            return "היום";
        }
        if (daysLeft == 1) {
            return "מחר";
        }
        if (daysLeft <= 7) {
            return "עוד " + daysLeft + " ימים";
        }
        // יותר משבוע — מציג תאריך
        return dueAt;
    }

    // =====================================================================
    //  ViewHolder
    // =====================================================================

    /**
     * ViewHolder — מחזיק references ל-Views בתוך כרטיס משימה בודד.
     * מקושר ל-item_child_task.xml.
     */
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        /** נקודת סטטוס צבעונית */
        View viewStatusDot;
        /** תמונת המשימה */
        ImageView imgTaskImage;
        /** כותרת המשימה */
        TextView tvTaskTitle;
        /** זמן שנותר / תאריך יעד */
        TextView tvDueDate;
        /** כמות כוכבים */
        TextView tvTaskStars;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            imgTaskImage = itemView.findViewById(R.id.imgTaskImage);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvTaskStars = itemView.findViewById(R.id.tvTaskStars);
        }
    }
}
