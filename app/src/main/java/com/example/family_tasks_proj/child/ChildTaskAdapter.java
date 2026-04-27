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

// כרטיסי משימות לילד — סטטוס, תאריך וכפתור "בוצע"
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
        long daysLeft = DateUtils.daysLeft(task.getDueAt());
        android.content.Context ctx = holder.itemView.getContext();

        if (holder.itemView instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) holder.itemView;
            int bgColor = ctx.getColor(task.getIsDone() ? R.color.surface_soft_green : (daysLeft < 0 ? R.color.danger_light : (daysLeft <= 2 ? R.color.surface_soft_orange : R.color.bg_card)));
            int strokeColor = ctx.getColor(task.getIsDone() ? R.color.accent_light : (daysLeft < 0 ? R.color.urgent : (daysLeft <= 2 ? R.color.urgent_light : R.color.border_light)));
            card.setCardBackgroundColor(bgColor);
            card.setStrokeColor(strokeColor);
            card.setStrokeWidth(1);
        }

        holder.tvTaskTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        if (task.getIsDone()) {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(ctx.getColor(R.color.text_hint));
        } else {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(ctx.getColor(R.color.text_primary));
        }

        if (task.getIsDone()) {
            holder.tvDueDate.setText(ctx.getString(R.string.child_due_done));
            holder.tvDueDate.setTextColor(ctx.getColor(R.color.success_dark));
        } else {
            String dueText = formatDueText(holder, task.getDueAt(), daysLeft);
            if (daysLeft < 0) {
                holder.tvDueDate.setTextColor(ctx.getColor(R.color.danger));
            } else if (daysLeft <= 2) {
                dueText = ctx.getString(R.string.child_due_urgent_prefix, dueText);
                holder.tvDueDate.setTextColor(ctx.getColor(R.color.warning_dark));
            } else {
                holder.tvDueDate.setTextColor(ctx.getColor(R.color.text_secondary));
            }
            holder.tvDueDate.setText(dueText);
        }

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(14, 14);
        dot.setColor(ctx.getColor(task.getIsDone() ? R.color.success_dark : (daysLeft < 0 ? R.color.danger : (daysLeft <= 2 ? R.color.warning_dark : R.color.text_hint))));
        holder.viewStatusDot.setBackground(dot);

        if (task.getImageBase64() != null && !task.getImageBase64().isEmpty()) {
            Bitmap bmp = ImageHelper.base64ToBitmap(task.getImageBase64());
            if (bmp != null) {
                holder.imgTaskImageShell.setVisibility(View.VISIBLE);
                holder.imgTaskImage.setImageBitmap(bmp);
                holder.imgTaskImage.setVisibility(View.VISIBLE);
            } else {
                holder.imgTaskImageShell.setVisibility(View.GONE);
                holder.imgTaskImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgTaskImageShell.setVisibility(View.GONE);
            holder.imgTaskImage.setVisibility(View.GONE);
        }

        if (task.getStarsWorth() > 0) {
            holder.tvTaskStars.setText(ctx.getString(R.string.child_stars_worth, task.getStarsWorth()));
            holder.tvTaskStars.setVisibility(View.VISIBLE);
        } else {
            holder.tvTaskStars.setVisibility(View.GONE);
        }

        if (task.getIsDone()) {
            holder.btnDone.setVisibility(View.GONE);
            holder.btnDone.setOnClickListener(null);
        } else {
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (doneListener != null) doneListener.onTaskDone(task);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private String formatDueText(@NonNull TaskViewHolder holder, String dueAt, long daysLeft) {
        android.content.Context ctx = holder.itemView.getContext();
        if (daysLeft == Long.MAX_VALUE) {
            return ctx.getString(R.string.child_due_no_date);
        }
        if (daysLeft < 0) {
            return ctx.getString(R.string.child_due_late, Math.abs(daysLeft));
        }
        if (daysLeft == 0) {
            return ctx.getString(R.string.child_due_today);
        }
        if (daysLeft == 1) {
            return ctx.getString(R.string.child_due_tomorrow);
        }
        if (daysLeft <= 7) {
            return ctx.getString(R.string.child_due_days_left, daysLeft);
        }
        return dueAt;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStatusDot;
        View imgTaskImageShell;
        ImageView imgTaskImage;
        TextView tvTaskTitle;
        TextView tvDueDate;
        TextView tvTaskStars;
        Button btnDone;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            imgTaskImageShell = itemView.findViewById(R.id.imgTaskImageShell);
            imgTaskImage = itemView.findViewById(R.id.imgTaskImage);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvTaskStars = itemView.findViewById(R.id.tvTaskStars);
            btnDone = itemView.findViewById(R.id.btnDone);
        }
    }
}
