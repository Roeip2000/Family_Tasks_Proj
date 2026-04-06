package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Child_Login.ChildQRLoginFragment;
import com.example.family_tasks_proj.Child_Login.ChildSelectionActivity;
import com.example.family_tasks_proj.Parents.ParentLoginFragment;
import com.example.family_tasks_proj.Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.R;

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
public class MainActivity extends AppCompatActivity {

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

        // כניסה ישירה ללא QR — פותח מסך בחירת הורה+ילד.
        // אם יש סשן שמור (parentId) — ChildSelectionActivity ידלג ישר ל-Spinner ילדים.
        // אם אין סשן — ChildSelectionActivity יציג Spinner הורים קודם ואז ילדים.
        btnChild.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, ChildSelectionActivity.class);
            // אם יש סשן שמור — מעביר כ-extra כדי לדלג על בחירת הורה
            SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
            String savedParent = sp.getString("parentId", null);
            if (savedParent != null) {
                intent.putExtra("parentId", savedParent);
            }
            startActivity(intent);
        });
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
