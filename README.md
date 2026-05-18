# نطاق | ماب

موقع Next.js لإنشاء QR مؤقت لرابط رحلة من Google Maps. الكود لا يحتوي على رابط خرائط مباشر، بل يحتوي على رابط داخلي مثل `/trip/[id]` يتحقق من الصلاحية قبل تحويل السائق.

## التشغيل المحلي

```bash
npm install
npm run dev
```

ثم افتح:

```text
http://localhost:3000
```

## متغيرات البيئة

انسخ `.env.example` إلى `.env.local` وضع قيمة سرية طويلة:

```bash
TRIP_TOKEN_SECRET=your-long-random-secret
```

على Vercel، أضف نفس المتغير من إعدادات المشروع. بدون هذا المتغير سيعمل المشروع محليًا بسر افتراضي للتطوير فقط.

## النشر على Vercel

```bash
npm run build
```

ثم اربط المجلد مع Vercel أو ارفعه إلى GitHub ثم استورده من Vercel.

إعدادات Vercel المقترحة:

- Framework Preset: Next.js
- Build Command: `npm run build`
- Install Command: `npm install`
- Output Directory: اتركه فارغًا
- Environment Variable: أضف `TRIP_TOKEN_SECRET` بقيمة سرية طويلة

المشروع يستخدم App Router و API Route لإنشاء معرف الرحلة المؤقت، ولا يحتاج قاعدة بيانات لأن معرف الرحلة موقّع وينتهي بعد 5 دقائق.
