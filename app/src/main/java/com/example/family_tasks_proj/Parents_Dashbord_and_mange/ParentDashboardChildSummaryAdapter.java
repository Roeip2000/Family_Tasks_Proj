package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Context;
import android.graphics.Bitmap;
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

/**
 * אדפטר לרשימת הילדים בדשבורד ההורה.
 * כל כרטיס מציג שם ילד, תמונה, וספירות משימות (נשלחו/בוצעו/דחופות).
 * לחיצה על כרטיס בוחרת את הילד ומציגה רק את המשימות שלו.
 */
class ParentDashboardChildSummaryAdapter
        extends RecyclerView.Adapter<ParentDashboardChildSummaryAdapter.ChildSummaryViewHolder> {

    interface OnChildSelectedListener {
        void onChildSelected(String childId);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final List<ChildSummary> childSummaries;
    private final Map<String, Bitmap> childPhotoCache;
    private final OnChildSelectedListener onChildSelectedListener;
    private String selectedChildId;

    ParentDashboardChildSummaryAdapter(@NonNull Context context,
                                       @NonNull List<ChildSummary> childSummaries,
                                       @NonNull Map<String, Bitmap> childPhotoCache,
                                       @NonNull OnChildSelectedListener onChildSelectedListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.childSummaries = childSummaries;
        this.childPhotoCache = childPhotoCache;
        this.onChildSelectedListener = onChildSelectedListener;
    }

    void setSelectedChildId(String selectedChildId) {
        this.selectedChildId = selectedChildId;
    }

    @NonNull
    @Override
    public ChildSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_parent_child_summary, parent, false);
        return new ChildSummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildSummaryViewHolder holder, int position) {
        ChildSummary childSummary = childSummaries.get(position);
        boolean isSelected = childSummary.childId.equals(selectedChildId);

        holder.tvChildSummaryName.setText(childSummary.displayName);
        holder.tvChildSummaryAssigned.setText(getCompactStats(childSummary));
        holder.cardChildSummary.setCardBackgroundColor(context.getColor(
                isSelected ? R.color.primary_light : R.color.bg_card));
        holder.cardChildSummary.setStrokeColor(context.getColor(
                isSelected ? R.color.primary : R.color.border_light));
        holder.cardChildSummary.setStrokeWidth(dpToPx(isSelected ? 2 : 1));
        holder.itemView.setContentDescription(
                context.getString(R.string.parent_dashboard_child_content_description, childSummary.displayName));

        bindChildPhoto(holder.ivChildSummaryPhoto,
                childSummary.childId,
                childSummary.childProfileBase64);

        holder.itemView.setOnClickListener(v -> onChildSelectedListener.onChildSelected(childSummary.childId));
    }

    @Override
    public int getItemCount() {
        return childSummaries.size();
    }

    // טוען תמונת ילד מ-Base64, עם cache כדי לא לפענח שוב ושוב
    private void bindChildPhoto(ImageView imageView, String childId, String base64) {
        imageView.setImageDrawable(null);

        if (base64 == null || base64.trim().isEmpty()) {
            return;
        }

        if (childPhotoCache.containsKey(childId)) {
            imageView.setImageBitmap(childPhotoCache.get(childId));
            return;
        }

        Bitmap raw = ImageHelper.base64ToBitmap(base64);
        if (raw == null) {
            return;
        }

        Bitmap circular = ImageHelper.getCircularBitmap(raw);
        childPhotoCache.put(childId, circular);
        imageView.setImageBitmap(circular);
    }

    private int dpToPx(int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    private String getCompactStats(ChildSummary childSummary) {
        if (childSummary.assignedCount <= 0) {
            return context.getString(R.string.parent_dashboard_child_compact_none);
        }

        if (childSummary.urgentCount > 0) {
            return context.getString(
                    R.string.parent_dashboard_child_compact_urgent,
                    childSummary.assignedCount,
                    childSummary.urgentCount
            );
        }

        return context.getString(
                R.string.parent_dashboard_child_compact_open,
                childSummary.assignedCount
        );
    }

    static class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardChildSummary;
        private final ImageView ivChildSummaryPhoto;
        private final TextView tvChildSummaryName;
        private final TextView tvChildSummaryAssigned;

        ChildSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardChildSummary = itemView.findViewById(R.id.cardChildSummary);
            ivChildSummaryPhoto = itemView.findViewById(R.id.ivChildSummaryPhoto);
            tvChildSummaryName = itemView.findViewById(R.id.tvChildSummaryName);
            tvChildSummaryAssigned = itemView.findViewById(R.id.tvChildSummaryAssigned);
        }
    }
}
