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

/** אדפטר לכרטיסי הילדים בדשבורד ההורה ולבחירת ילד. */
class ParentDashboardChildSummaryAdapter
        extends RecyclerView.Adapter<ParentDashboardChildSummaryAdapter.ChildSummaryViewHolder> {

    /** מזהה מיוחד לצ'יפ "כל הילדים" שאינו ילד אמיתי ב-Firebase. */
    static final String ALL_CHILDREN_ID = "__ALL__";

    interface OnChildSelectedListener {
        void onChildSelected(String childId);
    }

    private static final String METRIC_BLUE_BG = "#EAF4FF";
    private static final String METRIC_BLUE_TEXT = "#1F4E79";
    private static final String METRIC_BLUE_STROKE = "#B8DBFF";
    private static final String METRIC_GREEN_BG = "#ECF8F1";
    private static final String METRIC_GREEN_TEXT = "#1E7A45";
    private static final String METRIC_GREEN_STROKE = "#BDE7C9";
    private static final String METRIC_ORANGE_BG = "#FFF4E5";
    private static final String METRIC_ORANGE_TEXT = "#9C5A00";
    private static final String METRIC_ORANGE_STROKE = "#FFD199";
    private static final String METRIC_RED_BG = "#FFEBEE";
    private static final String METRIC_RED_TEXT = "#C62828";
    private static final String METRIC_RED_STROKE = "#FFCDD2";
    private static final String CHILD_CARD_DEFAULT_BG = "#FFFFFF";
    private static final String CHILD_CARD_SELECTED_BG = "#F3F8FF";
    private static final String CHILD_CARD_DEFAULT_STROKE = "#D9E2EC";
    private static final String CHILD_CARD_SELECTED_STROKE = "#2F80ED";

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
        boolean isAllChildren = ALL_CHILDREN_ID.equals(childSummary.childId);

        holder.tvChildSummaryName.setText(childSummary.displayName);
        bindMetricChip(holder.tvChildSummaryAssigned, R.string.parent_dashboard_summary_assigned,
                childSummary.assignedCount, METRIC_BLUE_BG, METRIC_BLUE_TEXT, METRIC_BLUE_STROKE);
        bindMetricChip(holder.tvChildSummaryCompleted, R.string.parent_dashboard_summary_completed,
                childSummary.completedCount, METRIC_GREEN_BG, METRIC_GREEN_TEXT, METRIC_GREEN_STROKE);
        bindMetricChip(holder.tvChildSummaryUrgent, R.string.parent_dashboard_summary_urgent,
                childSummary.urgentCount, METRIC_ORANGE_BG, METRIC_ORANGE_TEXT, METRIC_ORANGE_STROKE);
        if (childSummary.overdueCount > 0) {
            holder.tvChildSummaryOverdue.setVisibility(View.VISIBLE);
            bindMetricChip(holder.tvChildSummaryOverdue, R.string.parent_dashboard_group_overdue,
                    childSummary.overdueCount, METRIC_RED_BG, METRIC_RED_TEXT, METRIC_RED_STROKE);
        } else {
            holder.tvChildSummaryOverdue.setVisibility(View.GONE);
        }

        bindCardSelection(holder, isSelected);
        holder.itemView.setContentDescription(
                context.getString(R.string.parent_dashboard_child_content_description, childSummary.displayName));

        if (isAllChildren) {
            holder.ivChildSummaryPhoto.setImageResource(R.drawable.ic_family_cluster);
        } else {
            bindChildPhoto(holder.ivChildSummaryPhoto,
                    childSummary.childId,
                    childSummary.childProfileBase64);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onChildSelectedListener.onChildSelected(childSummary.childId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return childSummaries.size();
    }

    private void bindMetricChip(TextView textView, int labelResId, int count,
                                String backgroundColor, String textColor, String strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(18));
        background.setColor(Color.parseColor(backgroundColor));
        background.setStroke(dpToPx(1), Color.parseColor(strokeColor));

        textView.setBackground(background);
        textView.setTextColor(Color.parseColor(textColor));
        textView.setText(context.getString(R.string.parent_dashboard_metric_with_count,
                context.getString(labelResId), count));
    }

    // צובע כרטיס ילד לפי מצב הבחירה שלו ברשימה
    private void bindCardSelection(ChildSummaryViewHolder holder, boolean isSelected) {
        String backgroundColor;
        String strokeColor;
        int strokeWidth;

        if (isSelected) {
            backgroundColor = CHILD_CARD_SELECTED_BG;
            strokeColor = CHILD_CARD_SELECTED_STROKE;
            strokeWidth = 2;
        } else {
            backgroundColor = CHILD_CARD_DEFAULT_BG;
            strokeColor = CHILD_CARD_DEFAULT_STROKE;
            strokeWidth = 1;
        }

        holder.cardChildSummary.setCardBackgroundColor(Color.parseColor(backgroundColor));
        holder.cardChildSummary.setStrokeColor(Color.parseColor(strokeColor));
        holder.cardChildSummary.setStrokeWidth(dpToPx(strokeWidth));
    }

    // טוען תמונת ילד מ-Base64, עם זיכרון זמני כדי לא לפענח שוב ושוב
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

    static class ChildSummaryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardChildSummary;
        private final ImageView ivChildSummaryPhoto;
        private final TextView tvChildSummaryName;
        private final TextView tvChildSummaryAssigned;
        private final TextView tvChildSummaryCompleted;
        private final TextView tvChildSummaryUrgent;
        private final TextView tvChildSummaryOverdue;

        ChildSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardChildSummary = itemView.findViewById(R.id.cardChildSummary);
            ivChildSummaryPhoto = itemView.findViewById(R.id.ivChildSummaryPhoto);
            tvChildSummaryName = itemView.findViewById(R.id.tvChildSummaryName);
            tvChildSummaryAssigned = itemView.findViewById(R.id.tvChildSummaryAssigned);
            tvChildSummaryCompleted = itemView.findViewById(R.id.tvChildSummaryCompleted);
            tvChildSummaryUrgent = itemView.findViewById(R.id.tvChildSummaryUrgent);
            tvChildSummaryOverdue = itemView.findViewById(R.id.tvChildSummaryOverdue);
        }
    }
}
