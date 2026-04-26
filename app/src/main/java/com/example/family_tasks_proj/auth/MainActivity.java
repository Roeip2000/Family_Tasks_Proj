package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Child_Login.ChildQRLoginFragment;
import com.example.family_tasks_proj.Child_Login.ChildSelectionActivity;
import com.example.family_tasks_proj.Parents.ParentLoginFragment;
import com.example.family_tasks_proj.Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;

/**
 * מסך כניסה ראשי — Launcher Activity.
 *
 * זהו המסך הראשון שנפתח כשמריצים את האפליקציה.
 * מציג כפתורים לניווט בין:
 * - התחברות הורה (ParentLoginFragment)
 * - הרשמת הורה (ParentRegisterFragment)
 * - סריקת QR לילד (ChildQRLoginFragment)
 * - כניסה ישירה לדשבורד ילד — משתמש בסשן שמור (SharedPreferences)
 *
 * כל Fragment נטען לתוך fragmentContainer שב-layout.
 */

public class MainActivity extends AppCompatActivity
{

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // חיבור כפתורים מה-layout
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);

        // טעינת Fragment ברירת מחדל — מסך לוגין הורה
        if (savedInstanceState == null)
        {
            showFragment(new ParentLoginFragment(), false);
        }

        // ניווט בין מסכים — כל לחיצה מחליפה את ה-Fragment
        btnRegister.setOnClickListener(v -> showFragment(new ParentRegisterFragment(), true));
        btnLogin.setOnClickListener(v -> showFragment(new ParentLoginFragment(), true));
        btnChildQR.setOnClickListener(v -> showFragment(new ChildQRLoginFragment(), true));

        // כניסה מהירה לילד:
        // 1) יש סשן מלא (הורה + ילד) — הולכים ישר לדשבורד הילד.
        // 2) יש רק parentId שמור — ממשיכים למסך בחירת הילד של אותו הורה.
        // 3) אין כלום — מסבירים לילד קצר שצריך QR, ונותנים גם דרך ידנית כגיבוי.
        btnChild.setOnClickListener(v -> openChildQuickLogin());
    }

    private void openChildQuickLogin() {
        // session מקומי מאפשר לילד לחזור מהר בלי לסרוק QR בכל פעם
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        String savedParent = sp.getString("parentId", null);
        String savedChild = sp.getString("childId", null);

        if (savedParent != null && savedChild != null) {
            Intent intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra("parentId", savedParent);
            intent.putExtra("childId", savedChild);
            startActivity(intent);
            return;
        }

        if (savedParent != null) {
            Intent intent = new Intent(this, ChildSelectionActivity.class);
            intent.putExtra("parentId", savedParent);
            startActivity(intent);
            return;
        }

        showFirstTimeChildDialog();
    }

    // כניסה ראשונה של ילד — עדיף QR; בחירה ידנית מוצגת כגיבוי ולא כברירת מחדל
    private void showFirstTimeChildDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.child_first_time_title)
                .setMessage(R.string.child_first_time_message)
                .setPositiveButton(R.string.child_first_time_scan, (d, w) ->
                        showFragment(new ChildQRLoginFragment(), true))
                .setNegativeButton(R.string.child_first_time_manual, (d, w) ->
                        startActivity(new Intent(this, ChildSelectionActivity.class)))
                .setNeutralButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * מחליף את ה-Fragment המוצג ב-fragmentContainer ושומר ב-back stack.
     * ה-addToBackStack מאפשר לחזור ל-Fragment הקודם עם כפתור "חזרה".
     */
    private void showFragment(Fragment fragment, boolean addToBackStack)
    {
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
