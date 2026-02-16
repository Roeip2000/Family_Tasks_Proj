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
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף בדיקת auto-login — אם ההורה כבר מחובר (FirebaseAuth.currentUser != null),
 *       לדלג ישירות ל-ParentDashboardActivity בלי לדרוש login מחדש.
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
            openFragment(new ParentLoginFragment());
        }

        // ניווט בין מסכים — כל לחיצה מחליפה את ה-Fragment
        btnRegister.setOnClickListener(v -> openFragment(new ParentRegisterFragment()));
        btnLogin.setOnClickListener(v -> openFragment(new ParentLoginFragment()));
        btnChildQR.setOnClickListener(v -> openFragment(new ChildQRLoginFragment()));

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
    private void openFragment(Fragment fragment)
    {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
