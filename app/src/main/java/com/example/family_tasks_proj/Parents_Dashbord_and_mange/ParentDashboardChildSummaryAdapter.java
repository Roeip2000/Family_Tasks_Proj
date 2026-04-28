package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.model.ChildSummary;
import com.example.family_tasks_proj.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ParentDashboardChildSummaryAdapter extends RecyclerView.Adapter<ParentDashboardChildSummaryAdapter.ChildSummaryViewHolder> {

    public interface OnChildSelectedListener {
        void onChildSelected(String childId);
    }

    private final Context context;
    private final List<ChildSummary> childSummaries;
    private final OnChildSelectedListener listener;
    private String selectedChildId = "ALL";

    public ParentDashboardChildSummaryAdapter(Context context, List<ChildSummary> summaries, OnChildSelectedListener listener) {
        this.context = context;
        this.childSummaries = summaries;
        this.listener = listener;
    }

    public void setSelectedChildId(String id) {
        this.selectedChildId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChildSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChildSummaryViewHolder(LayoutInflater.from(context).inflate(R.layout.item_parent_child_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChildSummaryViewHolder holder, int position) {
        ChildSummary summary = childSummaries.get(position);
        holder.tvName.setText(summary.getDisplayName());
        holder.tvAssigned.setText(summary.getAssignedCount() + " משימות");
        
        if (summary.getOverdueCount() > 0) {
            holder.tvOverdue.setVisibility(View.VISIBLE);
            holder.tvOverdue.setText(summary.getOverdueCount() + " באיחור");
        } else {
            holder.tvOverdue.setVisibility(View.GONE);
        }

        boolean isSelected = summary.getChildId().equals(selectedChildId);
        holder.card.setStrokeColor(isSelected ? Color.parseColor("#2D3436") : Color.parseColor("#F1F2F6"));
        holder.card.setStrokeWidth(isSelected ? 4 : 2);

        holder.itemView.setOnClickListener(v -> listener.onChildSelected(summary.getChildId()));
    }

    @Override
    public int getItemCount() {
        return childSummaries.size();
    }

    static class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvAssigned, tvOverdue;

        ChildSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardChildSummary);
            tvName = itemView.findViewById(R.id.tvChildSummaryName);
            tvAssigned = itemView.findViewById(R.id.tvChildSummaryAssigned);
            tvOverdue = itemView.findViewById(R.id.tvChildSummaryOverdue);
        }
    }
}
