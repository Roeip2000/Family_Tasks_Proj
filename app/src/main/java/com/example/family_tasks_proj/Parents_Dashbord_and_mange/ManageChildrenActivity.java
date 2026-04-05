package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.Class_child.Child;
import com.example.family_tasks_proj.util.ImageHelper;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך ניהול ילדים — הוספה, צפייה, עריכה ומחיקה.
 *
 * המסך מחולק לשני חלקים:
 * - למעלה: טופס הוספת/עריכת ילד (תמונה + שם פרטי + משפחה + כפתורי שמור/ביטול).
 * - למטה: רשימת כל הילדים הרשומים של ההורה עם תמונות.
 *
 * לחיצה על ילד ברשימה פותחת תפריט:
 * 1. ערוך ילד — ממלא את הטופס בנתונים הנוכחיים (מצב עריכה).
 * 2. מחק ילד — מבקש אישור ומוחק מ-Firebase (כולל כל המשימות שלו).
 *
 * מצב עריכה מופעל ע"י enterEditMode() ומתאפס ע"י resetForm().
 * כשבמצב עריכה, כפתור "הוסף ילד" הופך ל"שמור שינויים" ומופיע כפתור ביטול.
 *
 * Layout: activity_manage_children.xml
 * שורת ילד ברשימה: item_manage_child.xml
 * נתיב Firebase: /parents/{uid}/children/
 */
public class ManageChildrenActivity extends AppCompatActivity {

    // --- Views ---
    private EditText etFirstName, etLastName;
    private Button btnAddChild, btnCancelEdit;
    private ListView lvChildren;
    private TextView tvNoChildren;
    /** ImageView לתצוגה מקדימה של תמונת הילד שנבחרה */
    private ImageView imgChildPhoto;

    // --- Firebase ---
    private DatabaseReference db;
    private String parentUID;

    /**
     * ה-Bitmap של תמונת הילד אחרי תיקון EXIF + הקטנה.
     * null = לא נבחרה תמונה (ילד יישמר בלי תמונה או תישמר התמונה הישנה).
     */
    private Bitmap childPhotosBitmap;

    /**
     * כשעורכים ילד קיים — מחזיק את ה-id שלו.
     * null = מצב הוספה רגיל.
     * לא null = מצב עריכה — כפתור השמירה יעדכן במקום להוסיף.
     */
    private String editingChildId = null;

    /**
     * Base64 של התמונה הנוכחית של הילד שנערך.
     * נשמר בזמן enterEditMode() כדי שאם ההורה לא בוחר תמונה חדשה,
     * נוכל לשמור את התמונה הישנה במקום לאבד אותה.
     */
    private String editingChildOldImageBase64 = null;

    /**
     * בוחר תמונה מהגלריה לפרופיל הילד.
     * אותו דפוס בדיוק כמו ב-ParentDashboardActivity — ImageHelper מטפל ב-EXIF.
     */
    private final ActivityResultLauncher<String> childImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                // טוען + מתקן EXIF + מקטין — שלושה שלבים דרך ImageHelper
                Bitmap bitmap = ImageHelper.loadCorrectedBitmap(getContentResolver(), uri);
                if (bitmap != null) {
                    childPhotosBitmap = bitmap;
                    // מציג בצורה עגולה בתצוגה המקדימה — עקבי עם שאר המסכים
                    imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(bitmap));
                } else {
                    Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                }
            });

    // --- נתוני רשימת ילדים ---
    /** רשימת ילדים — כל אחד מכיל id, firstName, lastName, profileImageBase64 */
    private final List<ChildItem> childItems = new ArrayList<>();
    /**
     * אדפטר מותאם אישית — מציג תמונה + שם לכל ילד ברשימה.
     * משתמש ב-childItems ישירות, ללא רשימת שמות מקבילה נפרדת.
     */
    private ChildListAdapter childListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // חיבור תמונת הפרופיל + כפתור בחירת תמונה
        imgChildPhoto = findViewById(R.id.imgChildPhoto);
        Button btnPickChildPhoto = findViewById(R.id.btnPickChildPhoto);
        // לחיצה → פותח גלריה (אותו מנגנון כמו בפרופיל ההורה)
        btnPickChildPhoto.setOnClickListener(v -> childImagePicker.launch("image/*"));

        // חיבור שדות טופס ההוספה/עריכה
        etFirstName   = findViewById(R.id.etFirstName);
        etLastName    = findViewById(R.id.etLastName);
        btnAddChild   = findViewById(R.id.btnAddChild);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        // חיבור רשימת ילדים
        lvChildren   = findViewById(R.id.lvChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);

        // reference ל-root של Firebase
        db = FirebaseDatabase.getInstance().getReference();

        // בדיקה שההורה מחובר
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "אינך מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentUID = currentUser.getUid();

        // הגדרת ListView עם אדפטר מותאם שמציג תמונה + שם
        childListAdapter = new ChildListAdapter();
        lvChildren.setAdapter(childListAdapter);

        // לחיצה על ילד ברשימה → תפריט ערוך/מחק
        lvChildren.setOnItemClickListener((parent, view, position, id) ->
                showChildOptionsDialog(position));

        // כפתור הוספה/שמירה — מנותב לפי מצב (הוספה / עריכה)
        btnAddChild.setOnClickListener(v -> {
            if (editingChildId != null) {
                updateExistingChild();
            } else {
                addChild();
            }
        });

        // כפתור ביטול עריכה — מנקה את מצב העריכה וחוזר לטופס ריק
        btnCancelEdit.setOnClickListener(v -> resetForm());

        // טעינת רשימת הילדים מ-Firebase
        loadChildren();
    }

    // =====================================================================
    //  הוספת ילד חדש
    // =====================================================================

    /**
     * שומר ילד חדש ב-Firebase ומרענן את הרשימה.
     *
     * תהליך:
     * 1. בודק שהשדות מלאים.
     * 2. push().getKey() — מזהה ייחודי.
     * 3. אם נבחרה תמונה — ממיר ל-Base64 ושומר עם הילד.
     * 4. setValue — כותב ל-Firebase.
     * 5. מנקה שדות + מאפס תמונה + טוען מחדש את הרשימה.
     *
     * נתיב כתיבה: /parents/{parentUID}/children/{childId}
     */
    private void addChild() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה ייחודי
        String childId = db.child("parents").child(parentUID)
                .child("children").push().getKey();
        if (childId == null) {
            Toast.makeText(this, "שגיאה ביצירת מזהה ילד", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם נבחרה תמונה — ממיר ל-Base64 (אותו תהליך כמו בתבניות)
        String imageBase64 = null;
        if (childPhotosBitmap != null) {
            imageBase64 = ImageHelper.bitmapToBase64(childPhotosBitmap);
        }

        // יוצר ילד עם תמונה (יכול להיות null — זה בסדר)
        Child newChild = new Child(first, last, imageBase64);

        // משבית כפתור למניעת לחיצות כפולות
        btnAddChild.setEnabled(false);

        db.child("parents")
                .child(parentUID)
                .child("children")
                .child(childId)
                .setValue(newChild)
                .addOnCompleteListener(task -> {
                    btnAddChild.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(ManageChildrenActivity.this,
                                first + " " + last + " נוסף בהצלחה!",
                                Toast.LENGTH_SHORT).show();
                        resetForm();
                        loadChildren();
                    } else {
                        Toast.makeText(this,
                                "שגיאה: " + (task.getException() != null
                                        ? task.getException().getMessage() : "לא ידועה"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // =====================================================================
    //  עריכת ילד קיים
    // =====================================================================

    /**
     * מכניס את הטופס למצב עריכה עבור ילד קיים.
     *
     * ממלא את השדות בנתונים הנוכחיים ומציג תמונה קיימת בצורה עגולה (אם יש).
     * משנה את הכפתור מ"הוסף ילד" ל"שמור שינויים" ומגלה את כפתור הביטול.
     *
     * @param position אינדקס הילד ברשימה
     */
    private void enterEditMode(int position) {
        ChildItem item = childItems.get(position);

        // שמירת מזהה הילד הנערך + תמונה ישנה (לשימוש אם לא נבחרה תמונה חדשה)
        editingChildId = item.id;
        editingChildOldImageBase64 = item.profileImageBase64;

        // מילוי שדות הטופס בנתונים הנוכחיים
        etFirstName.setText(item.firstName != null ? item.firstName : "");
        etLastName.setText(item.lastName  != null ? item.lastName  : "");

        // הצגת תמונה קיימת בצורה עגולה (אם יש) — עקבי עם שאר המסכים
        childPhotosBitmap = null; // מאפס תמונה חדשה — אם ההורה לא יבחר חדשה, נשמר הישן
        if (item.profileImageBase64 != null && !item.profileImageBase64.isEmpty()) {
            Bitmap raw = ImageHelper.base64ToBitmap(item.profileImageBase64);
            if (raw != null) {
                // חיתוך עגול לתצוגה מקדימה — עקבי עם כרטיסי הדשבורד
                imgChildPhoto.setImageBitmap(ImageHelper.getCircularBitmap(raw));
            } else {
                imgChildPhoto.setImageBitmap(null);
                imgChildPhoto.setBackgroundColor(0xFFCCCCCC);
            }
        } else {
            imgChildPhoto.setImageBitmap(null);
            imgChildPhoto.setBackgroundColor(0xFFCCCCCC);
        }

        // שינוי כפתורים למצב עריכה
        btnAddChild.setText("שמור שינויים");
        btnAddChild.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1976D2)); // כחול
        btnCancelEdit.setVisibility(View.VISIBLE);

        // גלילה לראש המסך כדי שהטופס יהיה גלוי
        findViewById(R.id.btnPickChildPhoto).requestFocus();
    }

    /**
     * מאפס את הטופס למצב הוספה רגיל.
     * מנקה שדות, תמונה, ומחזיר כפתורים למצבם המקורי.
     */
    private void resetForm() {
        editingChildId = null;
        editingChildOldImageBase64 = null;

        etFirstName.setText("");
        etLastName.setText("");
        etFirstName.requestFocus();

        childPhotosBitmap = null;
        imgChildPhoto.setImageBitmap(null);
        imgChildPhoto.setBackgroundColor(0xFFCCCCCC);

        btnAddChild.setText(getString(R.string.add_child));
        btnAddChild.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // ירוק
        btnCancelEdit.setVisibility(View.GONE);
    }

    /**
     * שומר שינויים של ילד קיים ב-Firebase.
     *
     * משתמש ב-updateChildren() ולא ב-setValue() — כדי לא לדרוס
     * שדות אחרים כמו tasks/ שנמצאים תחת אותו node של הילד.
     *
     * אם נבחרה תמונה חדשה — שומר אותה. אחרת — שומר את הישנה.
     *
     * נתיב עדכון: /parents/{parentUID}/children/{editingChildId}
     */
    private void updateExistingChild() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // בחירת תמונה: חדשה שנבחרה > ישנה מ-Firebase > null (ללא תמונה)
        String imageBase64;
        if (childPhotosBitmap != null) {
            imageBase64 = ImageHelper.bitmapToBase64(childPhotosBitmap);
        } else {
            imageBase64 = editingChildOldImageBase64; // שומר את הישנה
        }

        // בונה map לעדכון — updateChildren לא נוגע בשדות שלא ציינו (כמו tasks)
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName",  last);
        if (imageBase64 != null) {
            updates.put("profileImageBase64", imageBase64);
        }

        btnAddChild.setEnabled(false);

        db.child("parents").child(parentUID)
                .child("children").child(editingChildId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(this,
                            first + " " + last + " עודכן בהצלחה!",
                            Toast.LENGTH_SHORT).show();
                    resetForm();
                    loadChildren(); // רענון הרשימה
                })
                .addOnFailureListener(e -> {
                    btnAddChild.setEnabled(true);
                    Toast.makeText(this,
                            "שגיאה: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // =====================================================================
    //  טעינת רשימת ילדים מ-Firebase
    // =====================================================================

    /**
     * קורא את כל הילדים מ-/parents/{parentUID}/children/ ומציג ב-ListView.
     * טוען גם את profileImageBase64 לכל ילד (לשימוש בתצוגת הרשימה ובעריכה).
     * מטפל גם במצב ריק (אין ילדים — מציג הודעה).
     */
    private void loadChildren() {
        db.child("parents").child(parentUID).child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childItems.clear();

                        // עוברים על כל הילדים ושומרים את הנתונים (כולל תמונה)
                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String cId = childSnap.getKey();
                            if (cId == null) continue;

                            String first   = childSnap.child("firstName").getValue(String.class);
                            String last    = childSnap.child("lastName").getValue(String.class);
                            String imgB64  = childSnap.child("profileImageBase64").getValue(String.class);

                            childItems.add(new ChildItem(cId, first, last, imgB64));
                        }

                        // עדכון ה-ListView
                        childListAdapter.notifyDataSetChanged();

                        // הצגה/הסתרה של הודעת "אין ילדים"
                        if (childItems.isEmpty()) {
                            tvNoChildren.setVisibility(View.VISIBLE);
                            lvChildren.setVisibility(View.GONE);
                        } else {
                            tvNoChildren.setVisibility(View.GONE);
                            lvChildren.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ManageChildrenActivity.this,
                                "שגיאה בטעינת ילדים: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =====================================================================
    //  תפריט פעולות לילד (ערוך / מחק)
    // =====================================================================

    /**
     * פותח תפריט עם "ערוך ילד" / "מחק ילד".
     * נקרא כשלוחצים על ילד ברשימה.
     */
    private void showChildOptionsDialog(int position) {
        if (position < 0 || position >= childItems.size()) return;

        ChildItem item = childItems.get(position);
        String name = NameUtils.fullNameOrDefault(item.firstName, item.lastName, "ילד");

        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(new String[]{"ערוך ילד", "מחק ילד"}, (dialog, which) -> {
                    if (which == 0) {
                        enterEditMode(position); // מעבר למצב עריכה בטופס
                    } else {
                        showDeleteChildDialog(position);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  מחיקת ילד
    // =====================================================================

    /**
     * מבקש אישור מההורה ומוחק את הילד מ-Firebase.
     * שים לב: מחיקת הילד מוחקת גם את כל המשימות שלו (כי הן תחת אותו node).
     *
     * נתיב מחיקה: /parents/{parentUID}/children/{childId} (כולל tasks/)
     */
    private void showDeleteChildDialog(int position) {
        ChildItem item = childItems.get(position);
        String name = NameUtils.fullNameOrDefault(item.firstName, item.lastName, "ילד");

        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד")
                .setMessage("למחוק את " + name + "?\nכל המשימות שלו יימחקו גם!")
                .setPositiveButton("מחק", (dialog, which) ->
                        db.child("parents").child(parentUID)
                                .child("children").child(item.id)
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, name + " נמחק",
                                            Toast.LENGTH_SHORT).show();
                                    // אם מחקנו את הילד שאנחנו עורכים — מאפסים מצב עריכה
                                    if (item.id.equals(editingChildId)) resetForm();
                                    loadChildren(); // רענון הרשימה
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "שגיאה: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  Adapter מותאם — מציג תמונה + שם לכל ילד ברשימה
    // =====================================================================

    /**
     * Adapter שמציג שורת ילד עם תמונת פרופיל עגולה ושם מלא.
     *
     * משתמש ב-childItems ישירות — אין רשימת שמות מקבילה.
     * Layout: item_manage_child.xml
     */
    private class ChildListAdapter extends ArrayAdapter<ChildItem> {

        ChildListAdapter() {
            super(ManageChildrenActivity.this, 0, childItems);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // שימוש חוזר ב-View אם קיים
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        R.layout.item_manage_child, parent, false);
            }

            ChildItem item = getItem(position);
            if (item == null) return convertView;

            ImageView ivThumb = convertView.findViewById(R.id.ivChildThumb);
            TextView tvName   = convertView.findViewById(R.id.tvChildFullName);

            tvName.setText(NameUtils.fullNameOrDefault(item.firstName, item.lastName, "ילד"));

            // טעינת תמונת פרופיל עגולה — null אם אין
            if (item.profileImageBase64 != null && !item.profileImageBase64.isEmpty()) {
                Bitmap raw = ImageHelper.base64ToBitmap(item.profileImageBase64);
                if (raw != null) {
                    ivThumb.setImageBitmap(ImageHelper.getCircularBitmap(raw));
                } else {
                    ivThumb.setImageBitmap(null);
                }
            } else {
                ivThumb.setImageBitmap(null);
            }

            return convertView;
        }
    }

    // =====================================================================
    //  מחלקה פנימית — מחזיקה נתוני ילד מהרשימה
    // =====================================================================

    /**
     * שומרת id + שם פרטי + שם משפחה + תמונה (Base64) של ילד.
     * משמשת את הרשימה כדי שנדע איזה ילד לערוך/למחוק בלי לקרוא שוב מ-Firebase.
     */
    private static class ChildItem {
        final String id;
        final String firstName;
        final String lastName;
        /** Base64 של תמונת הפרופיל — יכול להיות null אם אין תמונה */
        final String profileImageBase64;

        ChildItem(String id, String firstName, String lastName, String profileImageBase64) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.profileImageBase64 = profileImageBase64;
        }
    }
}
