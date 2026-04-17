package com.example.family_tasks_proj.child;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        bindCardBackground(holder, task);
        bindTitle(holder, task);
        bindDueDate(holder, task, daysLeft);
        bindStatusDot(holder, task, daysLeft);
        bindTaskImage(holder, task);
        bindStars(holder, task);
        bindDoneButton(holder, task);
    }

    private void bindCardBackground(TaskViewHolder holder, ChildTask task) {
        if (holder.itemView instanceof CardView) {
            int bgColor = holder.itemView.getContext().getColor(
                    task.isDone ? R.color.surface_soft_green : R.color.bg_card);
            ((CardView) holder.itemView).setCardBackgroundColor(bgColor);
        }
    }

    // כותרת המשימה — עם קו חוצה אם בוצעה
    private void bindTitle(TaskViewHolder holder, ChildTask task) {
        holder.tvTaskTitle.setText(task.title != null ? task.title : "");

        if (task.isDone) {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(holder.itemView.getContext().getColor(R.color.text_hint));
        } else {
            holder.tvTaskTitle.setPaintFlags(
                    holder.tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
        }
    }

    // תאריך יעד — צבע משתנה לפי דחיפות
    private void bindDueDate(TaskViewHolder holder, ChildTask task, long daysLeft) {
        String dueText;
        if (task.isDone) {
            dueText = holder.itemView.getContext().getString(R.string.child_due_done);
            holder.tvDueDate.setTextColor(holder.itemView.getContext().getColor(R.color.success_dark));
        } else {
            dueText = formatDueText(holder, task.dueAt, daysLeft);
            if (daysLeft < 0) {
                holder.tvDueDate.setTextColor(holder.itemView.getContext().getColor(R.color.danger));
            } else if (daysLeft <= 2) {
                dueText = holder.itemView.getContext().getString(R.string.child_due_urgent_prefix, dueText);
                holder.tvDueDate.setTextColor(holder.itemView.getContext().getColor(R.color.warning_dark));
            } else {
                holder.tvDueDate.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
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
            dot.setColor(holder.itemView.getContext().getColor(R.color.success_dark));
        } else if (daysLeft >= 0 && daysLeft <= 2) {
            dot.setColor(holder.itemView.getContext().getColor(R.color.warning_dark));
        } else if (daysLeft < 0) {
            dot.setColor(holder.itemView.getContext().getColor(R.color.danger));
        } else {
            dot.setColor(holder.itemView.getContext().getColor(R.color.text_hint));
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
        if (task.starsWorth > 0) {
            holder.tvTaskStars.setText(holder.itemView.getContext()
                    .getString(R.string.child_stars_worth, task.starsWorth));
            holder.tvTaskStars.setVisibility(View.VISIBLE);
        } else {
            holder.tvTaskStars.setText("");
            holder.tvTaskStars.setVisibility(View.GONE);
        }
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
            if (doneListener != null) {
                doneListener.onTaskDone(task);
            }
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
