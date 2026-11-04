# نشر نطاق ماب على Google Play

هذا الدليل يغطي ما تحتاج تسويه بنفسك. الكود جاهز: سياسة الخصوصية على `https://nitaaq-map.vercel.app/privacy`، وبناء ملف AAB موقّع يتم من GitHub Actions، ونصوص المتجر والرسومات في `play-store/`.

## 1. حساب Play Console

1. سجّل في https://play.google.com/console (رسوم 25 دولار مرة وحدة) وأكمل التحقق من الهوية.
2. إذا كان الحساب **شخصيًا** وأُنشئ بعد نوفمبر 2023، Google تشترط **اختبارًا مغلقًا مع 12 مختبرًا على الأقل لمدة 14 يومًا متواصلة** قبل أن تسمح بالنشر للعامة. جهّز قائمة إيميلات 12 شخصًا (حسابات Google) من الآن.

## 2. مفتاح الرفع (upload key)

تحتاج تسويه مرة وحدة فقط، **واحتفظ بالملف وكلمة المرور في مكان آمن** (مثل مدير كلمات مرور + نسخة في Google Drive). لا ترفعه إلى GitHub أبدًا، فالمستودع عام.

على أي جهاز فيه Java أو Android Studio:

```bash
keytool -genkeypair -v -keystore nitaaq-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Nitaaq Map"
```

سيطلب منك كلمة مرور. استخدم كلمة مرور من حروف وأرقام فقط (بدون `\` أو `$`) لتجنب مشاكل في GitHub Actions، واستخدم نفس الكلمة للمفتاح والملف.

ثم حوّله إلى نص base64:

```bash
base64 -w0 nitaaq-upload.jks > nitaaq-upload.b64     # Linux
base64 -i nitaaq-upload.jks -o nitaaq-upload.b64     # macOS
```

في Windows (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nitaaq-upload.jks")) | Out-File nitaaq-upload.b64 -Encoding ascii
```

## 3. أسرار GitHub

في المستودع: **Settings ← Secrets and variables ← Actions ← New repository secret**، وأضف أربعة:

| الاسم | القيمة |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | محتوى ملف `nitaaq-upload.b64` كاملًا |
| `ANDROID_KEYSTORE_PASSWORD` | كلمة المرور |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | كلمة المرور |

## 4. بناء ملف AAB

1. في المستودع: **Actions ← Android release (Google Play) ← Run workflow**.
2. اكتب `version_name` (مثل `1.0.0`) واترك `version_code` فارغًا ليُحسب تلقائيًا (كل تشغيل يعطي رقمًا أكبر من اللي قبله).
3. بعد ما يخلص (حوالي 10 دقائق) نزّل الملف المضغوط من أسفل صفحة التشغيل. بداخله:
   - `nitaaq-map-….aab`: هذا اللي ترفعه لجوجل بلاي.
   - `nitaaq-map-….apk`: نفس النسخة، ثبّتها على جوالك وجرّبها قبل الرفع.
   - `mapping.txt`: ارفعه في Play Console (App bundle explorer ← Downloads ← ReTrace mapping file) ليظهر سبب أي انهيار بشكل مقروء.

## 5. إنشاء التطبيق في Play Console

1. **Create app**: الاسم «نطاق ماب»، اللغة الافتراضية العربية، تطبيق، مجاني.
   - تنبيه: بعد ما يكون مجانيًا لا يمكن تحويله لمدفوع. وإذا أردت لاحقًا بيع ميزة داخل تطبيق أندرويد، فيجب أن يكون الدفع عبر Google Play Billing، وليس عبر كود تفعيل يُشترى من واتساب كما في الموقع.
2. **Play App Signing**: اتركه مفعّلًا (الافتراضي). Google تحتفظ بمفتاح التوقيع النهائي، وأنت ترفع بمفتاح الرفع. لو ضاع مفتاح الرفع تقدر تطلب استبداله من الدعم.
3. **صفحة المتجر**: انسخ من `play-store/listing-ar.md`، وارفع `play-store/icon-512.png` و`play-store/feature-graphic.png` ولقطتين على الأقل من جوالك.

## 6. نماذج Policy (App content)

| النموذج | الإجابة |
|---|---|
| Privacy policy | `https://nitaaq-map.vercel.app/privacy` |
| App access | كل الميزات متاحة بدون تسجيل دخول |
| Ads | لا يحتوي على إعلانات |
| Content rating | املأ الاستبيان: أداة (Utility)، لا عنف ولا محتوى للبالغين ولا تفاعل بين المستخدمين ولا مشاركة للموقع الفعلي للجهاز. النتيجة المتوقعة: 3+ |
| Target audience | 18+ (أو 13+). لا تختر فئات الأطفال |
| News app | لا |
| Health / Financial features | لا |
| Government app | لا |

### أمان البيانات (Data safety)

- **هل يجمع التطبيق أو يشارك بيانات المستخدم؟** نعم (لأن رابط الخرائط يُرسل للخادم).
- **هل كل البيانات مشفرة أثناء النقل؟** نعم.
- **هل يمكن للمستخدم طلب حذف البيانات؟** لا توجد بيانات محفوظة لدينا (اختر الخيار المناسب: لا نحتفظ بالبيانات).
- **نوع البيانات**: «App activity ← Other user-generated content» (رابط الخرائط الذي يلصقه المستخدم).
  - Collected: نعم. Shared: لا.
  - Processed ephemerally (تُعالج مؤقتًا ولا تُحفظ): **نعم**.
  - Required أو Optional: مطلوب (بدونه لا يعمل التطبيق).
  - الغرض: App functionality.
- **الموقع (Location)**: لا. التطبيق لا يقرأ موقع الجهاز.
- لا معلومات شخصية، ولا معرّفات أجهزة، ولا تحليلات، ولا سجلات أعطال من أطراف ثالثة.

## 7. الرفع

1. **Testing ← Closed testing** (إذا انطبق شرط الـ 12 مختبرًا): أنشئ مسارًا، أضف إيميلات المختبرين، ارفع ملف `.aab`، وانشر. شارك رابط الانضمام معهم، وتأكد أنهم يثبتون التطبيق ويبقونه 14 يومًا.
2. بعد 14 يومًا: **Apply for production** من لوحة التحكم، ثم ارفع (أو رقِّ) نفس النسخة إلى **Production**.
3. المراجعة تأخذ عادة من يوم إلى 7 أيام للتطبيق الأول.

## تحديثات لاحقة

شغّل نفس الـ workflow برقم إصدار جديد (مثل `1.0.1`) وارفع ملف `.aab` الجديد. الـ versionCode يزيد تلقائيًا.
