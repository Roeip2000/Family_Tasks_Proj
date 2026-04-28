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

    interface OnChildSelectedListener { void onChildSelected(String childId); }

    private final Context context;
    private final List<ChildSummary> childSummaries;
    private final Map<String, Bitmap> childPhotoCache;
    private final OnChildSelectedListener onChildSelectedListener;
    private String selectedChildId;

    ParentDashboardChildSummaryAdapter(@NonNull Context context, @NonNull List<ChildSummary> summaries, @NonNull Map<String, Bitmap> cache, @NonNull OnChildSelectedListener listener) {
        this.context = context;
        this.childSummaries = summaries;
        this.childPhotoCache = cache;
        this.onChildSelectedListener = listener;
    }

    void setSelectedChildId(String id) { this.selectedChildId = id; }

    @NonNull
    @Override
    public ChildSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChildSummaryViewHolder(LayoutInflater.from(context).inflate(R.layout.item_parent_child_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChildSummaryViewHolder holder, int position) {
        final ChildSummary summary = childSummaries.get(position);
        boolean selected = summary.getChildId().equals(selectedChildId);
        boolean isAll = ALL_CHILDREN_ID.equals(summary.getChildId());

        holder.tvName.setText(summary.getDisplayName());
        bindChip(holder.tvAssigned, "פתוחות: " + summary.getAssignedCount(), "#EAF4FF", "#1F4E79", "#B8DBFF");
        bindChip(holder.tvCompleted, "בוצעו: " + summary.getCompletedCount(), "#ECF8F1", "#1E7A45", "#BDE7C9");
        bindChip(holder.tvUrgent, "דחופות: " + summary.getUrgentCount(), "#FFF4E5", "#9C5A00", "#FFD199");
        
        if (summary.getOverdueCount() > 0) {
            holder.tvOverdue.setVisibility(View.VISIBLE);
            bindChip(holder.tvOverdue, "באיחור: " + summary.getOverdueCount(), "#FFEBEE", "#C62828", "#FFCDD2");
        } else holder.tvOverdue.setVisibility(View.GONE);

        holder.card.setCardBackgroundColor(Color.parseColor(selected ? "#F3F8FF" : "#FFFFFF"));
        holder.card.setStrokeColor(Color.parseColor(selected ? "#2F80ED" : "#D9E2EC"));
        holder.card.setStrokeWidth(selected ? 6 : 3);

        if (isAll) {
            holder.ivPhoto.setImageResource(R.drawable.ic_home_family);
        } else {
            holder.ivPhoto.setImageDrawable(null);
            if (summary.getChildProfileBase64() != null && !summary.getChildProfileBase64().isEmpty()) {
                if (childPhotoCache.containsKey(summary.getChildId())) {
                    holder.ivPhoto.setImageBitmap(childPhotoCache.get(summary.getChildId()));
                } else {
                    Bitmap raw = ImageHelper.base64ToBitmap(summary.getChildProfileBase64());
                    if (raw != null) {
                        Bitmap circle = ImageHelper.getCircularBitmap(raw);
                        childPhotoCache.put(summary.getChildId(), circle);
                        holder.ivPhoto.setImageBitmap(circle);
                    }
                }
            } else holder.ivPhoto.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onChildSelectedListener.onChildSelected(summary.getChildId()); }
        });
    }

    @Override
    public int getItemCount() { return childSummaries.size(); }

    private void bindChip(TextView tv, String text, String bg, String txtColor, String stroke) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(40); gd.setColor(Color.parseColor(bg)); gd.setStroke(2, Color.parseColor(stroke));
        tv.setBackground(gd); tv.setTextColor(Color.parseColor(txtColor)); tv.setText(text);
    }

    static class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card; ImageView ivPhoto;
        TextView tvName, tvAssigned, tvCompleted, tvUrgent, tvOverdue;
        ChildSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardChildSummary);
            ivPhoto = itemView.findViewById(R.id.ivChildSummaryPhoto);
            tvName = itemView.findViewById(R.id.tvChildSummaryName);
            tvAssigned = itemView.findViewById(R.id.tvChildSummaryAssigned);
            tvCompleted = itemView.findViewById(R.id.tvChildSummaryCompleted);
            tvUrgent = itemView.findViewById(R.id.tvChildSummaryUrgent);
            tvOverdue = itemView.findViewById(R.id.tvChildSummaryOverdue);
        }
    }
}
