package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

/** מסך הכניסה הראשי להורה ולילד. */
public class MainActivity extends AppCompatActivity {

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (openSavedParentSession()) {
            return;
        }

        if (openSavedChildSession()) {
            return;
        }

        setContentView(R.layout.activity_main);

        // חיבור כפתורים מהמסך
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);

        // טעינת מסך ברירת מחדל — התחברות הורה
        if (savedInstanceState == null) {
            showFragment(new ParentLoginFragment(), false);
        }

        // ניווט בין מסכים — כל לחיצה מחליפה את המסך הפנימי
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFragment(new ParentRegisterFragment(), true);
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFragment(new ParentLoginFragment(), true);
            }
        });
        btnChildQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFragment(new ChildQRLoginFragment(), true);
            }
        });

        // כניסה מהירה לילד:
        // 1) יש סשן מלא (הורה + ילד) — הולכים ישר לדשבורד הילד.
        // 2) יש רק parentId שמור — ממשיכים למסך בחירת הילד של אותו הורה.
        // 3) אין כלום — פותחים בחירת ילד ידנית.
        btnChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openChildQuickLogin();
            }
        });
    }

    // בודק אם הורה עדיין מחובר ב-FirebaseAuth ופותח ישר את דשבורד ההורה
    private boolean openSavedParentSession() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        Intent intent = new Intent(this, ParentDashboardActivity.class);
        startActivity(intent);
        finish();
        return true;
    }

    // בודק אם יש סשן ילד מלא ב-SharedPreferences ופותח ישר את דשבורד הילד
    private boolean openSavedChildSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("child_session", MODE_PRIVATE);
        String savedParent = sharedPreferences.getString("parentId", null);
        String savedChild = sharedPreferences.getString("childId", null);

        if (savedParent == null || savedChild == null) {
            return false;
        }

        Intent intent = new Intent(this, ChildDashboardActivity.class);
        // parentId אומר לדשבורד מאיזה ענף /parents לקרוא את הילד
        intent.putExtra("parentId", savedParent);
        // childId אומר לדשבורד איזה ילד לפתוח תחת אותו הורה
        intent.putExtra("childId", savedChild);
        startActivity(intent);
        finish();
        return true;
    }

    // כניסה מהירה לילד מתוך מסך הבית לפי הסשן המקומי האחרון
    private void openChildQuickLogin() {
        // סשן מקומי מאפשר לילד לחזור מהר בלי לסרוק QR בכל פעם
        SharedPreferences childSessionPrefs = getSharedPreferences("child_session", MODE_PRIVATE);
        String savedParent = childSessionPrefs.getString("parentId", null);
        String savedChild = childSessionPrefs.getString("childId", null);

        // מקרה 1: יש סשן מלא — הולכים ישר לדשבורד
        if (savedParent != null && savedChild != null) {
            Intent intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra("parentId", savedParent);
            intent.putExtra("childId", savedChild);
            startActivity(intent);
            return;
        }

        // מקרה 2+3: אין סשן מלא — הולכים למסך בחירת ילד
        Intent intent = new Intent(this, ChildSelectionActivity.class);
        if (savedParent != null) {
            intent.putExtra("parentId", savedParent);
        }
        startActivity(intent);
    }

    // מחליף את ה-Fragment שמוצג ושומר אפשרות לחזור אחורה
    private void showFragment(Fragment fragment, boolean addToBackStack) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null
                && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }
}
