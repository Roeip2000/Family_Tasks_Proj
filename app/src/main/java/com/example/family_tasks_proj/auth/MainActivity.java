package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Child_Login.ChildQRLoginFragment;
import com.example.family_tasks_proj.Child_Login.ChildSelectionActivity;
import com.example.family_tasks_proj.Parents.ParentLoginFragment;
import com.example.family_tasks_proj.Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// מסך הכניסה הראשי המורכב מ-3 פרגמנטים בו זמנית
public class MainActivity extends AppCompatActivity implements MainSelectionFragment.OnRoleSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // בדיקה אם המשתמש כבר מחובר
        if (checkExistingSessions()) return;

        // טעינת 3 הפרגמנטים למסך בבת אחת
        loadStaticFragments();
    }

    private void loadStaticFragments() {
        // 1. פרגמנט כותרת (Header)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.headerContainer, new MainHeaderFragment())
                .commit();

        // 2. פרגמנט בחירה (Selection) - יושב במרכז
        MainSelectionFragment selectionFragment = new MainSelectionFragment();
        selectionFragment.setOnRoleSelectedListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, selectionFragment)
                .commit();

        // 3. פרגמנט תחתית (Footer)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.footerContainer, new MainFooterFragment())
                .commit();
    }

    @Override
    public void onRoleSelected(int actionId) {
        // החלפת הפרגמנט המרכזי בלבד לפי הלחיצה
        Fragment targetForm = null;
        switch (actionId) {
            case 1: targetForm = new ParentLoginFragment(); break;
            case 2: targetForm = new ParentRegisterFragment(); break;
            case 3: targetForm = new ChildQRLoginFragment(); break;
            case 4: 
                startActivity(new Intent(this, ChildSelectionActivity.class));
                return;
        }

        if (targetForm != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.contentContainer, targetForm)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private boolean checkExistingSessions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            startActivity(new Intent(this, ParentDashboardActivity.class));
            finish();
            return true;
        }
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        if (sp.contains("parentId") && sp.contains("childId")) {
            startActivity(new Intent(this, ChildDashboardActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
