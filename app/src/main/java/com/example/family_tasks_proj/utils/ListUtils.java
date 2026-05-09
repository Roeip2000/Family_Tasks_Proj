package com.example.family_tasks_proj.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/** מחלקת עזר ל-ListView. מתאימה את הגובה של הרשימה לפי מספר השורות בה. */
public final class ListUtils {

    private ListUtils() {
    }

    // מחשב את הגובה הכולל של פריטי הרשימה ומעדכן את ה-ListView,
    // כדי שכל הילדים יוצגו בלי גלילה פנימית בתוך מסך עם ScrollView.
    public static void fitListHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int listWidth;
        if (listView.getWidth() > 0) {
            listWidth = listView.getWidth();
        } else {
            listWidth = 500;
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);

        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = adapter.getView(i, null, listView);
            itemView.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += itemView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
