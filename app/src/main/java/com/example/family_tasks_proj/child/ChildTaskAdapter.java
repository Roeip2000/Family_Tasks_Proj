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

public class ChildTaskAdapter extends RecyclerView.Adapter<ChildTaskAdapter.TaskViewHolder> {

    private final List<ChildTask> tasks;
    private final OnTaskDoneListener doneListener;

    public interface OnTaskDoneListener {
        void onTaskDone(ChildTask task, int position);
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

        // RecyclerView rows are reused, so reset transient animation state first.
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

        if (holder.itemView instanceof CardView) {
            int bgColor = task.isDone
                    ? Color.parseColor("#E8F5E9")
                    : Color.WHITE;
            ((CardView) holder.itemView).setCardBackgroundColor(bgColor);
        }

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

        long daysLeft = DateUtils.daysLeft(task.dueAt);

        String dueText;
        if (task.isDone) {
            dueText = "בוצע";
            holder.tvDueDate.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            dueText = formatDueText(task.dueAt, daysLeft);
            if (daysLeft < 0) {
                holder.tvDueDate.setTextColor(Color.parseColor("#E53935"));
            } else if (daysLeft <= 2) {
                dueText = "דחוף! " + dueText;
                holder.tvDueDate.setTextColor(Color.parseColor("#FF5722"));
            } else {
                holder.tvDueDate.setTextColor(Color.parseColor("#888888"));
            }
        }
        holder.tvDueDate.setText(dueText);

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

        if (task.imageBase64 != null && !task.imageBase64.isEmpty()) {
            Bitmap bmp = ImageHelper.base64ToBitmap(task.imageBase64);
            if (bmp != null) {
                holder.imgTaskImage.setImageBitmap(bmp);
                holder.imgTaskImage.setVisibility(View.VISIBLE);
            } else {
                holder.imgTaskImage.setImageDrawable(null);
                holder.imgTaskImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgTaskImage.setImageDrawable(null);
            holder.imgTaskImage.setVisibility(View.GONE);
        }

        holder.tvTaskStars.setText(task.starsWorth > 0 ? task.starsWorth + " כוכבים" : "");

        if (task.isDone) {
            holder.btnDone.setVisibility(View.GONE);
            holder.btnDone.setOnClickListener(null);
        } else {
            holder.btnDone.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(v -> {
                if (doneListener == null) return;

                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                holder.btnDone.animate()
                        .scaleX(0f).scaleY(0f)
                        .setDuration(200)
                        .start();
                holder.itemView.animate()
                        .alpha(0.6f)
                        .setDuration(300)
                        .withEndAction(() -> doneListener.onTaskDone(task, adapterPosition))
                        .start();
            });
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

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
