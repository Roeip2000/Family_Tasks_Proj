package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.auth.ChildQRLoginFragment;
import com.example.family_tasks_proj.auth.ChildSelectionActivity;
import com.example.family_tasks_proj.firebase.FBsingleton;
import com.example.family_tasks_proj.auth.ParentLoginFragment;
import com.example.family_tasks_proj.auth.ParentRegisterFragment;
import com.example.family_tasks_proj.parent.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** מסך הכניסה הראשי של האפליקציה. מנתב בין כניסת הורה לילד ובודק סשנים פעילים. */
public class MainActivity extends AppCompatActivity {

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול Singleton של Firebase
        FBsingleton.getInstance();

        // FirebaseAuth שומר התחברות של הורה גם אחרי סגירת האפליקציה.
        // לכן קודם בודקים אם כבר יש הורה מחובר לפני שמציגים את מסך הבית.
        if (openSavedParentSession()) {
            return;
        }

        // לילד אין FirebaseAuth משלו. את פרטי הילד שומרים מקומית ב-SharedPreferences.
        if (openSavedChildSession()) {
            return;
        }

        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);


        //Fragment ברירית מחדל
        if (savedInstanceState == null) {
            showFragment(new ParentLoginFragment(), false);
        }

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
        btnChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openChildQuickLogin();
            }
        });
    }

    // בודק אם יש הורה מחובר ומדלג לדשבורד
    private boolean openSavedParentSession() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return false;
        }
        startActivity(new Intent(this, ParentDashboardActivity.class));
        finish();
        return true;
    }

    // בודק אם יש ילד מחובר ומדלג לדשבורד
    private boolean openSavedChildSession() {
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        String parentId = sp.getString("parentId", null);
        String childId = sp.getString("childId", null);
        if (parentId == null || childId == null) {
            return false;
        }
        
        // Intent מעביר את מזהי ההורה והילד למסך הבא כדי לדעת מאיזה נתיב ב-Firebase לטעון משימות.
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("childId", childId);
        startActivity(intent);
        finish();
        return true;
    }

    private void openChildQuickLogin() {
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        String parentId = sp.getString("parentId", null);
        String childId = sp.getString("childId", null);

        // אם כבר נשמר ילד במכשיר, נכנסים ישר אליו. אחרת פותחים בחירת משפחה/ילד.
        if (parentId != null && childId != null) {
            Intent intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra("parentId", parentId);
            intent.putExtra("childId", childId);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ChildSelectionActivity.class);
            if (parentId != null) {
                intent.putExtra("parentId", parentId);
            }
            startActivity(intent);
        }
    }

    // --- נושא במחוון 9.5: Fragments ---
    // הפונקציה הזו מחליפה בין המסכים השונים (כניסה, הרשמה, סריקת QR)
    // מבלי להחליף את ה-Activity, מה שמאפשר חוויית משתמש חלקה ומהירה יותר.
    private void showFragment(Fragment fragment, boolean addToBackStack) {
        // מחליף בין הפרגמנטים (מסכי כניסה/הרשמה) בתוך ה-Container שמוגדר ב-XML
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }
}
