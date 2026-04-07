# דוח ביקורת משאבי UI/XML

תאריך בדיקה: 2026-04-07

היקף:
- `app/src/main/res/layout/*.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/*.xml`
- `app/src/main/res/xml/*.xml`
- `app/src/main/AndroidManifest.xml` רק בהקשר של משאבים ו־layout/resource references

## 1. Mechanical UI/resource issues found

- לא נמצאו `hardcoded strings` ב־layout XML עבור `text` / `hint` / `contentDescription`.
- לא נמצא `ImageView` משמעותי בלי `contentDescription`, ולא קיימים `ImageButton` בכלל.
- לא נמצאו בעיות RTL מכניות ברמת attributes: אין שימוש ב־`Left/Right`, וברוב הקבצים נעשה שימוש ב־`Start/End`.
- לא קיימת תיקיית `res/menu`, ולכן לא היו `menu resources` לבדיקה.
- `AndroidManifest.xml` מפנה תקין ל־`@xml/data_extraction_rules`, `@xml/backup_rules`, `@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`, ו־`@string/app_name`.
- כן נמצאו שני סוגי ממצאים מכניים ברורים:
- `app/src/main/res/layout/activity_child_dashboard.xml`: לאווטאר הילד היה `@drawable/bg_avatar_placeholder` עגול, אבל בלי `android:clipToOutline="true"`, ולכן התמונה עצמה לא נחתכה לעיגול.
- `app/src/main/res/values/strings.xml`: נמצאו 5 strings ללא שום reference בכל הריפו, ולכן הם היו מועמדים בטוחים לניקוי:
- `title_parent_dashboard`
- `empty_no_tasks_child`
- `parent_dashboard_child_counts`
- `error_unknown_generic`
- `cd_status_dot`

## 2. Tiny fixes applied

- נוסף `android:clipToOutline="true"` ל־`imgChildAvatar` ב־`activity_child_dashboard.xml`.
- הוסרו 5 strings לא־בשימוש מ־`strings.xml`.
- לא בוצעו שינויי Java, לא בוצעו שינויי layout behavior, ולא בוצע redesign.

## 3. Exact patches

```diff
diff --git a/app/src/main/res/layout/activity_child_dashboard.xml b/app/src/main/res/layout/activity_child_dashboard.xml
index 28fec40..96af823 100644
--- a/app/src/main/res/layout/activity_child_dashboard.xml
+++ b/app/src/main/res/layout/activity_child_dashboard.xml
@@ -32,6 +32,7 @@
                     android:layout_height="90dp"
                     android:layout_marginBottom="12dp"
                     android:background="@drawable/bg_avatar_placeholder"
+                    android:clipToOutline="true"
                     android:contentDescription="@string/cd_child_avatar"
                     android:scaleType="centerCrop"
                     android:src="@android:drawable/ic_menu_my_calendar" />
diff --git a/app/src/main/res/values/strings.xml b/app/src/main/res/values/strings.xml
index 1136400..e48fbfd 100644
--- a/app/src/main/res/values/strings.xml
+++ b/app/src/main/res/values/strings.xml
@@ -47,7 +47,6 @@
     <string name="title_assign_task">הקצאת משימה מהמאגר</string>
     <string name="title_qr_family">QR כניסה למשפחה</string>
     <string name="title_child_login">כניסת ילד</string>
-    <string name="title_parent_dashboard">דשבורד הורה</string>
 
     <string name="desc_parent_login">התחברו כדי לנהל ילדים, להקצות משימות ולעקוב אחרי ההתקדמות.</string>
     <string name="desc_parent_register">פתחו חשבון חדש והתחילו לנהל משימות משפחתיות בצורה מסודרת ונעימה.</string>
@@ -61,7 +60,6 @@
 
     <string name="empty_no_children">אין ילדים רשומים עדיין. הוסף ילד למעלה.</string>
     <string name="empty_no_children_selection">אין ילדים רשומים. בקש מההורה להוסיף אותך.</string>
-    <string name="empty_no_tasks_child">אין משימות כרגע. ההורה יוסיף בקרוב.</string>
     <string name="child_no_tasks_open">אין משימות פתוחות כרגע.</string>
     <string name="child_no_tasks_completed">אין משימות שהושלמו כרגע.</string>
     <string name="child_no_tasks_urgent">אין משימות דחופות כרגע.</string>
@@ -139,7 +137,6 @@
     <string name="parent_dashboard_logout_title">התנתקות</string>
     <string name="parent_dashboard_logout_message">האם אתם בטוחים שברצונכם להתנתק?</string>
     <string name="parent_dashboard_logout_confirm">כן, התנתק</string>
-    <string name="parent_dashboard_child_counts">נשלחו: %1$d | בוצעו: %2$d | דחופות: %3$d</string>
     <string name="parent_dashboard_child_content_description">כרטיס ילד: %1$s</string>
     <string name="parent_dashboard_metric_with_count">%1$s %2$d</string>
     <string name="parent_dashboard_summary_total">הכול</string>
@@ -216,7 +213,6 @@
     <string name="error_unknown_register">שגיאת הרשמה לא ידועה</string>
     <string name="error_parent_session_missing">פג תוקף ההתחברות. התחברו מחדש.</string>
     <string name="error_with_details">שגיאה: %1$s</string>
-    <string name="error_unknown_generic">לא ידוע</string>
     <string name="success_parent_register">ההרשמה הצליחה!</string>
     <string name="success_parent_photo_saved">תמונת הפרופיל נשמרה!</string>
 
@@ -229,7 +225,6 @@
     <string name="cd_filter_urgent">סינון משימות דחופות</string>
     <string name="cd_filter_completed">סינון משימות שהושלמו</string>
     <string name="cd_filter_not_completed">סינון משימות שלא הושלמו</string>
-    <string name="cd_status_dot">נקודת סטטוס</string>
 
     <string name="default_task_name">שם המשימה</string>
     <string name="default_parent_name">הורה</string>
```

## 4. Remaining UI/resource issues for Claude

- `app/src/main/res/layout/activity_child_dashboard.xml:237-249`: `tvTaskSectionTitle` ו־`tvNoTasks` מוגדרים עם צבעי טקסט כמעט לבנים (`#FFFFFF`, `#F5F7FA`) מחוץ לכרטיס לבן. זה נראה כמו mismatch חזותי/ברירת־מחדל, אבל לא בטוח לתקן בלי החלטת UI קטנה.
- `app/src/main/res/layout/activity_child_dashboard.xml:38`: לאווטאר הילד יש placeholder של `@android:drawable/ic_menu_my_calendar`. זה חשוד סמנטית לאווטאר, אבל לא תיקון מכני מובהק.
- יש כפילויות טקסט ב־`strings.xml`, אבל לא אוחדו אוטומטית כדי לא לייצר churn בשמות ובקוד:
- `register_parents` / `title_parent_register`
- `parents_login` / `title_parent_login`
- `hint_task_title` / `default_task_name`
- `cd_child_photo` / `cd_child_avatar`
- `filter_urgent` / `child_task_section_urgent`
- `filter_completed` / `child_task_section_completed`
- `filter_not_completed` / `child_task_section_open`

## 5. What Codex should not touch

- לא למחוק את `bg_avatar_placeholder.xml`, `ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `backup_rules.xml`, או `data_extraction_rules.xml`; כולם בשימוש מוכח.
- לא להמציא `menu resources` רק כדי "לסגור audit"; אין `res/menu` כרגע.
- לא לאחד שמות strings כפולים אם זה דורש שינויי Java או churn לא נחוץ.
- לא לגעת בלוגיקת `visibility`/default state בלי הוכחה מהקוד.
- לא לעשות redesign לצבעים, spacing, או hierarchy במסגרת cleanup מכני.
