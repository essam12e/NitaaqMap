"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import QRCode from "qrcode";
import { motion } from "framer-motion";
import {
  AlertCircle,
  ArrowDown,
  CheckCircle2,
  Clock3,
  Copy,
  Download,
  ExternalLink,
  Link2,
  LockKeyhole,
  MessageCircle,
  Plus,
  QrCode,
  ShieldCheck,
  Sparkles,
  TimerReset,
  Zap,
} from "lucide-react";

const whatsappUrl = "https://wa.me/966573730430";

type TripResult = {
  tripUrl: string;
  expiresAt: string;
  qrDataUrl: string;
};

type ApiResponse =
  | {
      tripUrl: string;
      expiresAt: string;
    }
  | {
      error: string;
    };

const sectionMotion = {
  initial: { opacity: 0, y: 28 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: "-100px" },
  transition: { duration: 0.7, ease: [0.22, 1, 0.36, 1] },
} as const;

const features = [
  {
    icon: LockKeyhole,
    title: "خصوصية أعلى",
    text: "لا تحتاج لتسليم جوالك للسائق.",
  },
  {
    icon: Zap,
    title: "سهولة وسرعة",
    text: "انسخ الرابط، أنشئ الكود، وخلاص.",
  },
  {
    icon: Clock3,
    title: "صلاحية مؤقتة",
    text: "الكود ينتهي خلال 5 دقائق لحماية بيانات رحلتك.",
  },
];

const steps = [
  "افتح Google Maps وحدد الرحلة.",
  "انسخ رابط الاتجاهات أو الرحلة.",
  "الصق الرابط هنا واضغط إنشاء.",
  "دع السائق يمسح QR ويفتح المسار.",
];

function formatRemaining(ms: number) {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");

  return `${minutes}:${seconds}`;
}

function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className="inline-flex min-w-0 items-center gap-2.5 sm:gap-3">
      <div className="relative h-11 w-11 shrink-0 rounded-2xl border border-emerald-300/15 shadow-[0_0_38px_rgba(34,197,94,0.2)] sm:h-12 sm:w-12">
        <Image
          src="/nitaaq-official-logo-transparent.png"
          alt="شعار نطاق الرسمي"
          width={96}
          height={96}
          className="h-full w-full object-contain p-0.5"
        />
      </div>
      <div className="min-w-0 leading-none">
        <div className="whitespace-nowrap text-base font-black text-white sm:text-xl">
          نطاق | ماب
        </div>
        {!compact && (
          <div className="mt-1 text-xs font-bold uppercase text-slate-400">
            Nitaaq | Map
          </div>
        )}
      </div>
    </div>
  );
}

function PrimaryLink({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) {
  return (
    <a
      href={href}
      className="glow-button inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-3 text-sm font-extrabold text-white transition duration-300 hover:-translate-y-0.5 hover:shadow-[0_20px_54px_rgba(34,197,94,0.28)]"
    >
      {children}
    </a>
  );
}

export function LandingPage() {
  const [url, setUrl] = useState("");
  const [result, setResult] = useState<TripResult | null>(null);
  const [remainingMs, setRemainingMs] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const qrSectionRef = useRef<HTMLElement | null>(null);

  const isExpired = result ? remainingMs <= 0 : false;

  const remainingLabel = useMemo(() => formatRemaining(remainingMs), [remainingMs]);

  useEffect(() => {
    if (!result) {
      return;
    }

    const tick = () => {
      setRemainingMs(new Date(result.expiresAt).getTime() - Date.now());
    };

    tick();
    const interval = window.setInterval(tick, 1000);

    return () => window.clearInterval(interval);
  }, [result]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setCopied(false);

    if (!url.trim()) {
      setError("فضلا الصق رابط الرحلة أولا.");
      return;
    }

    setIsLoading(true);

    try {
      const response = await fetch("/api/trips", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, siteOrigin: window.location.origin }),
      });

      const data = (await response.json()) as ApiResponse;

      if (!response.ok || "error" in data) {
        setError("error" in data ? data.error : "تعذر إنشاء كود الرحلة.");
        return;
      }

      const qrDataUrl = await QRCode.toDataURL(data.tripUrl, {
        errorCorrectionLevel: "M",
        margin: 2,
        width: 920,
        color: {
          dark: "#071221",
          light: "#F8FAFC",
        },
      });

      setResult({
        tripUrl: data.tripUrl,
        expiresAt: data.expiresAt,
        qrDataUrl,
      });
    } catch {
      setError("حدث خطأ مؤقت. حاول مرة أخرى.");
    } finally {
      setIsLoading(false);
    }
  }

  async function copyTripLink() {
    if (!result || isExpired) {
      return;
    }

    await navigator.clipboard.writeText(result.tripUrl);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  function downloadQr() {
    if (!result || isExpired) {
      return;
    }

    const anchor = document.createElement("a");
    anchor.href = result.qrDataUrl;
    anchor.download = "nitaaq-map-trip-qr.png";
    anchor.target = "_blank";
    anchor.rel = "noopener noreferrer";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  }

  function resetTrip() {
    setResult(null);
    setRemainingMs(0);
    setUrl("");
    setError("");
    setCopied(false);
  }

  return (
    <main className="relative min-h-screen overflow-x-hidden pb-[env(safe-area-inset-bottom)]">
      <div className="subtle-grid pointer-events-none fixed inset-0 opacity-35" />
      <div className="pointer-events-none fixed -top-44 right-[-10rem] h-[34rem] w-[34rem] rounded-full bg-blue-600/20 blur-3xl" />
      <div className="pointer-events-none fixed bottom-[-14rem] left-[-10rem] h-[36rem] w-[36rem] rounded-full bg-emerald-400/16 blur-3xl" />

      <header className="sticky top-0 z-50 border-b border-white/10 bg-[#0b1a2b]/72 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-3 py-3 sm:px-6 sm:py-4 lg:px-8">
          <Brand />
          <nav className="flex items-center gap-2">
            <a
              href="tel:0573730430"
              className="hidden rounded-2xl border border-white/10 px-4 py-3 text-sm font-bold text-slate-200 transition hover:border-emerald-300/40 hover:text-white md:inline-flex"
            >
              تواصل معنا
            </a>
            <PrimaryLink href={whatsappUrl}>
              <MessageCircle className="h-5 w-5" />
              <span className="hidden min-[390px]:inline">واتساب</span>
            </PrimaryLink>
          </nav>
        </div>
      </header>

      <section className="relative z-10 mx-auto grid min-h-[calc(100svh-4.5rem)] max-w-7xl items-center gap-8 px-4 py-10 text-center sm:px-6 sm:py-14 lg:grid-cols-[1.05fr_0.95fr] lg:px-8 lg:py-20 lg:text-right">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
          className="mx-auto max-w-3xl lg:mx-0"
        >
          <div className="mb-5 inline-flex max-w-full items-center gap-2 rounded-full border border-emerald-300/20 bg-emerald-300/8 px-4 py-2 text-sm font-bold text-emerald-200 sm:mb-7">
            <Sparkles className="h-4 w-4" />
            QR مؤقت لرحلات Google Maps
          </div>

          <h1 className="text-[clamp(2.45rem,10vw,4.5rem)] font-black leading-tight text-white lg:text-7xl">
            شارك رحلتك بدون تسليم جوالك
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg leading-9 text-slate-300 sm:mt-6 sm:text-2xl sm:leading-10 lg:mx-0">
            انسخ رابط الرحلة من خرائط Google، وأنشئ QR Code خلال ثواني ليقرأه
            السائق بأمان.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-4 sm:flex-row sm:items-center lg:justify-start">
            <button
              type="button"
              onClick={() =>
                qrSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })
              }
              className="glow-button inline-flex min-h-14 items-center justify-center gap-3 rounded-3xl px-7 py-4 text-lg font-black text-white transition duration-300 hover:-translate-y-1 hover:shadow-[0_28px_70px_rgba(34,197,94,0.28)]"
            >
              <QrCode className="h-6 w-6" />
              إنشاء كود الرحلة
            </button>
            <span className="text-center text-base font-bold text-slate-300 sm:text-right">
              توصل بالسلامة 🤍
            </span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ duration: 0.85, delay: 0.12, ease: [0.22, 1, 0.36, 1] }}
          className="relative mx-auto w-full max-w-xl lg:max-w-none"
        >
          <div className="absolute -inset-8 rounded-[3rem] bg-gradient-to-br from-blue-500/15 via-emerald-400/10 to-transparent blur-2xl" />
          <div className="glass-panel relative rounded-[2rem] p-4 sm:p-7">
            <motion.div
              animate={{ y: [0, -8, 0], scale: [1, 1.015, 1] }}
              transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
              className="rounded-[1.6rem] border border-emerald-300/10 bg-white/[0.015] p-5 shadow-[0_0_70px_rgba(34,197,94,0.12)]"
            >
              <Image
                src="/nitaaq-official-logo-transparent.png"
                alt="شعار نطاق الرسمي"
                width={720}
                height={720}
                priority
                className="mx-auto aspect-[16/10] w-full object-contain object-center drop-shadow-[0_24px_44px_rgba(34,197,94,0.18)]"
              />
            </motion.div>
            <div className="mt-6 grid grid-cols-3 gap-3">
              {["آمن", "سريع", "مؤقت"].map((item) => (
                <div
                  key={item}
                  className="rounded-2xl border border-white/10 bg-white/[0.04] px-3 py-4 text-center text-sm font-black text-slate-100"
                >
                  {item}
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        <ArrowDown className="absolute bottom-8 left-1/2 hidden h-7 w-7 -translate-x-1/2 animate-bounce text-emerald-300/70 lg:block" />
      </section>

      <motion.section
        ref={qrSectionRef}
        id="qr"
        className="relative z-10 mx-auto max-w-6xl scroll-mt-24 px-4 py-10 sm:px-6 sm:py-12 lg:px-8"
        {...sectionMotion}
      >
        <div className="glass-panel rounded-[2rem] p-4 sm:p-8 lg:p-10">
          <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="mb-2 text-sm font-extrabold text-emerald-300">
                إنشاء QR
              </p>
              <h2 className="text-3xl font-black text-white sm:text-4xl">
                الصق رابط الرحلة وأنشئ الكود
              </h2>
            </div>
            <div className="inline-flex w-fit items-center gap-2 rounded-full border border-amber-300/20 bg-amber-300/8 px-4 py-2 text-sm font-bold text-amber-100">
              <ShieldCheck className="h-4 w-4" />
              صالح لمدة 5 دقائق فقط
            </div>
          </div>

          <form onSubmit={handleSubmit} className="grid gap-4 xl:grid-cols-[1fr_auto]">
            <label className="relative block">
              <Link2 className="absolute right-5 top-1/2 h-5 w-5 -translate-y-1/2 text-emerald-300" />
              <input
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                placeholder="الصق رابط الرحلة هنا من Google Maps"
                dir="ltr"
                className="h-16 w-full min-w-0 rounded-3xl border border-white/10 bg-[#071221]/70 px-5 pr-14 text-left text-base text-white outline-none transition placeholder:text-right placeholder:text-slate-500 focus:border-emerald-300/60 focus:ring-4 focus:ring-emerald-300/10"
              />
            </label>
            <button
              type="submit"
              disabled={isLoading}
              className="glow-button inline-flex h-16 items-center justify-center gap-3 rounded-3xl px-8 text-lg font-black text-white transition duration-300 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoading ? (
                <span className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
              ) : (
                <QrCode className="h-6 w-6" />
              )}
              إنشاء QR Code
            </button>
          </form>

          {error && (
            <div className="mt-4 flex items-center gap-3 rounded-2xl border border-red-400/20 bg-red-400/10 px-4 py-3 text-sm font-bold text-red-100">
              <AlertCircle className="h-5 w-5 shrink-0" />
              {error}
            </div>
          )}

          {result && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45 }}
              className="mt-8 grid items-start gap-6 xl:grid-cols-[minmax(280px,0.85fr)_minmax(0,1.15fr)]"
            >
              <div className="relative mx-auto w-full max-w-md overflow-hidden rounded-[2rem] border border-white/10 bg-white p-4 shadow-[0_28px_80px_rgba(0,0,0,0.25)] sm:p-5 xl:max-w-none">
                {isExpired && (
                  <div className="absolute inset-0 z-10 grid place-items-center bg-[#071221]/88 p-6 text-center backdrop-blur-sm">
                    <div>
                      <AlertCircle className="mx-auto mb-4 h-10 w-10 text-amber-200" />
                      <p className="text-xl font-black text-white">
                        انتهت صلاحية كود الرحلة
                      </p>
                    </div>
                  </div>
                )}
                {/* The QR image is generated client-side as a data URL. */}
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={result.qrDataUrl}
                  alt="QR Code لرابط الرحلة الداخلي"
                  className="mx-auto aspect-square w-full max-w-sm rounded-3xl"
                />
              </div>

              <div className="min-w-0 rounded-[2rem] border border-white/10 bg-white/[0.04] p-4 sm:p-7">
                <div className="mb-5 rounded-2xl border border-amber-300/20 bg-amber-300/8 p-4 text-amber-50">
                  <div className="flex items-start gap-3">
                    <TimerReset className="mt-1 h-5 w-5 shrink-0 text-amber-200" />
                    <div>
                      <p className="font-black">
                        تنبيه: كود الرحلة صالح لمدة 5 دقائق فقط حفاظًا على خصوصيتك.
                      </p>
                      <p className="mt-2 text-2xl font-black text-white">
                        ينتهي خلال: {remainingLabel}
                      </p>
                    </div>
                  </div>
                </div>

                {isExpired ? (
                  <div className="rounded-2xl border border-white/10 bg-[#071221]/70 p-5">
                    <p className="text-xl font-black text-white">
                      انتهت صلاحية كود الرحلة. أنشئ كود جديد عند الحاجة.
                    </p>
                  </div>
                ) : (
                  <div className="rounded-2xl border border-emerald-300/20 bg-emerald-300/8 p-5">
                    <p className="flex items-center gap-2 text-lg font-black text-emerald-100">
                      <CheckCircle2 className="h-5 w-5" />
                      الكود جاهز، خل السائق يمسحه وبيفتح Google Maps مباشرة.
                    </p>
                    <p className="mt-2 leading-8 text-slate-300">
                      رحلة موفقة، وخصوصيتك محفوظة.
                    </p>
                  </div>
                )}

                <div className="mt-6 grid gap-3 md:grid-cols-3">
                  <button
                    type="button"
                    onClick={downloadQr}
                    disabled={isExpired}
                    className="glow-button inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl px-4 py-3 text-center font-extrabold text-white transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-45"
                  >
                    <Download className="h-5 w-5" />
                    تحميل QR كصورة
                  </button>
                  <button
                    type="button"
                    onClick={copyTripLink}
                    disabled={isExpired}
                    className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/[0.06] px-4 py-3 text-center font-extrabold text-white transition hover:border-emerald-300/40 hover:bg-white/[0.1] disabled:cursor-not-allowed disabled:opacity-45"
                  >
                    <Copy className="h-5 w-5" />
                    {copied ? "تم النسخ" : "نسخ رابط الرحلة"}
                  </button>
                  <button
                    type="button"
                    onClick={resetTrip}
                    className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 text-center font-extrabold text-[#0b1a2b] transition hover:-translate-y-0.5"
                  >
                    <Plus className="h-5 w-5" />
                    إنشاء رحلة جديدة
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </div>
      </motion.section>

      <motion.section
        className="relative z-10 mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-14 lg:px-8"
        {...sectionMotion}
      >
        <div className="grid gap-4 md:grid-cols-3">
          {features.map((feature) => (
            <article
              key={feature.title}
              className="group rounded-[1.6rem] border border-white/10 bg-white/[0.045] p-6 transition duration-300 hover:-translate-y-1 hover:border-emerald-300/35 hover:bg-white/[0.07]"
            >
              <div className="mb-5 grid h-13 w-13 place-items-center rounded-2xl bg-emerald-300/10 text-emerald-200 ring-1 ring-emerald-300/20 transition group-hover:scale-105">
                <feature.icon className="h-7 w-7" />
              </div>
              <h3 className="text-2xl font-black text-white">{feature.title}</h3>
              <p className="mt-3 text-lg leading-8 text-slate-300">{feature.text}</p>
            </article>
          ))}
        </div>
      </motion.section>

      <motion.section
        className="relative z-10 mx-auto max-w-6xl px-4 py-12 sm:px-6 sm:py-14 lg:px-8"
        {...sectionMotion}
      >
        <div className="mb-9 text-center">
          <p className="mb-2 text-sm font-extrabold text-emerald-300">الخطوات</p>
          <h2 className="text-3xl font-black text-white sm:text-4xl">
            كيف تستخدم نطاق | ماب؟
          </h2>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          {steps.map((step, index) => (
            <article
              key={step}
              className="rounded-[1.5rem] border border-white/10 bg-[#112235]/55 p-5 transition duration-300 hover:-translate-y-1 hover:border-blue-300/35"
            >
              <div className="mb-5 inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 to-emerald-400 text-lg font-black text-white">
                {index + 1}
              </div>
              <p className="text-lg font-bold leading-8 text-slate-100">{step}</p>
            </article>
          ))}
        </div>
      </motion.section>

      <footer className="relative z-10 mt-10 border-t border-white/10">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-10 pb-28 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div>
            <Brand compact />
            <p className="mt-4 text-lg font-bold text-slate-300">
              رحلة آمنة، وخصوصيتك محفوظة.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <a
              href="tel:0573730430"
              className="inline-flex items-center justify-center rounded-2xl border border-white/10 px-5 py-3 font-extrabold text-slate-100 transition hover:border-emerald-300/40"
            >
              0573730430
            </a>
            <PrimaryLink href={whatsappUrl}>
              <MessageCircle className="h-5 w-5" />
              واتساب
              <ExternalLink className="h-4 w-4" />
            </PrimaryLink>
          </div>
        </div>
      </footer>

      <a
        href={whatsappUrl}
        aria-label="تواصل واتساب"
        className="glow-button fixed bottom-[calc(1.25rem+env(safe-area-inset-bottom))] left-4 z-[60] grid h-14 w-14 place-items-center rounded-full text-white shadow-[0_20px_70px_rgba(34,197,94,0.28)] transition hover:-translate-y-1 sm:bottom-7 sm:left-auto sm:right-7 sm:inline-flex sm:w-auto sm:gap-2 sm:px-5"
      >
        <MessageCircle className="h-6 w-6 sm:h-5 sm:w-5" />
        <span className="hidden sm:inline">تواصل واتساب</span>
      </a>
    </main>
  );
}
