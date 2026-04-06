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
import com.example.family_tasks_proj.child.model.ChildTask;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;

import java.util.List;

/**
 * אדפטר למשימות הילד — מציג כרטיס לכל משימה עם כותרת, תאריך, כוכבים וכפתור "בוצע".
 * משימה שהושלמה מקבלת קו חוצה ורקע ירוק.
 * משימה דחופה מודגשת בכתום/אדום לפי ימים שנשארו.
 */
public class ChildTaskAdapter extends RecyclerView.Adapter<ChildTaskAdapter.TaskViewHolder> {

    private final List<ChildTask> tasks;
    private final OnTaskDoneListener doneListener;

    public interface OnTaskDoneListener {
        void onTaskDone(ChildTask task);
    }

    public ChildTaskAdapter(List<ChildTask> tasks, OnTaskDoneListener doneListener) {
        this.tasks = tasks;
        this.doneListener = doneListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ChildTask task = tasks.get(position);
        long daysLeft = DateUtils.daysLeft(task.dueAt);

        animateEntry(holder, position);
        bindCardBackground(holder, task);
        bindTitle(holder, task);
        bindDueDate(holder, task, daysLeft);
        bindStatusDot(holder, task, daysLeft);
        bindTaskImage(holder, task);
        bindStars(holder, task);
        bindDoneButton(holder, task);
    }

    // אנימציית כניסה — כל שורה נכנסת מימין עם השהייה קטנה
    private void animateEntry(TaskViewHolder holder, int position) {
        holder.itemView.animate().cancel();
        holder.btnDone.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationX(0f);
        holder.btnDone.setScaleX(1f);
        holder.btnDone.setScaleY(1f);

        holder.itemView.setTranslationX(300f);
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(350)
                .setStartDelay(position * 60L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // משימה שבוצעה מקבלת רקע ירוק
    private void bindCardBackground(TaskViewHolder holder, ChildTask task) {
        if (holder.itemView instanceof CardView) {
            int bgColor = task.isDone ? Color.parseColor("#E8F5E9") : Color.WHITE;
            ((CardView) holder.itemView).setCardBackgroundColor(bgColor);
        }
    }

    // כותרת המשימה — עם קו חוצה אם בוצעה
    private void bindTitle(TaskViewHolder holder, ChildTask task) {
        holder.tvTaskTitle.setText(task.title != null ? task.title : "");

        if (task.isDone) {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#999999"));
        } else {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.parseColor("#1A1A1A"));
        }
    }

    // תאריך יעד — צבע משתנה לפי דחיפות
    private void bindDueDate(TaskViewHolder holder, ChildTask task, long daysLeft) {
        String dueText;
        if (task.isDone) {
            dueText = holder.itemView.getContext().getString(R.string.child_due_done);
            holder.tvDueDate.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            dueText = formatDueText(holder, task.dueAt, daysLeft);
            if (daysLeft < 0) {
                holder.tvDueDate.setTextColor(Color.parseColor("#E53935"));
            } else if (daysLeft <= 2) {
                dueText = holder.itemView.getContext().getString(R.string.child_due_urgent_prefix, dueText);
                holder.tvDueDate.setTextColor(Color.parseColor("#FF5722"));
            } else {
                holder.tvDueDate.setTextColor(Color.parseColor("#888888"));
            }
        }
        holder.tvDueDate.setText(dueText);
    }

    // נקודת סטטוס צבעונית — ירוק/כתום/אדום/אפור
    private void bindStatusDot(TaskViewHolder holder, ChildTask task, long daysLeft) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(14, 14);

        if (task.isDone) {
            dot.setColor(Color.parseColor("#4CAF50"));
        } else if (daysLeft >= 0 && daysLeft <= 2) {
            dot.setColor(Color.parseColor("#FF9800"));
        } else if (daysLeft < 0) {
            dot.setColor(Color.parseColor("#E53935"));
        } else {
            dot.setColor(Color.parseColor("#BDBDBD"));
        }
        holder.viewStatusDot.setBackground(dot);
    }

    // תמונת משימה — מוצגת רק אם קיימת
    private void bindTaskImage(TaskViewHolder holder, ChildTask task) {
        if (task.imageBase64 != null && !task.imageBase64.isEmpty()) {
            Bitmap bmp = ImageHelper.base64ToBitmap(task.imageBase64);
            if (bmp != null) {
                holder.imgTaskImage.setImageBitmap(bmp);
                holder.imgTaskImage.setVisibility(View.VISIBLE);
                return;
            }
        }
        holder.imgTaskImage.setImageDrawable(null);
        holder.imgTaskImage.setVisibility(View.GONE);
    }

    // כוכבים — מוצגים רק אם יש ערך חיובי
    private void bindStars(TaskViewHolder holder, ChildTask task) {
        holder.tvTaskStars.setText(task.starsWorth > 0
                ? holder.itemView.getContext().getString(R.string.child_stars_worth, task.starsWorth)
                : "");
    }

    // כפתור "בוצע" — מוצג רק למשימות פתוחות, עם אנימציה בלחיצה
    private void bindDoneButton(TaskViewHolder holder, ChildTask task) {
        if (task.isDone) {
            holder.btnDone.setVisibility(View.GONE);
            holder.btnDone.setOnClickListener(null);
            return;
        }

        holder.btnDone.setVisibility(View.VISIBLE);
        holder.btnDone.setOnClickListener(v -> {
            if (doneListener == null) return;

            holder.btnDone.animate()
                    .scaleX(0f).scaleY(0f)
                    .setDuration(200)
                    .start();
            holder.itemView.animate()
                    .alpha(0.6f)
                    .setDuration(300)
                    .withEndAction(() -> doneListener.onTaskDone(task))
                    .start();
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private String formatDueText(@NonNull TaskViewHolder holder, String dueAt, long daysLeft) {
        if (daysLeft == Long.MAX_VALUE) {
            return holder.itemView.getContext().getString(R.string.child_due_no_date);
        }
        if (daysLeft < 0) {
            return holder.itemView.getContext().getString(R.string.child_due_late, Math.abs(daysLeft));
        }
        if (daysLeft == 0) {
            return holder.itemView.getContext().getString(R.string.child_due_today);
        }
        if (daysLeft == 1) {
            return holder.itemView.getContext().getString(R.string.child_due_tomorrow);
        }
        if (daysLeft <= 7) {
            return holder.itemView.getContext().getString(R.string.child_due_days_left, daysLeft);
        }
        return dueAt;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStatusDot;
        ImageView imgTaskImage;
        TextView tvTaskTitle;
        TextView tvDueDate;
        TextView tvTaskStars;
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
