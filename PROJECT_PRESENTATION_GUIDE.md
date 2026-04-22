# מדריך הצגת הפרויקט

## 1. תקציר הפרויקט

האפליקציה היא מערכת לניהול משימות משפחתיות. ההורה מנהל ילדים, יוצר תבניות משימה, משייך משימות לילדים ועוקב אחרי ההתקדמות. הילד נכנס עם QR או session שמור, רואה את המשימות שלו, מסנן לפי מצב ומסמן משימות שבוצעו.

המשתמשים הם:
- הורה - מנהל את הבית, הילדים, התבניות והמשימות.
- ילד - רואה רק את המשימות שלו ומסמן ביצוע.

הבעיה שהאפליקציה פותרת: במקום שהורה ינהל משימות ביתיות בעל פה או בפתקים, יש מקום אחד מסודר שבו כל ילד רואה מה עליו לעשות, וההורה רואה תמונת מצב של כל הבית.

## 2. זרימות מרכזיות באפליקציה

### זרימת הורה

`login/register -> ParentDashboard -> ManageChildren -> ParentTaskTemplate -> AssignTaskToChild -> GenerateQR`

מה להסביר:
- ההורה נרשם או מתחבר דרך FirebaseAuth.
- אחרי התחברות נפתח `ParentDashboardActivity`.
- מהדשבורד ההורה מנהל ילדים, תבניות, שליחת משימות ו-QR משפחתי.

### זרימת ילד

`QR / saved session -> ChildSelectionActivity / ChildDashboardActivity -> filter tasks -> mark done`

מה להסביר:
- ילד סורק QR שמזהה את המשפחה.
- אם ה-QR כולל רק הורה, הילד בוחר את שמו.
- אם כבר נשמר session מלא, הילד יכול להמשיך ישירות לדשבורד.
- בדשבורד הילד רואה פתוחות, דחופות והושלמו, ומסמן משימות כבוצעו.

### מצב כל הילדים

`ParentDashboard -> כל הילדים -> צפייה במשימות מכל הילדים`

מה להסביר:
- זה לא מסך חדש ולא נתיב Firebase חדש.
- בדשבורד ההורה יש צ'יפ סינתטי בשם "כל הילדים".
- כשבוחרים אותו, הקוד מציג משימות מכל הילדים ומוסיף לכל שורה את שם הילד.

## 3. הסבר Firebase

שורש הנתונים המרכזי הוא:

```text
/parents/{uid}
```

מבנה הנתונים:

```text
/parents/{uid}
  firstName
  lastName
  email
  role
  profileImageBase64

  /children/{childId}
    firstName
    lastName
    profileImageBase64

    /tasks/{taskId}
      title
      dueAt
      isDone
      starsWorth
      imageBase64
      createdAt

  /task_templates/{templateId}
    id
    title
    imageBase64
    starsWorth
```

איך להסביר:
- ההורה הוא השורש הראשי כי כל משפחה שייכת להורה מחובר.
- ילדים נשמרים תחת ההורה.
- משימות נשמרות תחת ילד ספציפי.
- תבניות נשמרות תחת ההורה כי הן משותפות לכל הילדים.
- כוכבים נשמרים בשדה `starsWorth` בתוך התבנית, ואז מועתקים למשימה שנשלחת לילד.

קבצים חשובים:
- `FBsingleton.java` - שמירת פרופיל הורה עם `updateChildren`.
- `ManageChildrenActivity.java` - נתיב הילדים.
- `ParentTaskTemplateActivity.java` - נתיב התבניות.
- `AssignTaskToChildActivity.java` - כתיבת משימה לילד.
- `ChildDashboardActivity.java` - קריאת משימות הילד.
- `ParentDashboardActivity.java` - קריאת כל הילדים והמשימות לדשבורד ההורה.

## 4. הסבר QR

פורמט ה-QR הראשי:

```text
parent:{parentId}
```

פורמט נוסף שנתמך לתאימות:

```text
parent:{parentId}|child:{childId}
```

מה קורה בסריקה:
- `ChildQRLoginFragment` מקבל את הטקסט מהסורק.
- הפונקציה `parseQr` מחלצת `parentId` ו-`childId` אם קיים.
- אם יש רק `parentId`, הקוד בודק שההורה קיים ואז פותח בחירת ילד.
- אם יש גם `childId`, הקוד בודק שהילד קיים תחת ההורה ואז נכנס ישר לדשבורד הילד.

ההבדל בין QR משפחתי ל-QR ספציפי:
- QR משפחתי: מזהה את ההורה בלבד, וכל ילד בוחר את שמו.
- QR ספציפי לילד: מזהה גם הורה וגם ילד, ולכן אפשר לדלג על בחירת שם.
- במסך `GenerateQRActivity` נוצר QR משפחתי פשוט, כדי שכל הילדים של אותו הורה יוכלו להשתמש באותו קוד.

## 5. הסבר Session

ה-session של הילד נשמר מקומית ב-`SharedPreferences` בשם:

```text
child_session
```

נשמרים בו:
- `parentId`
- `childId`

למה זה חשוב:
- אחרי כניסה ראשונה, הילד לא חייב לסרוק QR בכל פעם.
- אם יש `parentId` ו-`childId`, אפשר לפתוח ישר את `ChildDashboardActivity`.
- אם יש רק `parentId`, נפתח `ChildSelectionActivity`.
- ביציאה מהילד, `ChildDashboardActivity` מנקה את ה-session כדי שלא ייכנסו בטעות לילד הלא נכון.

## 6. פיצ'רים שמומשו

- התחברות והרשמת הורה - `ParentLoginFragment`, `ParentRegisterFragment`, FirebaseAuth.
- כניסת ילד עם QR - `ChildQRLoginFragment`.
- בחירת ילד - `ChildSelectionActivity`.
- דשבורד ילד - `ChildDashboardActivity`, `ChildTaskAdapter`.
- דשבורד הורה - `ParentDashboardActivity`.
- מצב כל הילדים - צ'יפ סינתטי ב-`ParentDashboardChildSummaryAdapter`.
- הקצאת משימה - `AssignTaskToChildActivity`.
- תבניות משימה - `ParentTaskTemplateActivity`, `TaskTemplate`.
- כוכבים דינמיים - `starsWorth` בתבנית ובמשימה.
- משימות שהושלמו לקריאה בלבד - `ParentDashboardActivity.showTaskOptionsDialog`.
- תמונות ילדים/הורה/משימות - `ImageHelper` ושדות `profileImageBase64` / `imageBase64`.
- Firebase Realtime Database - נתיבים תחת `/parents/{uid}`.
- מצבי ריק - טקסטים במסכי ילד, הורה, בחירת ילד ותבניות.
- UI/UX polish - layouts, styles, colors ו-drawables קיימים.

## 7. מיפוי למחוון הפרויקט

| קריטריון | מה מומש | קבצים שמראים את זה | משפט קצר להצגה |
|---|---|---|---|
| Functionality | הורה מנהל ילדים, תבניות ומשימות; ילד מסמן ביצוע | `ParentDashboardActivity`, `AssignTaskToChildActivity`, `ChildDashboardActivity` | "המימוש מכסה את שני המשתמשים המרכזיים ואת כל זרימת המשימות." |
| Database usage | Firebase Realtime Database עם מבנה היררכי ברור | `FBsingleton`, `ManageChildrenActivity`, `AssignTaskToChildActivity` | "כל משפחה נשמרת תחת `/parents/{uid}` ומשם הילדים והמשימות מסודרים." |
| User roles | הפרדה בין הורה לילד | `MainActivity`, `ParentLoginFragment`, `ChildQRLoginFragment` | "ההורה מתחבר עם חשבון, והילד נכנס דרך QR/session." |
| Input validation / error handling | בדיקות שדות, אימייל, סיסמה, בחירת ילד/תאריך | `ParentRegisterFragment`, `ParentLoginFragment`, `AssignTaskToChildActivity` | "לפני כתיבה ל-Firebase אני בודק שהמידע המינימלי קיים ותקין." |
| UI/UX | מסכים עבריים, כרטיסי תפקיד, פילטרים ומצבי ריק | `activity_main.xml`, `activity_parent_dashboard.xml`, `activity_child_dashboard.xml` | "המסכים מחולקים לפי סיפור ברור: כותרת, תוכן מרכזי ופעולות." |
| Code organization | חבילות לפי תחום: auth, Parents, child, Firebase, util | מבנה `app/src/main/java` | "שמרתי על חלוקה פשוטה שקל להסביר ולנווט בה." |
| Maintainability | עזרי תאריך, שם ותמונה; מודלים פשוטים | `DateUtils`, `NameUtils`, `ImageHelper`, models | "לוגיקה שחוזרת בכמה מקומות נמצאת במחלקות עזר פשוטות." |
| Demonstration readiness | מדריך הצגה, הערות בקוד ומפת ניווט | `PROJECT_PRESENTATION_GUIDE.md`, הערות Java/XML | "אפשר להציג את הפרויקט לפי הזרימות ולא לחפש כל פעם איפה הקוד נמצא." |

## 8. מדריך לבחינה בעל פה

1. איך Firebase עובד אצלך?
   - כל משפחה מתחילה תחת `/parents/{uid}`. מתחת לזה יש ילדים, משימות ותבניות.

2. איך QR מחבר ילד להורה?
   - ה-QR מכיל `parent:{parentId}`. הילד סורק, הקוד בודק שההורה קיים, ואז הילד בוחר את שמו.

3. איך נשמר session?
   - אחרי בחירת ילד נשמרים `parentId` ו-`childId` ב-`SharedPreferences` בשם `child_session`.

4. למה משימה שהושלמה לא ניתנת לעריכה?
   - כדי לשמור על אמינות הנתונים. אחרי שהילד סימן ביצוע, ההורה רואה אותה לקריאה בלבד.

5. איך הכוכבים מחושבים?
   - לכל תבנית יש `starsWorth`. כששולחים משימה לילד, הערך נשמר במשימה, והילד צובר אותו רק אחרי ביצוע.

6. איך ההורה רואה את כל הילדים?
   - הדשבורד טוען את כל הילדים והמשימות, ומוסיף צ'יפ "כל הילדים" שמציג את כולן יחד.

7. איפה בקוד נטענות המשימות?
   - אצל הילד ב-`ChildDashboardActivity.loadTasks`; אצל ההורה ב-`ParentDashboardActivity.loadDashboardData`.

8. מה קורה אם אין משימות?
   - מוצגים מצבי ריק שונים לפי הפילטר, למשל אין פתוחות, אין דחופות או אין שהושלמו.

9. איך מנעת קריסה אם חסר מידע?
   - יש בדיקות `null`, בדיקות שדות ריקים, fallback לשמות ברירת מחדל ותמונות placeholder.

10. איזה חלק הכי מורכב בפרויקט?
    - `ParentDashboardActivity`, כי הוא מחבר ילדים, משימות, סיכומים, פילטרים ומצב כל הילדים במסך אחד.

## 9. מפת ניווט בקוד

| File | Role in project | Important methods | What to say in presentation |
|---|---|---|---|
| `auth/MainActivity.java` | מסך פתיחה וניווט בין הורה לילד | `openChildQuickLogin`, `showFragment` | "כאן מתחילה האפליקציה ובוחרים תפקיד." |
| `Parents/ParentLoginFragment.java` | התחברות הורה | `loginUser`, `setLoading` | "FirebaseAuth מאמת את ההורה לפני הדשבורד." |
| `Parents/ParentRegisterFragment.java` | הרשמת הורה | `registerParent`, `setLoading` | "נוצר משתמש ואז נשמר פרופיל הורה ב-Database." |
| `Child_Login/ChildQRLoginFragment.java` | סריקת QR ופענוח payload | `parseQr`, `checkParentExists`, `checkChildExists` | "כאן QR הופך ל-parentId/childId." |
| `Child_Login/ChildSelectionActivity.java` | בחירת ילד אחרי QR/session | `resolveIds`, `loadParents`, `loadChildren`, `saveSession` | "כאן נשמר session הילד." |
| `child/ChildDashboardActivity.java` | מסך הילד | `resolveSession`, `loadTasks`, `markTaskDone`, `applyFilter` | "הילד רואה משימות, מסנן ומסמן ביצוע." |
| `child/ChildTaskAdapter.java` | הצגת כרטיסי משימה לילד | `bindStars`, `bindDoneButton`, `bindDueDate` | "האדפטר אחראי לאיך כל משימה נראית." |
| `Parents_Dashbord_and_mange/ParentDashboardActivity.java` | דשבורד הורה | `parseDashboardData`, `buildSelectedChildTaskList`, `showTaskOptionsDialog` | "זה מסך השליטה הראשי של ההורה." |
| `Parents_Dashbord_and_mange/ManageChildrenActivity.java` | ניהול ילדים | `saveChild`, `loadChildren`, `enterEditMode`, `childrenRef` | "ההורה מוסיף, עורך ומוחק ילדים." |
| `Parents_Dashbord_and_mange/ParentTaskTemplateActivity.java` | תבניות משימה | `loadTemplates`, `saveOrUpdateTemplate`, `parseStarsOrNotify` | "תבניות חוסכות זמן ושומרות גם ניקוד." |
| `Parents_Dashbord_and_mange/AssignTaskToChildActivity.java` | שליחת משימה לילד | `loadTemplates`, `loadChildren`, `assignTask`, `tasksRef` | "כאן נוצרת משימה חדשה תחת הילד שנבחר." |
| `Parents_Dashbord_and_mange/GenerateQRActivity.java` | יצירת QR משפחתי | `generateParentQR` | "ההורה מציג QR שמכיל את ה-UID שלו." |
| `Parents_Dashbord_and_mange/model/TaskTemplate.java` | מודל תבנית | `safeStarsWorth` | "התבנית מגדירה שם, תמונה וכוכבים." |
| `child/model/ChildTask.java` | מודל משימה לילד | fields, empty constructor | "Firebase ממיר DataSnapshot לאובייקט הזה." |
| `util/ImageHelper.java` | טיפול בתמונות | `loadCorrectedBitmap`, `bitmapToBase64`, `base64ToBitmap` | "אותה תשתית תמונות משמשת בכל האפליקציה." |
| `util/DateUtils.java` | חישוב דחיפות | `daysLeft`, `isDueSoon` | "הפילטרים משתמשים בחישוב ימים עד תאריך היעד." |
| `util/NameUtils.java` | בניית שם מלא | `fullNameOrDefault` | "זה מונע כפילות כשמציגים שמות." |
