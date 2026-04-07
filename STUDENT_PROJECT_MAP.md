# מפת פרויקט לסטודנט

מטרת המסמך:
לתת תמונה מהירה וברורה של מבנה הפרויקט, בלי שצריך לשחזר לבד מי פותח מה, איפה נשמרים הנתונים, ואילו קבצים הם המרכזיים.

הערת כיוון חשובה:
יש בפרויקט שמות חבילות עם כתיב לא אחיד, למשל `Parents_Dashbord_and_mange`, `Child_Login`, `FireBase`. זה המצב הנוכחי של ה־working tree, ולכן המסמך מתאר אותו כמו שהוא ולא מציע שינויי naming.

## 1. package-by-package map

### `com.example.family_tasks_proj.auth`

- `MainActivity`
- זהו ה־launcher של האפליקציה.
- טוען את `activity_main.xml`.
- מחזיק `fragmentContainer` ומחליף בתוכו את מסכי ההורה/QR כ־Fragments.
- נותן גם כניסה ישירה למסך בחירת ילד.

### `com.example.family_tasks_proj.Parents`

- `ParentLoginFragment`
- `ParentRegisterFragment`
- זו חבילת ה־auth של ההורה.
- שני ה־Fragments האלה לא עומדים לבד; הם נטענים בתוך `MainActivity`.

### `com.example.family_tasks_proj.Parents_Dashbord_and_mange`

- `ParentDashboardActivity`
- `ManageChildrenActivity`
- `ParentTaskTemplateActivity`
- `AssignTaskToChildActivity`
- `GenerateQRActivity`
- `ParentDashboardTaskAdapter`
- `ParentDashboardChildSummaryAdapter`
- זו חבילת כל מסכי ההורה אחרי התחברות.
- כאן נמצאים גם ה־adapters שמציגים את רשימות הדשבורד של ההורה.

### `com.example.family_tasks_proj.Parents_Dashbord_and_mange.model`

- `AssignedTask`
- `ChildSummary`
- `TaskListItem`
- `TaskTemplate`
- אלו המודלים של צד ההורה: משימות שהוקצו, סיכומי ילדים, שורות לרשימת דשבורד, ותבניות משימה.

### `com.example.family_tasks_proj.Child_Login`

- `ChildQRLoginFragment`
- `ChildSelectionActivity`
- זו חבילת הכניסה של הילד.
- היא מטפלת בסריקת QR, אימות הנתונים מול Firebase, בחירת הורה אם צריך, ובחירת ילד.

### `com.example.family_tasks_proj.child`

- `ChildDashboardActivity`
- `ChildTaskAdapter`
- זו חבילת המסך הראשי של הילד.
- כאן נמצאים גם לוגיקת הסינון וה־adapter של כרטיסי המשימות.

### `com.example.family_tasks_proj.child.model`

- `ChildTask`
- כרגע אין כאן `Child.java`.
- זה חשוב כי `ManageChildrenActivity` עדיין מייבא `com.example.family_tasks_proj.child.model.Child`, וזה ה־compile blocker הנוכחי.

### `com.example.family_tasks_proj.FireBase`

- `FBsingleton`
- `ParentInFb`
- זו חבילה קטנה שמרכזת את מודל ההורה ואת הכתיבה הראשונית של נתוני ההורה ל־Firebase.

### `com.example.family_tasks_proj.util`

- `DateUtils`
- `ImageHelper`
- `NameUtils`
- חבילת helpers כללית, בלי תלות במסך אחד בלבד.

## 2. main screens and their roles

- `MainActivity` + `activity_main.xml`
- מסך הבית הראשון.
- מציג כפתורים להתחברות הורה, הרשמת הורה, סריקת QR לילד, וכניסה ישירה למסך בחירת ילד.
- מחליף Fragments בתוך `fragmentContainer`.

- `ParentLoginFragment` + `fragment_parent_login.xml`
- התחברות הורה דרך `FirebaseAuth`.
- בכניסה מוצלחת פותח את `ParentDashboardActivity`.

- `ParentRegisterFragment` + `fragment_parent_register.xml`
- יצירת חשבון הורה חדש.
- יוצר משתמש ב־`FirebaseAuth`, ואז שומר פרופיל הורה ב־Realtime Database דרך `FBsingleton`.

- `ParentDashboardActivity` + `activity_parent_dashboard.xml`
- המסך הראשי של ההורה אחרי login.
- מציג פרטי הורה, סיכומי משימות, כרטיסי ילדים, רשימת משימות, וכפתורי ניווט למסכי ניהול.

- `ManageChildrenActivity` + `activity_manage_children.xml`
- CRUD של ילדים.
- הוספה, עריכה, מחיקה, ותמונת ילד.
- כרגע זה גם המסך שבו יש שגיאת קומפילציה בגלל import למודל `Child` שלא קיים.

- `ParentTaskTemplateActivity` + `activity_parent_task_template.xml`
- ניהול תבניות משימה.
- יצירה, עריכה ומחיקה של תבניות עם כותרת ותמונה.

- `AssignTaskToChildActivity` + `activity_assign_task_to_child.xml`
- הקצאת משימה בפועל לילד.
- בוחר תבנית, ילד, ותאריך יעד, ואז כותב משימה תחת הילד ב־Firebase.

- `GenerateQRActivity` + `activity_generate_qr.xml`
- יוצר QR קבוע של ההורה המחובר.
- הפורמט הוא `parent:{parentId}`.

- `ChildQRLoginFragment` + `fragment_child_q_r_login.xml`
- סורק QR.
- יודע לפענח גם `parent:{parentId}` וגם `parent:{parentId}|child:{childId}`.
- מאמת מול Firebase ואז מעביר ל־`ChildSelectionActivity`.

- `ChildSelectionActivity` + `activity_child_selection.xml`
- מסך בחירת הורה וילד.
- אם `parentId` כבר הגיע מ־QR או מה־session, הוא מדלג על בחירת הורה ומציג רק את הילדים הרלוונטיים.

- `ChildDashboardActivity` + `activity_child_dashboard.xml`
- המסך הראשי של הילד.
- טוען שם, אווטאר, כמות כוכבים ומשימות.
- תומך בפילטרים: פתוחות, הושלמו, דחופות.
- מאפשר סימון משימה כבוצעה.

## 3. important models and what they represent

- `TaskTemplate`
- מייצג תבנית משימה שהורה שומר כדי להקצות שוב בעתיד.
- נשמר תחת `/parents/{uid}/task_templates/{templateId}`.
- כולל `id`, `title`, `imageBase64`.

- `AssignedTask`
- מייצג משימה שהוקצתה כבר לילד, מצד הדשבורד של ההורה.
- כולל גם מידע על הילד וגם מידע על המשימה.

- `ChildSummary`
- מייצג שורת סיכום של ילד בדשבורד ההורה.
- כולל `childId`, שם תצוגה, תמונת ילד, ומוני משימות.

- `TaskListItem`
- לא מודל של Firebase.
- זהו wrapper לתצוגת רשימת משימות אצל ההורה.
- כל פריט הוא או header של קבוצה או משימה רגילה.

- `ChildTask`
- מודל המשימה מצד הילד.
- נשמר תחת `/parents/{uid}/children/{childId}/tasks/{taskId}`.
- כולל `title`, `dueAt`, `isDone`, `starsWorth`, `imageBase64`, `createdAt`.

- `ParentInFb`
- מודל פרופיל ההורה כפי שהוא נשמר ב־Realtime Database.
- כולל `uid`, `firstName`, `lastName`, `email`, `role`, `children`, ו־`profileImageBase64`.

- מודלים פנימיים בתוך קבצים גדולים
- `ManageChildrenActivity.ChildItem` הוא data holder פנימי לרשימת הילדים במסך הניהול.
- `ChildSelectionActivity.ParentItem` ו־`ChildSelectionActivity.ChildItem` הם data holders פנימיים ל־Spinnerים.
- `ChildQRLoginFragment.ParsedQr` הוא data holder קטן לפענוח ה־QR.

- הערה חשובה על מודל חסר
- כרגע אין קובץ `child/model/Child.java`.
- למרות זאת, `ManageChildrenActivity` עדיין יוצר `new Child(...)`.
- לכן Claude לא צריך לחפש את המודל הזה בכל הפרויקט; הוא באמת חסר כרגע.

## 4. important Firebase-related classes

### מחלקות מרכזיות

- `FBsingleton`
- גישה מרכזית ל־`FirebaseAuth` ול־`FirebaseDatabase`.
- שומר זמנית את נתוני ההורה בזיכרון.
- כותב פרופיל הורה ל־Firebase דרך `updateChildren`, כדי לא לדרוס צמתים קיימים.

- `ParentInFb`
- מגדיר את מבנה פרופיל ההורה בצומת `/parents/{uid}`.

### מסכים שעובדים ישירות מול Firebase

- `ParentRegisterFragment`
- יוצר משתמש ב־Auth ושומר פרופיל הורה ב־DB.

- `ParentDashboardActivity`
- טוען פרופיל הורה, תמונת פרופיל, ילדים, ומשימות.
- גם מבצע עדכונים/מחיקות עבור משימות מתוך הדשבורד.

- `ManageChildrenActivity`
- עובד מול `/parents/{uid}/children/{childId}`.
- מוסיף, מעדכן ומוחק ילדים.

- `ParentTaskTemplateActivity`
- עובד מול `/parents/{uid}/task_templates/{templateId}`.

- `AssignTaskToChildActivity`
- טוען תבניות וילדים מהורה קיים.
- כותב משימות תחת `/parents/{uid}/children/{childId}/tasks/{taskId}`.

- `ChildQRLoginFragment`
- בודק אם parent או child קיימים ב־Firebase לפי תוכן ה־QR.

- `ChildSelectionActivity`
- טוען רשימת הורים ורשימת ילדים לפי parent.

- `ChildDashboardActivity`
- טוען header של הילד ומשימותיו.
- מעדכן `isDone` של משימה כשהילד מסמן ביצוע.

### נתיבי Firebase החשובים באמת

- `/parents/{uid}`
- פרופיל הורה.

- `/parents/{uid}/children/{childId}`
- פרטי ילד.

- `/parents/{uid}/children/{childId}/tasks/{taskId}`
- משימות שהוקצו לילד.

- `/parents/{uid}/task_templates/{templateId}`
- תבניות משימה.

## 5. important helpers/utilities

- `ImageHelper`
- מחלקת helper חשובה מאוד.
- מטפלת בטעינת תמונה מ־Uri, תיקון EXIF, הקטנה, המרה ל־Base64, המרה מ־Base64, וחיתוך עגול.
- משמשת גם לתמונות פרופיל וגם לתמונות משימה.

- `DateUtils`
- מרכז את לוגיקת התאריכים.
- מחזיר כמה ימים נשארו, ואם משימה נחשבת דחופה.
- נמצא בשימוש חזק בצד הילד.

- `NameUtils`
- מחבר `firstName + lastName` בצורה null-safe.
- נותן fallback כשאין שם תקין.
- חשוב בעיקר למסכים שמרכיבים שמות ישויות שמגיעות מ־Firebase.

## 6. parent flow summary

1. ההורה נכנס ל־`MainActivity`.
2. הוא בוחר `ParentLoginFragment` או `ParentRegisterFragment`.
3. אחרי הצלחה נפתח `ParentDashboardActivity`.
4. מתוך הדשבורד הוא יכול:
5. לעבור ל־`ManageChildrenActivity` כדי לנהל ילדים.
6. לעבור ל־`ParentTaskTemplateActivity` כדי לנהל תבניות.
7. לעבור ל־`AssignTaskToChildActivity` כדי להקצות משימה.
8. לעבור ל־`GenerateQRActivity` כדי להציג QR לילדים.
9. לחזור ל־`MainActivity` דרך logout.

## 7. child flow summary

1. הילד מתחיל ב־`MainActivity`.
2. הוא יכול לבחור כניסה דרך `ChildQRLoginFragment`.
3. או לבחור כניסה ישירה ל־`ChildSelectionActivity`.
4. אם נסרק QR, ה־fragment מוודא שה־parent או ה־child קיימים ב־Firebase.
5. אחר כך נפתח `ChildSelectionActivity`.
6. אם כבר יש `parentId`, הילד בוחר רק את עצמו.
7. אם אין `parentId`, בוחרים קודם הורה ואז ילד.
8. המסך שומר `parentId` ו־`childId` ב־`SharedPreferences` בשם `child_session`.
9. אחר כך נפתח `ChildDashboardActivity`.
10. שם הילד רואה משימות, מסנן אותן, ומסמן משימה כבוצעה.
11. logout מחזיר ל־`MainActivity`.

## 8. files that are still too large

- `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ParentDashboardActivity.java` — בערך 609 שורות
- `app/src/main/res/layout/activity_parent_dashboard.xml` — בערך 480 שורות
- `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ManageChildrenActivity.java` — בערך 363 שורות
- `app/src/main/java/com/example/family_tasks_proj/child/ChildDashboardActivity.java` — בערך 360 שורות
- `app/src/main/java/com/example/family_tasks_proj/Child_Login/ChildSelectionActivity.java` — בערך 284 שורות
- `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/AssignTaskToChildActivity.java` — בערך 274 שורות
- `app/src/main/res/layout/activity_child_dashboard.xml` — בערך 248 שורות
- `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ParentTaskTemplateActivity.java` — בערך 240 שורות

## 9. files that Claude should review next

- `ManageChildrenActivity`
- זה היעד הראשון, כי הוא גם גדול וגם מכיל את שגיאת הקומפילציה הנוכחית סביב `Child`.

- `ParentDashboardActivity`
- זה המסך המרכזי והעמוס ביותר בצד ההורה.
- הוא גם טוען נתונים, גם מסנן, גם מציג summary, וגם מפעיל מסכי משנה.

- `ChildDashboardActivity`
- זה המסך המרכזי של צד הילד.
- יש בו session restore, פילטרים, טעינת header, טעינת משימות ועדכון `isDone`.

- `ChildSelectionActivity`
- זה קובץ חשוב כי הוא יושב בדיוק בין QR/session לבין המסך של הילד.

- `AssignTaskToChildActivity`
- הוא קושר בין תבניות, ילדים, תאריך יעד וכתיבה ל־Firebase.

- `ParentTaskTemplateActivity`
- חשוב להבין אותו כי הרבה לוגיקה של תמונות ו־Base64 מתחילה שם.

- `ParentDashboardTaskAdapter`
- חשוב לבדוק אם לוגיקת התצוגה של רשימת משימות אצל ההורה נשארה פשוטה וברורה.

- `ParentDashboardChildSummaryAdapter`
- חשוב אם צריך לגעת בצד כרטיסי הילדים/summary של ההורה.

- `activity_parent_dashboard.xml`
- layout גדול מאוד עם הרבה אזורים שונים.

- `activity_child_dashboard.xml`
- layout מרכזי למסך הילד, שכבר עבר cleanup במשאבים.

## 10. short manual demo checklist for the student

לפני הדמו:
המצב הנוכחי של ה־working tree עדיין נתקע ב־compile על `ManageChildrenActivity`, לכן צריך קודם לפתור את זה או לעבוד על build שבו זה כבר תוקן.

צ'קליסט קצר:

1. לפתוח את מסך הבית ולהראות את כל אפשרויות הכניסה.
2. לבצע הרשמת הורה או login של הורה.
3. להיכנס לדשבורד הורה.
4. להוסיף ילד חדש.
5. לערוך ילד קיים.
6. ליצור תבנית משימה.
7. להקצות משימה לילד מתוך תבנית.
8. לפתוח את מסך ה־QR ולהראות מה ההורה מציג לילד.
9. לעבור למסלול של הילד, לסרוק QR או לבחור ילד ישירות.
10. לפתוח את דשבורד הילד.
11. לסנן משימות.
12. לסמן משימה כבוצעה.
13. לבצע logout של הילד ושל ההורה.
