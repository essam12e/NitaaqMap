import type { Metadata } from "next";
import Link from "next/link";
import { ArrowRight, ShieldCheck } from "lucide-react";

export const metadata: Metadata = {
  title: "سياسة الخصوصية | نطاق ماب",
  description: "كيف يتعامل نطاق ماب (الموقع وتطبيق أندرويد) مع بياناتك.",
  alternates: { canonical: "/privacy" },
};

const LAST_UPDATED = "4 نوفمبر 2026";

const sections: { title: string; body: React.ReactNode }[] = [
  {
    title: "باختصار",
    body: (
      <ul className="list-disc space-y-2 pr-5">
        <li>لا نطلب منك إنشاء حساب، ولا نجمع اسمك أو رقمك أو بريدك.</li>
        <li>لا نصل إلى موقع جهازك (GPS)، ولا نطلب أي صلاحية غير الاتصال بالإنترنت.</li>
        <li>لا نخزّن روابط رحلاتك في أي قاعدة بيانات، ولا نبيع أي بيانات ولا نشاركها للإعلان.</li>
        <li>الرابط المؤقت ينتهي تلقائيًا بعد 5 دقائق.</li>
      </ul>
    ),
  },
  {
    title: "ما البيانات التي نعالجها، ولماذا",
    body: (
      <>
        <p>
          عندما تلصق رابط خرائط Google وتطلب إنشاء رمز QR، يُرسَل هذا الرابط إلى خادمنا عبر اتصال مشفّر (HTTPS)،
          فيتحقق الخادم من أنه رابط خرائط Google صالح، ثم يُنشئ رابطًا مؤقتًا موقّعًا رقميًا يحتوي على رابط الرحلة
          ووقت انتهائه. لا يحفظ الخادم الرابط بعد إرسال الرد، فكل المعلومات اللازمة موجودة داخل الرابط المؤقت نفسه.
        </p>
        <p className="mt-3">
          رابط الرحلة قد يدل على وجهة أو مكان تختاره أنت، لذلك شارك رمز QR فقط مع من تريد أن يعرف هذه الوجهة.
          من يمسح الرمز خلال 5 دقائق يُحوَّل إلى خرائط Google، وبعدها يظهر له أن الرابط منتهي الصلاحية.
        </p>
      </>
    ),
  },
  {
    title: "تطبيق أندرويد",
    body: (
      <ul className="list-disc space-y-2 pr-5">
        <li>الصلاحية الوحيدة هي الإنترنت، لإنشاء الرابط المؤقت.</li>
        <li>
          يحفظ التطبيق على جهازك فقط تفضيل المظهر (فاتح أو داكن)، والرحلة النشطة حاليًا ليكمل العدّاد إذا أغلقت
          التطبيق، وتُمسح الرحلة عند انتهائها. لا يُنسخ شيء من ذلك احتياطيًا إلى السحابة ولا يُنقل لجهاز آخر.
        </li>
        <li>حفظ صورة QR يكون في معرض جهازك وبطلب منك، ومشاركتها تكون عبر قائمة المشاركة في أندرويد وبطلب منك.</li>
        <li>لا يحتوي التطبيق على إعلانات، ولا على أدوات تحليلات أو تتبع من أطراف ثالثة.</li>
      </ul>
    ),
  },
  {
    title: "الموقع الإلكتروني",
    body: (
      <ul className="list-disc space-y-2 pr-5">
        <li>
          يستخدم الموقع Vercel Web Analytics لإحصاءات زيارات مجمّعة (مثل عدد الزيارات والصفحات)، وهي لا تستخدم
          ملفات تعريف الارتباط (Cookies) ولا تحدد هويتك.
        </li>
        <li>يحفظ الموقع في متصفحك فقط تفضيل المظهر وحالة التفعيل، ولا تُرسَل إلى خادمنا.</li>
      </ul>
    ),
  },
  {
    title: "الاستضافة والسجلات",
    body: (
      <p>
        الخادم مستضاف لدى Vercel. مثل أي خدمة على الإنترنت، قد يسجّل مزوّد الاستضافة بيانات تقنية مؤقتة لكل طلب
        (مثل عنوان IP ووقت الطلب) لأغراض التشغيل والحماية من إساءة الاستخدام، وتُحذف وفق سياسة الاحتفاظ لدى المزوّد.
        لا نستخدم هذه السجلات لتحديد هويتك.
      </p>
    ),
  },
  {
    title: "الأطفال",
    body: <p>الخدمة غير موجهة للأطفال دون 13 عامًا، ولا نجمع عن قصد أي بيانات منهم.</p>,
  },
  {
    title: "حذف البيانات",
    body: (
      <p>
        لا نحتفظ بأي بيانات مرتبطة بك، فلا يوجد ما يحتاج حذفًا من جهتنا. لحذف ما يحفظه التطبيق على جهازك، احذف
        التطبيق أو امسح بياناته من إعدادات أندرويد.
      </p>
    ),
  },
  {
    title: "التغييرات على هذه السياسة",
    body: <p>إذا غيّرنا هذه السياسة سنحدّث هذه الصفحة وتاريخ آخر تحديث أعلاها.</p>,
  },
  {
    title: "تواصل معنا",
    body: (
      <p>
        لأي سؤال عن الخصوصية: واتساب{" "}
        <a className="font-bold text-emerald-300 underline" href="https://wa.me/966573730430">
          966573730430+
        </a>{" "}
        أو اتصال على{" "}
        <a className="font-bold text-emerald-300 underline" href="tel:0573730430" dir="ltr">
          0573730430
        </a>
        .
      </p>
    ),
  },
];

export default function PrivacyPage() {
  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-10 sm:px-6 sm:py-16">
      <div className="subtle-grid absolute inset-0 opacity-45" />
      <div className="absolute -top-28 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-blue-500/20 blur-3xl" />

      <article className="glass-panel relative z-10 mx-auto w-full max-w-3xl rounded-[2rem] p-6 sm:p-10">
        <div className="mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-emerald-400/10 text-emerald-200 ring-1 ring-emerald-200/20">
          <ShieldCheck className="h-8 w-8" />
        </div>
        <h1 className="text-3xl font-black sm:text-4xl">سياسة الخصوصية</h1>
        <p className="mt-3 text-sm text-slate-400">
          تنطبق على موقع نطاق ماب وتطبيق نطاق ماب على أندرويد. آخر تحديث: {LAST_UPDATED}
        </p>

        <div className="mt-8 space-y-8 text-lg leading-9 text-slate-300">
          {sections.map((s) => (
            <section key={s.title}>
              <h2 className="mb-3 text-xl font-extrabold text-white">{s.title}</h2>
              {s.body}
            </section>
          ))}
        </div>

        <Link
          href="/"
          className="glow-button mt-10 inline-flex items-center justify-center gap-2 rounded-2xl px-7 py-4 text-base font-extrabold text-white transition hover:-translate-y-0.5"
        >
          <ArrowRight className="h-5 w-5" />
          رجوع للرئيسية
        </Link>
      </article>
    </main>
  );
}
