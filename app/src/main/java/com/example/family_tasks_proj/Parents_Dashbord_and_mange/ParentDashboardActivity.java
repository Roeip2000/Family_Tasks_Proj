package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;

/**
 * דשבורד ראשי של ההורה — מסך Hub.
 *
 * זהו המסך המרכזי אחרי התחברות ההורה.
 * מציג ארבעה כפתורי ניווט:
 * 1. ניהול ילדים — הוספת ילדים חדשים (ManageChildrenActivity)
 * 2. מאגר תבניות — יצירת תבניות משימה עם תמונה (ParentTaskTemplateActivity)
 * 3. הקצאת משימה — שליחת משימה מהמאגר לילד ספציפי (AssignTaskToChildActivity)
 * 4. QR לילדים — הצגת קוד QR לכל ילד, ה-ילד סורק כדי להתחבר (GenerateQRActivity)
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף סיכום כמות ילדים/משימות מ-Firebase (הנתונים ב-layout קיימים אבל לא מאוכלסים).
 * TODO: להוסיף כפתור logout שמנתק מ-FirebaseAuth ומחזיר ל-MainActivity.
 * TODO: להציג שם ההורה המחובר בכותרת המסך.
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private Button btnManageChildren, btnManageTemplates, btnAssignTaskToChild, btnShowQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // חיבור כפתורים — כל כפתור פותח Activity ייעודי
        btnManageChildren = findViewById(R.id.btnManageChildren);
        btnManageChildren.setOnClickListener(v ->
        {
            // ניהול ילדים — הוספת ילד חדש
            startActivity(new Intent(ParentDashboardActivity.this, ManageChildrenActivity.class));
        });

        btnManageTemplates = findViewById(R.id.btnManageTemplates);
        btnManageTemplates.setOnClickListener(v ->
        {
            // מאגר תבניות — יצירת תבנית משימה חדשה עם תמונה
            startActivity(new Intent(ParentDashboardActivity.this, ParentTaskTemplateActivity.class));
        });

        btnAssignTaskToChild = findViewById(R.id.btnAssignTaskToChild);
        btnAssignTaskToChild.setOnClickListener(v ->
        {
            // הקצאת משימה — בחירת תבנית + ילד + תאריך
            startActivity(new Intent(ParentDashboardActivity.this, AssignTaskToChildActivity.class));
        });

        btnShowQR = findViewById(R.id.btnShowQR);
        btnShowQR.setOnClickListener(v ->
        {
            // QR לילדים — Spinner לבחירת ילד + הצגת QR
            startActivity(new Intent(ParentDashboardActivity.this, GenerateQRActivity.class));
        });
    }
}
