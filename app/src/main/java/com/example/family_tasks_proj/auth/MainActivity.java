package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.family_tasks_proj.firebase.FBsingleton;
import com.example.family_tasks_proj.parent.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.example.family_tasks_proj.utils.ChildSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_SKIP_AUTO_LOGIN = "skipAutoLogin";

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול Singleton של Firebase
        FBsingleton.getInstance();

        boolean skipAutoLogin = getIntent().getBooleanExtra(EXTRA_SKIP_AUTO_LOGIN, false);

        // FirebaseAuth שומר התחברות של הורה גם אחרי סגירת האפליקציה.
        // לכן קודם בודקים אם כבר יש הורה מחובר לפני שמציגים את מסך הבית.
        if (!skipAutoLogin && openSavedParentSession()) {
            return;
        }

        // לילד אין FirebaseAuth משלו. את פרטי הילד שומרים מקומית ב-SharedPreferences.
        if (!skipAutoLogin && openChildSession(true)) {
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

        // הגדרת המחלקה עצמה כמאזינה ללחיצות
        btnRegister.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnChildQR.setOnClickListener(this);
        btnChild.setOnClickListener(this);
    }

    // פונקציה אחת מרוכזת המטפלת בכל הלחיצות במסך
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnRegister) {
            showFragment(new ParentRegisterFragment(), true);
        } else if (id == R.id.btnLogin) {
            showFragment(new ParentLoginFragment(), true);
        } else if (id == R.id.btnChildQR) {
            showFragment(new ChildQRLoginFragment(), true);
        } else if (id == R.id.btnChild) {
            openChildSession(false);
        }
    }

    // בודק אם יש הורה מחובר ומדלג לדשבורד
    private boolean openSavedParentSession()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
        {
            return false;
        }
        startActivity(new Intent(this, ParentDashboardActivity.class));
        finish();
        return true;
    }

    // בודק או מבצע כניסת ילד
    // autoLogin = true: מופעל אוטומטית, מחזיר false אם אין חיבור מלא כדי לא להפריע ל-MainActivity.
    // autoLogin = false: מופעל בלחיצת כפתור, פותח מסך בחירת ילד (ChildSelectionActivity) אם חסר חיבור.
    private boolean openChildSession(boolean autoLogin)
    {
        // קוראים נתוני session של ילד מהמחלקה המרוכזת
        String parentId = ChildSession.getParentId(this);
        String childId = ChildSession.getChildId(this);

        // אם יש parentId וגם childId ב-SharedPreferences:
        if (parentId != null && childId != null) {
            // מעבירים Intent extras כדי לדעת מאיזה נתיב ב-Firebase לטעון משימות
            Intent intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra(ChildSession.KEY_PARENT, parentId);
            intent.putExtra(ChildSession.KEY_CHILD, childId);
            startActivity(intent);
            if (autoLogin) {
                finish();
            }
            return true;
        }

        // אם זו כניסה אוטומטית ואין session מלא - לא עושים כלום
        if (autoLogin) {
            return false;
        }

        // navigation: מעבר למסך בחירת ילד (ChildSelectionActivity)
        Intent intent = new Intent(this, ChildSelectionActivity.class);

        // אם אין parentId שמור, נבדוק אם יש הורה מחובר כרגע ב-FirebaseAuth
        if (parentId == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                parentId = user.getUid();
            }
        }

        // מעבירים את parentId כדי שהמשתמש יבחר רק ילד, ולא יבחר שוב הורה
        if (parentId != null) {
            intent.putExtra(ChildSession.KEY_PARENT, parentId);
        }

        startActivity(intent);
        return false;
    }

    // הפונקציה הזו מחליפה בין המסכים השונים (כניסה, הרשמה, סריקת QR)
    private void showFragment(Fragment fragment, boolean addToBackStack)
    {
        // מחליף בין הפרגמנטים (מסכי כניסה/הרשמה) בתוך ה-Container שמוגדר ב-XML
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }
}
