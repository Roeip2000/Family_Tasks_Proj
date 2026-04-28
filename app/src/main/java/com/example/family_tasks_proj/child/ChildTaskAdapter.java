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

    public interface OnTaskDoneListener { void onTaskDone(ChildTask task); }

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
        android.content.Context ctx = holder.itemView.getContext();

        // עיצוב הכרטיס לפי סטטוס המשימה
        if (holder.itemView instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) holder.itemView;
            int bg = ctx.getColor(task.getIsDone() ? R.color.surface_soft_green : (days < 0 ? R.color.danger_light : (DateUtils.isDueSoon(task.getDueAt()) ? R.color.surface_soft_orange : R.color.bg_card)));
            int stroke = ctx.getColor(task.getIsDone() ? R.color.accent_light : (days < 0 ? R.color.urgent : (DateUtils.isDueSoon(task.getDueAt()) ? R.color.urgent_light : R.color.border_light)));
            card.setCardBackgroundColor(bg); card.setStrokeColor(stroke); card.setStrokeWidth(2);
        }

        holder.tvTitle.setText(task.getTitle());
        if (task.getIsDone()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_hint));
            holder.tvDue.setText(ctx.getString(R.string.child_due_done));
            holder.tvDue.setTextColor(ctx.getColor(R.color.success_dark));
            holder.btnDone.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_primary));
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { doneListener.onTaskDone(task); } });
            
            String dueText = formatDueText(ctx, task.getDueAt(), days);
            holder.tvDue.setText(dueText);
            holder.tvDue.setTextColor(ctx.getColor(days < 0 ? R.color.danger : (DateUtils.isDueSoon(task.getDueAt()) ? R.color.warning_dark : R.color.text_secondary)));
        }

        // נקודת סטטוס צבעונית
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL); dot.setSize(14, 14);
        dot.setColor(ctx.getColor(task.getIsDone() ? R.color.success_dark : (days < 0 ? R.color.danger : (DateUtils.isDueSoon(task.getDueAt()) ? R.color.warning_dark : R.color.text_hint))));
        holder.viewStatusDot.setBackground(dot);

        // הצגת תמונה אם קיימת
        if (task.getImageBase64() != null && !task.getImageBase64().isEmpty()) {
            Bitmap bmp = ImageHelper.base64ToBitmap(task.getImageBase64());
            if (bmp != null) {
                holder.imgShell.setVisibility(View.VISIBLE);
                holder.imgTask.setImageBitmap(bmp);
            } else holder.imgShell.setVisibility(View.GONE);
        } else holder.imgShell.setVisibility(View.GONE);

        holder.tvStars.setText(ctx.getString(R.string.child_stars_worth, (int)task.getStarsWorth()));
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    private String formatDueText(android.content.Context ctx, String dueAt, long days) {
        if (days == Long.MAX_VALUE) return ctx.getString(R.string.child_due_no_date);
        if (days < 0) return ctx.getString(R.string.child_due_late, (int)Math.abs(days));
        if (days == 0) return ctx.getString(R.string.child_due_today);
        if (days == 1) return ctx.getString(R.string.child_due_tomorrow);
        if (days <= 7) return ctx.getString(R.string.child_due_days_left, (int)days);
        return dueAt;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewStatusDot, imgShell; ImageView imgTask;
        TextView tvTitle, tvDue, tvStars; Button btnDone;
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
