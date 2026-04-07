# דוח מיפוי זרימות

תאריך בדיקה: 2026-04-07

היקף:
- מבוסס רק על ה־working tree הנוכחי.
- סטטי בלבד, ללא הרצה.
- ללא שינויי ארכיטקטורה, ללא redesign של חבילות, ללא פיצול קבצים.
- לא שונתה לוגיקת Firebase, QR, session או UI.

הערות רוחב:
- אין שימוש ב־fragment arguments באף אחת מהזרימות שנבדקו. כל ההעברות הן דרך `Intent extras`, `FirebaseAuth`, `Firebase Realtime Database`, או `SharedPreferences`.
- session של הורה נשען רק על `FirebaseAuth`.
- session של ילד נשען על `SharedPreferences` בשם `child_session` עם המפתחות `parentId` ו־`childId`.

## 1. הרשמת הורה

- שרשרת הזרימה: `MainActivity` בלחיצה על `btnRegister` -> `showFragment(new ParentRegisterFragment(), true)` -> `ParentRegisterFragment.registerParent()` -> `FirebaseAuth.createUserWithEmailAndPassword(...)` -> `FBsingleton.setUserData(...)` -> `FBsingleton.saveParentToFirebase(...)` -> `startActivity(ParentDashboardActivity)` -> `finish()` על `MainActivity`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/auth/MainActivity.java`, `app/src/main/java/com/example/family_tasks_proj/Parents/ParentRegisterFragment.java`
- המסך/קומפוננטה הבא: `ParentDashboardActivity`
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- `FirebaseAuth.createUserWithEmailAndPassword(email, password)`
- כתיבה ל־`/parents/{uid}` דרך `FBsingleton.saveParentToFirebase(...)`
- שדות שנכתבים: `uid`, `firstName`, `lastName`, `email`, `role`
- guards קיימים:
- בדיקת `firstName/lastName/email/password` ריקים
- בדיקת פורמט אימייל
- בדיקת אורך סיסמה (`>= 6`)
- בדיקת `isAdded()` בתוך callback
- בדיקת `user != null` אחרי יצירת המשתמש
- סיכון סטטי בולט:
- אם יצירת המשתמש ב־Auth מצליחה אבל הכתיבה ל־`/parents/{uid}` נכשלת, נוצר חשבון Auth בלי פרופיל הורה מלא ב־Realtime Database.
- אם `FBsingleton.uid` יוצא `null`, `saveParentToFirebase(...)` חוזר בלי callback והמסך עלול להישאר ב־loading.
- שלמות מהקוד בלבד: כן, אבל תלויה בכתיבה מוצלחת של פרופיל ההורה ל־DB.

## 2. התחברות הורה

- שרשרת הזרימה: `MainActivity` ברירת מחדל -> `ParentLoginFragment` -> לחיצה על `btnLogin` -> `ParentLoginFragment.loginUser()` -> `FirebaseAuth.signInWithEmailAndPassword(...)` -> `startActivity(ParentDashboardActivity)` -> `finish()` על `MainActivity`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents/ParentLoginFragment.java`
- המסך/קומפוננטה הבא: `ParentDashboardActivity`
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- `FirebaseAuth.signInWithEmailAndPassword(email, password)`
- אין קריאת DB בשלב ההתחברות עצמו.
- guards קיימים:
- בדיקת אימייל/סיסמה ריקים
- בדיקת פורמט אימייל
- `setLoading(true/false)`
- בדיקת `isAdded()` לפני שימוש ב־Fragment context
- סיכון סטטי בולט:
- אין בדיקת קיום פרופיל הורה ב־`/parents/{uid}` לפני פתיחת `ParentDashboardActivity`.
- אין auto-restore של session הורה מה־launcher; `MainActivity` תמיד נטען עם `ParentLoginFragment` כברירת מחדל.
- שלמות מהקוד בלבד: כן.

## 3. פתיחת דשבורד הורה

- שרשרת הזרימה: `ParentLoginFragment` או `ParentRegisterFragment` -> `ParentDashboardActivity.onCreate()` -> `bindViews()` / `bindActions()` / setup של רשימות ופילטרים -> `onResume()` -> `getSignedInParentOrRedirect()` -> `loadParentProfile(user)` + `loadDashboardData(user)`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ParentDashboardActivity.java`
- המסך/קומפוננטה הבא:
- בתוך הדשבורד עצמו נטענים header, child summaries, task list.
- מתוך הדשבורד אפשר לפתוח את זרימות 4-7 דרך `btnManageChildren`, `btnManageTemplates`, `btnAssignTaskToChild`, `btnShowQR`.
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- `FirebaseAuth.getCurrentUser()`
- קריאת פרופיל: `/parents/{uid}`
- קריאת תמונת הורה: `/parents/{uid}/profileImageBase64`
- קריאת כל הילדים: `/parents/{uid}/children`
- קריאת כל המשימות בפועל מתוך `/parents/{uid}/children/{childId}/tasks/{taskId}`
- guards קיימים:
- `getSignedInParentOrRedirect()` בודק `currentUser != null`, אחרת עושה redirect ל־`MainActivity` עם `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
- `parseDashboardData(...)` מדלג על `childId` ריק ועל `taskId` ריק
- `safeText(...)` לשדות טקסט
- guards על `task == null` ועל child selection
- סיכון סטטי בולט:
- `tvParentTotalTasks` נטען מ־`assigned` בלבד, כלומר בפועל זה "פתוחות/משויכות" ולא "סה״כ".
- `loadParentProfile(...)` מתעלם מ־`onCancelled(...)`, ולכן כשל בקריאת הפרופיל שקט.
- שלמות מהקוד בלבד: כן.

## 4. ניהול ילדים

- שרשרת הזרימה: `ParentDashboardActivity` בלחיצה על `btnManageChildren` -> `ManageChildrenActivity` -> `loadChildren()` -> הוספה דרך `addChild()` או עריכה/מחיקה דרך `showChildOptionsDialog(position)`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ManageChildrenActivity.java`
- המסך/קומפוננטה הבא:
- אין מעבר למסך אחר.
- הזרימה נשארת באותו מסך ומרעננת רשימה אחרי add/update/delete.
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- `FirebaseAuth.getCurrentUser()` לקבלת `parentUid`
- reference בסיסי: `/parents/{uid}/children`
- הוספה: `/parents/{uid}/children/{childId}` עם `setValue(new Child(firstName, lastName, profileImageBase64))`
- עריכה: `/parents/{uid}/children/{editingChildId}` עם `updateChildren(...)`
- מחיקה: `/parents/{uid}/children/{childId}` עם `removeValue()`
- שדות בשימוש: `firstName`, `lastName`, `profileImageBase64`
- guards קיימים:
- אם `currentUser == null` מוצג toast והמסך נסגר
- בדיקת `firstName/lastName` ריקים לפני add/update
- guard על `childId == null` אחרי `push().getKey()`
- guards על bounds לפני edit/delete
- reset של הטופס אחרי save/delete
- סיכון סטטי בולט:
- מחיקת ילד מוחקת גם את כל תת־העץ שלו, כולל `/tasks`, כי מתבצעת `removeValue()` על node הילד כולו.
- אין בדיקת duplicate לילדים עם אותו שם.
- שלמות מהקוד בלבד: כן.

## 5. יצירה/עריכה/מחיקה של תבנית משימה

- שרשרת הזרימה: `ParentDashboardActivity` בלחיצה על `btnManageTemplates` -> `ParentTaskTemplateActivity` -> `loadTemplates()` -> יצירה/עדכון דרך `btnSave` ו־`saveOrUpdateTemplate()` -> עריכה/מחיקה דרך לחיצה ארוכה על `lvTemplates` -> `showTemplateOptionsDialog(position)`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ParentTaskTemplateActivity.java`
- המסך/קומפוננטה הבא:
- אין מעבר למסך אחר.
- אחרי save/delete מתבצע `resetForm()` ו־`loadTemplates()` מחדש.
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- קריאה: `/parents/{uid}/task_templates`
- כתיבה/עדכון: `/parents/{uid}/task_templates/{templateId}` עם `updateChildren(...)`
- מחיקה: `/parents/{uid}/task_templates/{templateId}` עם `removeValue()`
- שדות: `id`, `title`, `imageBase64`
- guards קיימים:
- `title` חובה תמיד
- `image` חובה רק ביצירה חדשה (`editingTemplateId == null`)
- guard על `currentUser == null` ב־save/delete
- guard על `template == null` בטעינה
- guard על `imageBase64 == null` אחרי conversion
- guard על bounds בפתיחת dialog אפשרויות
- סיכון סטטי בולט:
- `loadTemplates()` ו־`deleteTemplate()` חוזרים בשקט אם `currentUser == null`, בלי redirect ובלי הודעת שגיאה במקרים מסוימים.
- edit/delete תלויים ב־long press על הרשימה; אין trigger נוסף מהקוד.
- שלמות מהקוד בלבד: כן.

## 6. הקצאת משימה לילד

- שרשרת הזרימה: `ParentDashboardActivity` בלחיצה על `btnAssignTaskToChild` -> `AssignTaskToChildActivity` -> `loadTemplates()` + `loadChildren()` -> בחירת template ב־`spTemplates` מעדכנת `etTitle` ו־preview -> בחירת תאריך דרך `showDatePicker()` -> `showAssignConfirmDialog()` -> `assignTask()` -> `finish()` אחרי save מוצלח.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/AssignTaskToChildActivity.java`
- המסך/קומפוננטה הבא:
- אין מסך ביניים נוסף.
- אחרי save מוצלח הפעילות נסגרת וחוזרת למסך הקודם.
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- `FirebaseAuth.getCurrentUser()` לקבלת `parentUid`
- קריאת templates: `/parents/{uid}/task_templates`
- קריאת ילדים: `/parents/{uid}/children`
- כתיבת משימה: `/parents/{uid}/children/{childId}/tasks/{taskId}`
- שדות שנכתבים: `title`, `dueAt`, `isDone=false`, `starsWorth=10`, `imageBase64`, `createdAt`
- guards קיימים:
- אם `currentUser == null` מוצג toast והמסך נסגר
- `title` חובה
- `dueDate` חובה
- בדיקת index תקין לילד נבחר
- guard על `taskId == null`
- guard על bounds של template selection לפני קריאת `imageBase64`
- סיכון סטטי בולט:
- אין empty-state ייעודי אם אין ילדים או אין templates; ה־spinners פשוט יכולים להישאר ריקים.
- `title` נשמר editable, ולכן אפשר לחרוג מהכותרת המקורית של template בלי שום סימון.
- שלמות מהקוד בלבד: כן, אבל רק אם קיים לפחות ילד אחד לבחירה.

## 7. יצירת QR

- שרשרת הזרימה: `ParentDashboardActivity` בלחיצה על `btnShowQR` -> `GenerateQRActivity.onCreate()` -> `FirebaseAuth.getCurrentUser()` -> `generateParentQR(user.getUid())` -> הצגת bitmap ב־`imageViewQrCode`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/GenerateQRActivity.java`
- המסך/קומפוננטה הבא:
- אין מעבר אוטומטי למסך אחר.
- `btnBackToDashboard` מבצע `finish()`.
- `Intent extras` / `fragment args`: אין.
- תלות Firebase / נתונים:
- אין קריאה ל־Realtime Database.
- payload נבנה רק מ־`FirebaseAuth.getCurrentUser().getUid()`.
- פורמט ה־QR שנוצר: `parent:{parentId}`
- guards קיימים:
- guard על `currentUser == null`
- `try/catch` על `WriterException`
- סיכון סטטי בולט:
- QR נוצר רק בפורמט parent-only, ולכן תמיד נשאר שלב child selection נפרד אחרי הסריקה.
- אין שיתוף/שמירה/ייצוא מהקוד, רק תצוגה על המסך.
- שלמות מהקוד בלבד: כן.

## 8. כניסת ילד דרך QR

- שרשרת הזרימה: `MainActivity` בלחיצה על `btnChildQR` -> `showFragment(new ChildQRLoginFragment(), true)` -> `ChildQRLoginFragment.startQrScan()` -> callback של `ScanContract` -> `parseQr(raw)` -> `checkParentExists(parentId)` או `checkChildExists(parentId, childId)` -> `saveSession(...)` -> `openChildSelection(parentId, childId)` -> `finish()` על `MainActivity`.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Child_Login/ChildQRLoginFragment.java`
- המסך/קומפוננטה הבא: `ChildSelectionActivity`
- `Intent extras` / `fragment args`:
- יוצאים ל־`ChildSelectionActivity` עם `parentId`
- `childId` נשלח רק אם קיים ב־QR
- אין `fragment args`
- תלות Firebase / נתונים:
- בדיקת הורה: `/parents/{parentId}`
- בדיקת ילד: `/parents/{parentId}/children/{childId}`
- שמירת session: `SharedPreferences("child_session")`
- פורמטים נתמכים בפועל לפי parser:
- `parent:{parentId}`
- `parent:{parentId}|child:{childId}`
- parser יודע לזהות גם `childId:{childId}` או raw child id, אבל בלי `parentId` הזרימה נפסלת כ־invalid
- guards קיימים:
- guard על `isAdded()`
- guard על סריקה שבוטלה (`raw == null`)
- guard על `parentId` ריק אחרי parsing
- guard על `snapshot.exists()` גם להורה וגם לילד
- מחיקת `childId` ישן ב־session אם נסרק QR של הורה בלבד
- סיכון סטטי בולט:
- parser תומך תחבירית ב־child-only payload, אבל הזרימה עצמה לא יכולה להשתמש בו כי `parentId` חובה.
- שלמות מהקוד בלבד: כן, עבור payload שמכיל `parentId`.

## 9. בחירת ילד

- שרשרת הזרימה:
- כניסה מ־`ChildQRLoginFragment` עם `parentId` ולעיתים `childId`
- או כניסה מ־`MainActivity` בלחיצה על `btnChild`, עם `parentId` שמועבר מה־session אם קיים
- `ChildSelectionActivity.resolveIds()` -> אם אין `parentId` אז `showParentPicker()` ו־`loadParents()` -> בחירת הורה מפעילה `loadChildren(parentId)` -> `onEnterClicked()` -> `saveSession(parentId, childId)` -> `startActivity(ChildDashboardActivity)` -> `finish()`
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Child_Login/ChildSelectionActivity.java`, וגם `app/src/main/java/com/example/family_tasks_proj/auth/MainActivity.java` עבור direct child entry
- המסך/קומפוננטה הבא: `ChildDashboardActivity`
- `Intent extras` / `fragment args`:
- נקלטים: `parentId`, `childId` אופציונלי
- נשלחים הלאה: `parentId`, `childId`
- אין `fragment args`
- תלות Firebase / נתונים:
- טעינת כל ההורים: `/parents`
- טעינת ילדי הורה נבחר: `/parents/{parentId}/children`
- שמירת session: `SharedPreferences("child_session")`
- guards קיימים:
- אם `parentId` חסר בעת Enter, מוצג toast
- בדיקת index תקין ל־child spinner
- empty-state אם אין הורים
- empty-state אם אין ילדים
- `preselectedChildId` נבחר אם הגיע ב־intent ונמצא ברשימה
- סיכון סטטי בולט:
- אם `parentId` ישן/לא תקף מגיע מה־session או מה־intent, המסך מדלג על parent picker ונשאר רק עם `loadChildren(parentId)` ו־"אין ילדים", בלי fallback לבחור הורה אחר.
- בכניסה מ־`MainActivity` עם `parentId` מה־session, `resolveIds()` לא טוען `childId` מה־prefs כי הוא יוצא מוקדם, ולכן אין preselect אוטומטי של הילד האחרון.
- שלמות מהקוד בלבד: כן, אבל session restore כאן חלקי.

## 10. דשבורד ילד

- שרשרת הזרימה: `ChildSelectionActivity` -> `startActivity(ChildDashboardActivity)` עם `parentId` ו־`childId` -> `ChildDashboardActivity.onCreate()` -> `resolveSession()` -> `loadChildHeader()` + `loadTasks()` -> פילטרים (`NOT_COMPLETED` / `COMPLETED` / `URGENT`) -> `markTaskDone(task)` מעדכן `isDone=true` ואז `loadTasks()` מחדש.
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/child/ChildDashboardActivity.java`
- המסך/קומפוננטה הבא:
- אין מעבר אוטומטי למסך אחר.
- logout מחזיר ל־`MainActivity`.
- `Intent extras` / `fragment args`:
- `parentId`, `childId`
- אם extras חסרים, יש fallback ל־`SharedPreferences("child_session")`
- אין `fragment args`
- תלות Firebase / נתונים:
- header ילד: `/parents/{parentId}/children/{childId}`
- משימות: `/parents/{parentId}/children/{childId}/tasks`
- סימון משימה כהושלמה: `/parents/{parentId}/children/{childId}/tasks/{taskId}/isDone`
- שדות משימה בשימוש: `id`, `title`, `dueAt`, `isDone`, `starsWorth`, `imageBase64`, `createdAt`
- guards קיימים:
- אם `parentId` או `childId` חסרים אחרי `resolveSession()`, יש toast ואז `finish()`
- guard על `task == null` או `task.id` חסר לפני `markTaskDone(...)`
- guards על `base64` / bitmap null
- guards בתוך filtering logic
- סיכון סטטי בולט:
- אם session חסר, הפעילות רק נסגרת ולא מבצעת redirect מפורש למסך הכניסה.
- `tvTotalTasks` מוצג לפי `openCount` בלבד, לא לפי כל המשימות.
- שלמות מהקוד בלבד: כן.

## 11. logout / session restore

- שרשרת logout הורה: `ParentDashboardActivity.showLogoutDialog()` -> `FirebaseAuth.getInstance().signOut()` -> `startActivity(MainActivity)` עם `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` -> `finish()`
- שרשרת logout ילד: `ChildDashboardActivity.showLogoutDialog()` -> `SharedPreferences("child_session").edit().clear().apply()` -> `startActivity(MainActivity)` עם אותם flags -> `finish()`
- שרשרת session restore של ילד:
- `ChildQRLoginFragment.saveSession(parentId, childId)` שומר/מעדכן `child_session`
- `MainActivity` בלחיצה על `btnChild` קורא רק `parentId` מה־prefs ומעביר אותו ל־`ChildSelectionActivity`
- `ChildSelectionActivity.resolveIds()` טוען `parentId/childId` מה־prefs רק אם `parentId` לא הגיע ב־intent
- `ChildDashboardActivity.resolveSession()` משלים חסרים מ־prefs אם לא הגיעו extras
- נקודת כניסה: `app/src/main/java/com/example/family_tasks_proj/Parents_Dashbord_and_mange/ParentDashboardActivity.java`, `app/src/main/java/com/example/family_tasks_proj/child/ChildDashboardActivity.java`, `app/src/main/java/com/example/family_tasks_proj/auth/MainActivity.java`, `app/src/main/java/com/example/family_tasks_proj/Child_Login/ChildQRLoginFragment.java`, `app/src/main/java/com/example/family_tasks_proj/Child_Login/ChildSelectionActivity.java`
- המסך/קומפוננטה הבא:
- logout הורה -> `MainActivity`
- logout ילד -> `MainActivity`
- restore ילד דרך launcher -> `ChildSelectionActivity`, לא `ChildDashboardActivity`
- `Intent extras` / `fragment args`:
- keys בשימוש: `parentId`, `childId`
- אחסון: `SharedPreferences("child_session")`
- אין `fragment args`
- תלות Firebase / נתונים:
- logout הורה תלוי רק ב־`FirebaseAuth`
- restore של ילד עצמו תלוי ב־prefs; Firebase נצרך אחר כך במסכי הבחירה/דשבורד
- guards קיימים:
- logout הורה עטוף בדיאלוג אישור
- logout ילד עטוף בדיאלוג אישור
- בכל נקודות restore יש בדיקות blank/null לפני שימוש במזהים
- סיכון סטטי בולט:
- אין auto-restore של session הורה מה־launcher.
- restore של ילד הוא חלקי: מה־launcher נשמר רק shortcut ל־`ChildSelectionActivity` עם `parentId`, ולא כניסה אוטומטית לדשבורד או preselect בטוח של הילד האחרון.
- session של ילד עלול להפוך stale אם `parentId` או `childId` נמחקו מה־DB.
- שלמות מהקוד בלבד: חלקי.

## סיכוני רוחב שכדאי להשאיר מול העיניים של Claude

- parent session restore לא ממומש ב־launcher; רק child session restore ממומש, וגם הוא חלקי.
- child flow תלוי ב־`SharedPreferences("child_session")`, אבל אין normalization או validation גלובלי של מזהים stale לפני דילוגים במסכים.
- parent register יכול ליצור פער בין `FirebaseAuth` לבין `/parents/{uid}` אם כתיבת הפרופיל נכשלת.
- בשני הדשבורדים (`ParentDashboardActivity`, `ChildDashboardActivity`) יש חשד ל־metric label mismatch עבור שדה שמוצג כ"סה״כ" אבל נטען בפועל מ־open/assigned count.
