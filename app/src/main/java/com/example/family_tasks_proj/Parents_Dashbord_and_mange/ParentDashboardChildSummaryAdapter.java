package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.ChildSummary;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.util.ImageHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

/** מתאם עבור רשימת כרטיסי הילדים בדשבורד. מאפשר להורה לבחור ילד ולראות סיכום מהיר עליו. */
class ParentDashboardChildSummaryAdapter extends RecyclerView.Adapter<ParentDashboardChildSummaryAdapter.ChildSummaryViewHolder> {

    static final String ALL_CHILDREN_ID = "__ALL__";

    interface OnChildSelectedListener {
        void onChildSelected(String childId);
    }

    private final Context context;
    private final List<ChildSummary> childSummaries;
    private final OnChildSelectedListener onChildSelectedListener;
    private String selectedChildId;

    ParentDashboardChildSummaryAdapter(@NonNull Context context, @NonNull List<ChildSummary> summaries, @NonNull OnChildSelectedListener listener) {
        this.context = context;
        this.childSummaries = summaries;
        this.onChildSelectedListener = listener;
    }

    void setSelectedChildId(String id) {
        this.selectedChildId = id;
    }

    @NonNull
    @Override
    public ChildSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChildSummaryViewHolder(LayoutInflater.from(context).inflate(R.layout.item_parent_child_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChildSummaryViewHolder holder, int position) {
        final ChildSummary summary = childSummaries.get(position);
        boolean selected = summary.getChildId().equals(selectedChildId);

        holder.tvName.setText(summary.getDisplayName());
        bindChip(holder.tvAssigned, context.getString(R.string.child_summary_open_tasks, summary.getAssignedCount()), "#EAF4FF", "#1F4E79", "#B8DBFF");
        bindChip(holder.tvCompleted, context.getString(R.string.child_summary_completed_tasks, summary.getCompletedCount()), "#ECF8F1", "#1E7A45", "#BDE7C9");
        bindChip(holder.tvUrgent, context.getString(R.string.child_summary_urgent_tasks, summary.getUrgentCount()), "#FFF4E5", "#9C5A00", "#FFD199");
        
        if (summary.getOverdueCount() > 0) {
            holder.tvOverdue.setVisibility(View.VISIBLE);
            bindChip(holder.tvOverdue, context.getString(R.string.child_summary_overdue_tasks, summary.getOverdueCount()), "#FFEBEE", "#C62828", "#FFCDD2");
        } else {
            holder.tvOverdue.setVisibility(View.GONE);
        }

        String cardBackgroundColor;
        String cardStrokeColor;
        int cardStrokeWidth;
        if (selected) {
            cardBackgroundColor = "#F3F8FF";
            cardStrokeColor = "#2F80ED";
            cardStrokeWidth = 6;
        } else {
            cardBackgroundColor = "#FFFFFF";
            cardStrokeColor = "#D9E2EC";
            cardStrokeWidth = 3;
        }
        holder.card.setCardBackgroundColor(Color.parseColor(cardBackgroundColor));
        holder.card.setStrokeColor(Color.parseColor(cardStrokeColor));
        holder.card.setStrokeWidth(cardStrokeWidth);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onChildSelectedListener.onChildSelected(summary.getChildId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return childSummaries.size();
    }

    private void bindChip(TextView textView, String text, String backgroundColor, String textColor, String strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(40);
        background.setColor(Color.parseColor(backgroundColor));
        background.setStroke(2, Color.parseColor(strokeColor));
        textView.setBackground(background);
        textView.setTextColor(Color.parseColor(textColor));
        textView.setText(text);
    }

    static class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName;
        TextView tvAssigned;
        TextView tvCompleted;
        TextView tvUrgent;
        TextView tvOverdue;

        ChildSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardChildSummary);
            tvName = itemView.findViewById(R.id.tvChildSummaryName);
            tvAssigned = itemView.findViewById(R.id.tvChildSummaryAssigned);
            tvCompleted = itemView.findViewById(R.id.tvChildSummaryCompleted);
            tvUrgent = itemView.findViewById(R.id.tvChildSummaryUrgent);
            tvOverdue = itemView.findViewById(R.id.tvChildSummaryOverdue);
        }
    }
}
