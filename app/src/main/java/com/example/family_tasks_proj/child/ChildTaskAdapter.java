package com.example.family_tasks_proj.child;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
 * - מציג כל משימה ככרטיס (CardView) עם כותרת, זמן שנותר, סטטוס, כוכבים, וכפתור "בוצע".
 * - צובע את נקודת הסטטוס: ירוק (בוצע), כתום (דחוף — עד 2 ימים), אפור (רגיל).
 * - כרטיס שבוצע → רקע ירוק בהיר + קו חוצה על הכותרת + כפתור מוסתר.
 * - אנימציית slide-in מימין לכל כרטיס כשמופיע.
 * - לחיצה על "בוצע" → אנימציית celebrate → callback ל-ChildDashboardActivity.
 *
 * Layout: item_child_task.xml
 */
public class ChildTaskAdapter extends RecyclerView.Adapter<ChildTaskAdapter.TaskViewHolder> {

    /** רשימת המשימות להצגה */
    private final List<ChildTask> tasks;

    /** callback — נקרא כשהילד לוחץ "בוצע" על משימה */
    private final OnTaskDoneListener doneListener;

    /**
     * ממשק callback — ChildDashboardActivity מממש אותו כדי לעדכן Firebase.
     */
    public interface OnTaskDoneListener {
        /**
         * נקרא כשהילד לוחץ "בוצע" על משימה
         * @param task המשימה שסומנה כ-done
         * @param position המיקום ברשימה
         */
        void onTaskDone(ChildTask task, int position);
    }

    /**
     * יוצר אדפטר חדש.
     *
     * @param tasks רשימת המשימות — מגיעה מ-Firebase דרך ChildDashboardActivity
     * @param doneListener callback לעדכון Firebase כשמשימה מסומנת כ-done
     */
    public ChildTaskAdapter(List<ChildTask> tasks, OnTaskDoneListener doneListener) {
        this.tasks = tasks;
        this.doneListener = doneListener;
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

        // === אנימציית slide-in: כל כרטיס נכנס מימין בהדרגה ===
        holder.itemView.setTranslationX(300f);
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(350)
                .setStartDelay(position * 60L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // === רקע כרטיס: ירוק בהיר אם בוצע, לבן אחרת ===
        if (holder.itemView instanceof CardView) {
            int bgColor = task.isDone
                    ? Color.parseColor("#E8F5E9")  // ירוק בהיר — בוצע
                    : Color.WHITE;
            ((CardView) holder.itemView).setCardBackgroundColor(bgColor);
        }

        // === כותרת ===
        holder.tvTaskTitle.setText(task.title != null ? task.title : "");

        // קו חוצה אם בוצע + שינוי צבע טקסט
        if (task.isDone) {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#999999"));
        } else {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#1A1A1A"));
        }

        // === זמן שנותר (משתמש ב-DateUtils) ===
        long daysLeft = DateUtils.daysLeft(task.dueAt);

        // טקסט תאריך — עם אמוג'י לפי סטטוס
        String dueText;
        if (task.isDone) {
            dueText = "✅ הושלם";
            holder.tvDueDate.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            dueText = formatDueText(task.dueAt, daysLeft);
            // צבע טקסט לפי דחיפות
            if (daysLeft < 0) {
                holder.tvDueDate.setTextColor(Color.parseColor("#E53935")); // אדום — עבר זמן
            } else if (daysLeft <= 2) {
                dueText = "⚡ דחוף!  " + dueText;
                holder.tvDueDate.setTextColor(Color.parseColor("#FF5722")); // כתום — דחוף
            } else {
                holder.tvDueDate.setTextColor(Color.parseColor("#888888")); // אפור — רגיל
            }
        }
        holder.tvDueDate.setText(dueText);

        // === נקודת סטטוס (עיגול צבעוני) — אותה לוגיקה כמו קודם ===
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

        // === תמונת משימה ===
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

        // === כוכבים ===
        holder.tvTaskStars.setText(task.starsWorth > 0 ? task.starsWorth + " ⭐" : "");

        // === כפתור "בוצע" — מוצג רק אם המשימה עדיין לא הושלמה ===
        if (task.isDone) {
            holder.btnDone.setVisibility(View.GONE);
        } else {
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(v -> {
                if (doneListener == null) return;

                // אנימציית celebrate: הכפתור מתכווץ + הכרטיס נדהה
                // הCallback נקרא רק אחרי שהאנימציה מסתיימת
                holder.btnDone.animate()
                        .scaleX(0f).scaleY(0f)
                        .setDuration(200)
                        .start();
                holder.itemView.animate()
                        .alpha(0.6f)
                        .setDuration(300)
                        .withEndAction(() ->
                                doneListener.onTaskDone(task, holder.getAdapterPosition()))
                        .start();
            });
        }
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
     * @param daysLeft תוצאה מ-DateUtils.daysLeft
     * @return מחרוזת תצוגה בעברית
     */
    private String formatDueText(String dueAt, long daysLeft) {
        if (daysLeft == Long.MAX_VALUE) {
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
        /** כפתור סימון "בוצע" */
        Button btnDone;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            imgTaskImage = itemView.findViewById(R.id.imgTaskImage);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvTaskStars = itemView.findViewById(R.id.tvTaskStars);
            btnDone = itemView.findViewById(R.id.btnDone);
        }
    }
}
