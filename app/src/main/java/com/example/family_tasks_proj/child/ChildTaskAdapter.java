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
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.model.ChildTask;
import com.example.family_tasks_proj.util.DateUtils;
import com.example.family_tasks_proj.util.ImageHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/** מתאם עבור רשימת המשימות של הילד. מציג כרטיס לכל משימה עם אפשרות לסמן כבוצע. */
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
        return new TaskViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        final ChildTask task = tasks.get(position);
        long days = DateUtils.daysLeft(task.getDueAt());
        boolean dueSoon = DateUtils.isDueSoon(task.getDueAt());
        android.content.Context ctx = holder.itemView.getContext();

        // עיצוב הכרטיס לפי סטטוס המשימה
        if (holder.itemView instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) holder.itemView;
            int bgColor;
            int strokeColor;

            if (task.getIsDone()) {
                bgColor = R.color.primary_light;
                strokeColor = R.color.divider;
            } else if (days < 0) {
                bgColor = R.color.danger_light;
                strokeColor = R.color.danger;
            } else if (dueSoon) {
                bgColor = R.color.urgent_light;
                strokeColor = R.color.urgent;
            } else {
                bgColor = R.color.bg_card;
                strokeColor = R.color.border_light;
            }

            card.setCardBackgroundColor(ctx.getColor(bgColor));
            card.setStrokeColor(ctx.getColor(strokeColor));
            card.setStrokeWidth(2);
        }

        holder.tvTitle.setText(task.getTitle());
        if (task.getIsDone()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_hint));
            holder.tvDue.setText(ctx.getString(R.string.child_due_done));
            holder.tvDue.setTextColor(ctx.getColor(R.color.success));
            holder.btnDone.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_primary));
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doneListener.onTaskDone(task);
                }
            });
            
            String dueText = formatDueText(ctx, task.getDueAt(), days);
            holder.tvDue.setText(dueText);
            int dueTextColor;
            if (days < 0) {
                dueTextColor = R.color.danger;
            } else if (dueSoon) {
                dueTextColor = R.color.urgent;
            } else {
                dueTextColor = R.color.text_secondary;
            }
            holder.tvDue.setTextColor(ctx.getColor(dueTextColor));
        }

        // נקודת סטטוס צבעונית
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(14, 14);
        int dotColor;
        if (task.getIsDone()) {
            dotColor = R.color.success;
        } else if (days < 0) {
            dotColor = R.color.danger;
        } else if (dueSoon) {
            dotColor = R.color.urgent;
        } else {
            dotColor = R.color.text_hint;
        }
        dot.setColor(ctx.getColor(dotColor));
        holder.viewStatusDot.setBackground(dot);

        // הצגת תמונה אם קיימת במשימה
        String imageBase64 = task.getImageBase64();
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            Bitmap bitmap = ImageHelper.base64ToBitmap(imageBase64);
            if (bitmap != null) {
                holder.imgShell.setVisibility(View.VISIBLE);
                holder.imgTask.setVisibility(View.VISIBLE);
                holder.imgTask.setImageBitmap(bitmap);
            } else {
                holder.imgShell.setVisibility(View.GONE);
                holder.imgTask.setVisibility(View.GONE);
            }
        } else {
            holder.imgShell.setVisibility(View.GONE);
            holder.imgTask.setVisibility(View.GONE);
        }

        // הצגת כמות כוכבים
        int stars = (int) task.getStarsWorth();
        if (stars <= 0) stars = 10; // ברירת מחדל
        holder.tvStars.setText(ctx.getString(R.string.child_stars_worth, stars));
        holder.tvStars.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private String formatDueText(android.content.Context ctx, String dueAt, long days) {
        if (days == Long.MAX_VALUE) {
            return ctx.getString(R.string.child_due_no_date);
        }
        if (days < 0) {
            return ctx.getString(R.string.child_due_late, (int) Math.abs(days));
        }
        if (days == 0) {
            return ctx.getString(R.string.child_due_today);
        }
        if (days == 1) {
            return ctx.getString(R.string.child_due_tomorrow);
        }
        if (days <= 7) {
            return ctx.getString(R.string.child_due_days_left, (int) days);
        }
        return dueAt;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStatusDot;
        View imgShell;
        ImageView imgTask;
        TextView tvTitle;
        TextView tvDue;
        TextView tvStars;
        Button btnDone;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            imgShell = itemView.findViewById(R.id.imgTaskImageShell);
            imgTask = itemView.findViewById(R.id.imgTaskImage);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDue = itemView.findViewById(R.id.tvDueDate);
            tvStars = itemView.findViewById(R.id.tvTaskStars);
            btnDone = itemView.findViewById(R.id.btnDone);
        }
    }
}
