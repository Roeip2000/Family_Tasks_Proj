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
import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.example.family_tasks_proj.Parents.ParentLoginFragment;
import com.example.family_tasks_proj.Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
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

        if (openSavedParentSession()) return;
        if (openSavedChildSession()) return;

        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);

        if (savedInstanceState == null) showFragment(new ParentLoginFragment(), false);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFragment(new ParentRegisterFragment(), true); }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFragment(new ParentLoginFragment(), true); }
        });
        btnChildQR.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFragment(new ChildQRLoginFragment(), true); }
        });
        btnChild.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openChildQuickLogin(); }
        });
    }

    // בודק אם יש הורה מחובר ומדלג לדשבורד
    private boolean openSavedParentSession() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;
        startActivity(new Intent(this, ParentDashboardActivity.class));
        finish();
        return true;
    }

    // בודק אם יש ילד מחובר ומדלג לדשבורד
    private boolean openSavedChildSession() {
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        String pId = sp.getString("parentId", null);
        String cId = sp.getString("childId", null);
        if (pId == null || cId == null) return false;
        
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("parentId", pId);
        intent.putExtra("childId", cId);
        startActivity(intent);
        finish();
        return true;
    }

    private void openChildQuickLogin() {
        SharedPreferences sp = getSharedPreferences("child_session", MODE_PRIVATE);
        String pId = sp.getString("parentId", null);
        String cId = sp.getString("childId", null);

        if (pId != null && cId != null) {
            Intent intent = new Intent(this, ChildDashboardActivity.class);
            intent.putExtra("parentId", pId); intent.putExtra("childId", cId);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ChildSelectionActivity.class);
            if (pId != null) intent.putExtra("parentId", pId);
            startActivity(intent);
        }
    }

    private void showFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) transaction.addToBackStack(fragment.getClass().getSimpleName());
        transaction.commit();
    }
}
