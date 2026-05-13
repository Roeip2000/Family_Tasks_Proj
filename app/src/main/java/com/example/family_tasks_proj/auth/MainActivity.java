package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.firebase.FBsingleton;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול Singleton של Firebase
        FBsingleton.getInstance();

        // אין כניסה אוטומטית אחרי סגירת האפליקציה.
        FirebaseAuth.getInstance().signOut();

        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);

        // Fragment ברירת מחדל
        if (savedInstanceState == null) {
            showFragment(new ParentLoginFragment(), false);
        }

        // הגדרת המחלקה עצמה כמאזינה ללחיצות
        btnRegister.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnChildQR.setOnClickListener(this);
        btnChild.setOnClickListener(this);
    }

    // פונקציה אחת מרכזת שמטפלת בכל הלחיצות במסך
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
            startActivity(new Intent(this, ChildSelectionActivity.class));
        }
    }

    // הפונקציה הזו מחליפה בין המסכים השונים (כניסה, הרשמה, סריקת QR)
    private void showFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }
}
