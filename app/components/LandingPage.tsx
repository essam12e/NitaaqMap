"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import QRCode from "qrcode";
import { motion } from "framer-motion";
import {
  AlertCircle,
  ArrowDown,
  Camera,
  CheckCircle2,
  Clock3,
  Copy,
  CreditCard,
  Download,
  ExternalLink,
  HelpCircle,
  KeyRound,
  Link2,
  LockKeyhole,
  Map,
  MapPin,
  MessageCircle,
  Moon,
  MousePointerClick,
  Navigation,
  Plus,
  QrCode,
  Route,
  ShieldCheck,
  Sparkles,
  Sun,
  TimerReset,
  Zap,
} from "lucide-react";
import { isGoogleMapsUrl, normalizeGoogleMapsUrl } from "@/lib/maps-url";

const whatsappUrl = "https://wa.me/966573730430";
const activationWhatsappUrl =
  "https://wa.me/966573730430?text=%D9%85%D8%B1%D8%AD%D8%A8%D8%A7%D9%8B%D8%8C%20%D8%A3%D8%B1%D9%8A%D8%AF%20%D8%B7%D9%84%D8%A8%20%D9%83%D9%88%D8%AF%20%D8%AA%D9%81%D8%B9%D9%8A%D9%84%20%D9%86%D8%B7%D8%A7%D9%82%20%D9%85%D8%A7%D8%A8";
const activationCode = "NT-7KQ4-M9X2-ZP8A";
const qrTtlMs = 5 * 60 * 1000;

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

function roundedRect(
  context: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number,
) {
  context.beginPath();
  context.moveTo(x + radius, y);
  context.arcTo(x + width, y, x + width, y + height, radius);
  context.arcTo(x + width, y + height, x, y + height, radius);
  context.arcTo(x, y + height, x, y, radius);
  context.arcTo(x, y, x + width, y, radius);
  context.closePath();
}

function loadImageElement(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new window.Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = src;
  });
}

async function createBrandedQrDataUrl(value: string) {
  const baseQr = await QRCode.toDataURL(value, {
    errorCorrectionLevel: "H",
    margin: 2,
    width: 920,
    color: {
      dark: "#071221",
      light: "#F8FAFC",
    },
  });

  const [qrImage, logoImage] = await Promise.all([
    loadImageElement(baseQr),
    loadImageElement("/nitaaq-official-logo-transparent.png"),
  ]);

  const canvas = document.createElement("canvas");
  canvas.width = 920;
  canvas.height = 920;

  const context = canvas.getContext("2d");

  if (!context) {
    return baseQr;
  }

  context.drawImage(qrImage, 0, 0, canvas.width, canvas.height);

  // QR logo enhancement: a small padded mark keeps the code scannable.
  const badgeSize = 168;
  const logoSize = 126;
  const badgeX = (canvas.width - badgeSize) / 2;
  const badgeY = (canvas.height - badgeSize) / 2;
  const logoX = (canvas.width - logoSize) / 2;
  const logoY = (canvas.height - logoSize) / 2;

  roundedRect(context, badgeX, badgeY, badgeSize, badgeSize, 36);
  context.fillStyle = "#F8FAFC";
  context.fill();
  context.strokeStyle = "rgba(34, 197, 94, 0.35)";
  context.lineWidth = 8;
  context.stroke();
  context.drawImage(logoImage, logoX, logoY, logoSize, logoSize);

  return canvas.toDataURL("image/png");
}

function getCountdownTone(remainingMs: number) {
  const seconds = Math.ceil(remainingMs / 1000);

  if (seconds <= 0) {
    return {
      label: "منتهي",
      text: "text-red-100",
      border: "border-red-300/30",
      bg: "bg-red-400/12",
      bar: "bg-red-400",
      message: "انتهت صلاحية الكود حفاظاً على خصوصيتك",
    };
  }

  if (seconds < 60) {
    return {
      label: "ينتهي قريباً",
      text: "text-red-100",
      border: "border-red-300/30",
      bg: "bg-red-400/12",
      bar: "bg-red-400",
      message: "أقل من دقيقة، أنشئ كود جديد إذا احتجت.",
    };
  }

  if (seconds < 180) {
    return {
      label: "وقت متوسط",
      text: "text-orange-100",
      border: "border-orange-300/30",
      bg: "bg-orange-400/12",
      bar: "bg-orange-400",
      message: "الكود صالح الآن، والوقت يمشي.",
    };
  }

  return {
    label: "صالح",
    text: "text-emerald-100",
    border: "border-emerald-300/30",
    bg: "bg-emerald-400/10",
    bar: "bg-emerald-400",
    message: "الكود نشط وجاهز للمسح.",
  };
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
  const [isLocating, setIsLocating] = useState(false);
  const [isEmergencyLoading, setIsEmergencyLoading] = useState(false);
  const [error, setError] = useState("");
  const [infoMessage, setInfoMessage] = useState("");
  const [copied, setCopied] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [theme, setTheme] = useState<"dark" | "light">("dark");
  const [activationChecked, setActivationChecked] = useState(false);
  const [isActivated, setIsActivated] = useState(false);
  const [showActivation, setShowActivation] = useState(false);
  const [activationInput, setActivationInput] = useState("");
  const [activationError, setActivationError] = useState("");
  const [activationSuccess, setActivationSuccess] = useState("");
  const qrSectionRef = useRef<HTMLElement | null>(null);
  const activationRef = useRef<HTMLElement | null>(null);

  const isExpired = result ? remainingMs <= 0 : false;

  const remainingLabel = useMemo(() => formatRemaining(remainingMs), [remainingMs]);
  const linkValidation = useMemo(() => {
    if (!url.trim()) {
      return "idle";
    }

    return isGoogleMapsUrl(url) ? "valid" : "invalid";
  }, [url]);
  const countdownTone = useMemo(() => getCountdownTone(remainingMs), [remainingMs]);
  const countdownProgress = result
    ? Math.max(0, Math.min(100, (remainingMs / qrTtlMs) * 100))
    : 0;

  useEffect(() => {
    window.setTimeout(() => {
      const savedActivation = window.localStorage.getItem("nitaaq-map-activated");
      setIsActivated(savedActivation === "true");
      setActivationChecked(true);

      const savedTheme = window.localStorage.getItem("nitaaq-map-theme") as
        | "dark"
        | "light"
        | null;
      const prefersLight = window.matchMedia("(prefers-color-scheme: light)").matches;
      setTheme(savedTheme ?? (prefersLight ? "light" : "dark"));
    }, 0);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    window.localStorage.setItem("nitaaq-map-theme", theme);
  }, [theme]);

  useEffect(() => {
    if ("serviceWorker" in navigator) {
      // PWA registration: intentionally small and non-blocking for Vercel.
      navigator.serviceWorker.register("/sw.js").catch(() => undefined);
    }
  }, []);

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

  async function createTrip(nextUrl: string) {
    setError("");
    setInfoMessage("");
    setCopied(false);

    if (!nextUrl.trim()) {
      setError("فضلا الصق رابط الرحلة أولا.");
      return;
    }

    if (!isGoogleMapsUrl(nextUrl)) {
      setError("الرابط غير صحيح، الصق رابط خرائط جوجل أو إحداثيات صحيحة.");
      return;
    }

    setIsLoading(true);

    try {
      const normalizedUrl = normalizeGoogleMapsUrl(nextUrl);
      const response = await fetch("/api/trips", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: normalizedUrl, siteOrigin: window.location.origin }),
      });

      const data = (await response.json()) as ApiResponse;

      if (!response.ok || "error" in data) {
        setError("error" in data ? data.error : "تعذر إنشاء كود الرحلة.");
        return;
      }

      const qrDataUrl = await createBrandedQrDataUrl(data.tripUrl);

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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await createTrip(url);
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
    setInfoMessage("");
    setCopied(false);
  }

  async function generateTripFromUrl(nextUrl: string) {
    setUrl(nextUrl);
    await createTrip(nextUrl);
  }

  function getCurrentPosition() {
    return new Promise<GeolocationPosition>((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("geolocation-unavailable"));
        return;
      }

      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 12000,
        maximumAge: 30000,
      });
    });
  }

  async function createQrForCurrentLocation() {
    setError("");
    setInfoMessage("");
    setIsLocating(true);

    try {
      const position = await getCurrentPosition();
      const { latitude, longitude } = position.coords;
      const mapsUrl = coordinateToMapsUrl(latitude, longitude);
      await generateTripFromUrl(mapsUrl);
      setInfoMessage("تم تحديد موقعك الحالي، جاري إنشاء QR للموقع.");
    } catch {
      setError("لم نتمكن من الوصول لموقعك، يمكنك لصق رابط الخريطة يدوياً");
    } finally {
      setIsLocating(false);
    }
  }

  async function shareEmergencyLocation() {
    setError("");
    setInfoMessage("");
    setIsEmergencyLoading(true);

    try {
      const position = await getCurrentPosition();
      const { latitude, longitude } = position.coords;
      const mapsUrl = coordinateToMapsUrl(latitude, longitude);
      const message = `أنا في رحلة حالياً، هذا هو موقعي: ${mapsUrl}\n\nويمكنك أيضاً استخدام نطاق ماب لمشاركة الرحلات بأمان:\n${window.location.origin}`;
      window.open(`https://wa.me/?text=${encodeURIComponent(message)}`, "_blank", "noopener,noreferrer");
    } catch {
      const message = `يمكنك استخدام نطاق ماب لمشاركة الرحلات بأمان:\n${window.location.origin}`;
      setError("لم يتم السماح بالموقع، فتحنا واتساب برسالة تحتوي على رابط نطاق ماب فقط.");
      window.open(`https://wa.me/?text=${encodeURIComponent(message)}`, "_blank", "noopener,noreferrer");
    } finally {
      setIsEmergencyLoading(false);
    }
  }

  function activateCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActivationError("");
    setActivationSuccess("");

    if (activationInput.trim().toUpperCase() !== activationCode) {
      setActivationError("الكود غير صحيح، تأكد من الكود أو اطلب كود التفعيل عبر واتساب");
      return;
    }

    // MVP activation is local-only; this can later move behind an API/database.
    window.localStorage.setItem("nitaaq-map-activated", "true");
    setActivationSuccess("تم تفعيل نطاق ماب بنجاح.");
    setIsActivated(true);
  }

  function coordinateToMapsUrl(latitude: number, longitude: number) {
    return `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;
  }

  if (!activationChecked) {
    return (
      <main className="grid min-h-screen place-items-center bg-[#0b1a2b] px-6 text-center text-white">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-emerald-300/30 border-t-emerald-300" />
      </main>
    );
  }

  if (!isActivated) {
    return (
      <IntroLanding
        activationInput={activationInput}
        activationError={activationError}
        activationSuccess={activationSuccess}
        activationRef={activationRef}
        onActivationInput={setActivationInput}
        onActivateCode={activateCode}
        onShowActivation={() => {
          setShowActivation(true);
          setTimeout(
            () => activationRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }),
            50,
          );
        }}
        showActivation={showActivation}
      />
    );
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
            <button
              type="button"
              onClick={() => setHelpOpen(true)}
              className="grid h-11 w-11 place-items-center rounded-2xl border border-white/10 bg-white/[0.04] text-slate-100 transition hover:border-emerald-300/40"
              aria-label="تعليمات السائق"
            >
              <HelpCircle className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={() => setTheme((current) => (current === "dark" ? "light" : "dark"))}
              className="grid h-11 w-11 place-items-center rounded-2xl border border-white/10 bg-white/[0.04] text-slate-100 transition hover:border-emerald-300/40"
              aria-label="تبديل الوضع الليلي"
            >
              {theme === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </button>
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
            <div>
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
              {linkValidation === "valid" && (
                <p className="mt-3 flex items-center gap-2 text-sm font-bold text-emerald-200">
                  <CheckCircle2 className="h-4 w-4" />
                  تم التحقق من رابط خرائط جوجل
                </p>
              )}
              {linkValidation === "invalid" && (
                <p className="mt-3 flex items-center gap-2 text-sm font-bold text-red-200">
                  <AlertCircle className="h-4 w-4" />
                  الرابط غير صحيح، استخدم رابط Google Maps أو إحداثيات مثل 24.7136,46.6753
                </p>
              )}
            </div>
            <button
              type="submit"
              data-create-qr-button
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

          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              onClick={createQrForCurrentLocation}
              disabled={isLocating || isLoading}
              className="inline-flex min-h-13 items-center justify-center gap-2 rounded-2xl border border-emerald-300/20 bg-emerald-300/8 px-4 py-3 text-sm font-extrabold text-emerald-100 transition hover:border-emerald-300/45 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLocating ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-emerald-100/30 border-t-emerald-100" />
              ) : (
                <Navigation className="h-5 w-5" />
              )}
              توليد كود لموقعي الحالي
            </button>
            <button
              type="button"
              onClick={shareEmergencyLocation}
              disabled={isEmergencyLoading}
              className="inline-flex min-h-13 items-center justify-center gap-2 rounded-2xl border border-red-300/25 bg-red-400/12 px-4 py-3 text-sm font-extrabold text-red-100 transition hover:border-red-300/50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isEmergencyLoading ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-red-100/30 border-t-red-100" />
              ) : (
                <MapPin className="h-5 w-5" />
              )}
              مشاركة موقعي مع جهة اتصال
            </button>
          </div>

          {error && (
            <div className="mt-4 flex items-center gap-3 rounded-2xl border border-red-400/20 bg-red-400/10 px-4 py-3 text-sm font-bold text-red-100">
              <AlertCircle className="h-5 w-5 shrink-0" />
              {error}
            </div>
          )}
          {infoMessage && (
            <div className="mt-4 flex items-center gap-3 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm font-bold text-emerald-100">
              <CheckCircle2 className="h-5 w-5 shrink-0" />
              {infoMessage}
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

                <div className={`mb-5 rounded-2xl border p-4 ${countdownTone.border} ${countdownTone.bg}`}>
                  <div className="flex items-center justify-between gap-3">
                    <div className={`text-sm font-black ${countdownTone.text}`}>
                      {countdownTone.label}
                    </div>
                    <div className={`text-3xl font-black ${countdownTone.text}`}>
                      {remainingLabel}
                    </div>
                  </div>
                  <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${countdownTone.bar}`}
                      style={{ width: `${countdownProgress}%` }}
                    />
                  </div>
                  <p className={`mt-3 text-sm font-bold ${countdownTone.text}`}>
                    {countdownTone.message}
                  </p>
                </div>

                {isExpired ? (
                  <div className="rounded-2xl border border-white/10 bg-[#071221]/70 p-5">
                    <p className="text-xl font-black text-white">
                      انتهت صلاحية الكود حفاظاً على خصوصيتك
                    </p>
                    <button
                      type="button"
                      onClick={resetTrip}
                      className="glow-button mt-4 inline-flex items-center justify-center gap-2 rounded-2xl px-5 py-3 font-extrabold text-white"
                    >
                      <Plus className="h-5 w-5" />
                      إعادة توليد QR
                    </button>
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

      {helpOpen && (
        <div className="fixed inset-0 z-[80] grid place-items-center bg-[#071221]/80 px-4 backdrop-blur-xl">
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 18 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            className="glass-panel w-full max-w-md rounded-[2rem] p-6"
          >
            <div className="mb-5 flex items-center justify-between gap-3">
              <h2 className="text-2xl font-black text-white">تعليمات السائق</h2>
              <button
                type="button"
                onClick={() => setHelpOpen(false)}
                className="rounded-2xl border border-white/10 px-4 py-2 font-black text-slate-100"
              >
                إغلاق
              </button>
            </div>
            <div className="grid gap-3">
              {[
                { icon: Camera, text: "افتح كاميرا الجوال." },
                { icon: QrCode, text: "وجّه الكاميرا نحو QR." },
                { icon: MousePointerClick, text: "اضغط على الرابط الذي يظهر." },
                { icon: Map, text: "ستفتح لك الرحلة في خرائط جوجل." },
              ].map((item, index) => (
                <motion.div
                  key={item.text}
                  initial={{ opacity: 0, x: 16 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.04] p-4"
                >
                  <div className="grid h-11 w-11 place-items-center rounded-2xl bg-emerald-300/10 text-emerald-200">
                    <item.icon className="h-5 w-5" />
                  </div>
                  <p className="font-bold text-slate-100">{item.text}</p>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>
      )}

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

function IntroLanding({
  activationInput,
  activationError,
  activationSuccess,
  activationRef,
  onActivationInput,
  onActivateCode,
  onShowActivation,
  showActivation,
}: {
  activationInput: string;
  activationError: string;
  activationSuccess: string;
  activationRef: { current: HTMLElement | null };
  onActivationInput: (value: string) => void;
  onActivateCode: (event: FormEvent<HTMLFormElement>) => void;
  onShowActivation: () => void;
  showActivation: boolean;
}) {
  const introFeatures = [
    { icon: LockKeyhole, title: "خصوصية أعلى", text: "شارك الرحلة بدون تسليم جوالك." },
    { icon: Clock3, title: "صلاحية مؤقتة", text: "QR ينتهي خلال 5 دقائق." },
    { icon: QrCode, title: "تجربة سهلة", text: "انسخ الرابط وأنشئ الكود فوراً." },
  ];

  return (
    <main className="relative min-h-screen overflow-x-hidden pb-[env(safe-area-inset-bottom)]">
      <div className="subtle-grid pointer-events-none fixed inset-0 opacity-35" />
      <div className="pointer-events-none fixed -top-44 right-[-10rem] h-[34rem] w-[34rem] rounded-full bg-blue-600/20 blur-3xl" />
      <div className="pointer-events-none fixed bottom-[-14rem] left-[-10rem] h-[36rem] w-[36rem] rounded-full bg-emerald-400/16 blur-3xl" />

      <header className="sticky top-0 z-50 border-b border-white/10 bg-[#0b1a2b]/72 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-3 py-3 sm:px-6 sm:py-4 lg:px-8">
          <Brand />
          <PrimaryLink href={activationWhatsappUrl}>
            <MessageCircle className="h-5 w-5" />
            اطلب كود التفعيل
          </PrimaryLink>
        </div>
      </header>

      <section className="relative z-10 mx-auto grid min-h-[calc(100svh-4.5rem)] max-w-7xl items-center gap-8 px-4 py-12 text-center sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:px-8 lg:text-right">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
          className="mx-auto max-w-3xl lg:mx-0"
        >
          <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-emerald-300/20 bg-emerald-300/8 px-4 py-2 text-sm font-bold text-emerald-200">
            <Sparkles className="h-4 w-4" />
            مشاركة الرحلات بأمان
          </div>
          <h1 className="text-[clamp(2.5rem,10vw,4.5rem)] font-black leading-tight text-white lg:text-7xl">
            نطاق | ماب
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg leading-9 text-slate-300 sm:text-2xl sm:leading-10 lg:mx-0">
            أنشئ QR مؤقت لرابط خرائط جوجل واحمِ خصوصيتك أثناء مشاركة الرحلة.
          </p>
          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row lg:justify-start">
            <button
              type="button"
              onClick={onShowActivation}
              className="glow-button inline-flex min-h-14 items-center justify-center gap-3 rounded-3xl px-7 py-4 text-lg font-black text-white transition hover:-translate-y-1"
            >
              <KeyRound className="h-6 w-6" />
              ابدأ الآن
            </button>
            <a
              href={activationWhatsappUrl}
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-3xl border border-white/10 bg-white/[0.04] px-7 py-4 font-extrabold text-slate-100 transition hover:border-emerald-300/40"
            >
              <CreditCard className="h-5 w-5" />
              20 ريال مدى الحياة
            </a>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ duration: 0.85, delay: 0.12, ease: [0.22, 1, 0.36, 1] }}
          className="glass-panel relative mx-auto w-full max-w-xl rounded-[2rem] p-5 sm:p-7"
        >
          <div className="relative h-72 overflow-hidden rounded-[1.6rem] border border-white/10 bg-[#071221]/70">
            <div className="absolute inset-8 rounded-[2rem] border border-emerald-300/15" />
            <motion.div
              animate={{ x: ["18%", "66%", "38%", "78%"], y: ["68%", "28%", "42%", "62%"] }}
              transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
              className="absolute grid h-14 w-14 place-items-center rounded-full bg-emerald-400 text-[#071221] shadow-[0_0_44px_rgba(34,197,94,0.45)]"
            >
              <MapPin className="h-7 w-7" />
            </motion.div>
            <Route className="absolute bottom-9 left-10 h-32 w-32 rotate-12 text-blue-300/25" />
            <Image
              src="/nitaaq-official-logo-transparent.png"
              alt="شعار نطاق الرسمي"
              width={420}
              height={420}
              className="absolute bottom-6 right-6 h-28 w-28 object-contain opacity-90"
            />
          </div>
        </motion.div>
      </section>

      <section className="relative z-10 mx-auto grid max-w-7xl gap-4 px-4 py-10 sm:px-6 md:grid-cols-3 lg:px-8">
        {introFeatures.map((feature) => (
          <article
            key={feature.title}
            className="rounded-[1.6rem] border border-white/10 bg-white/[0.045] p-6"
          >
            <div className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-emerald-300/10 text-emerald-200">
              <feature.icon className="h-6 w-6" />
            </div>
            <h2 className="text-2xl font-black text-white">{feature.title}</h2>
            <p className="mt-3 leading-8 text-slate-300">{feature.text}</p>
          </article>
        ))}
      </section>

      <section className="relative z-10 mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="glass-panel rounded-[2rem] p-6 sm:p-8">
          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <p className="mb-2 text-sm font-extrabold text-emerald-300">من نحن</p>
              <h2 className="text-3xl font-black text-white">لماذا نطاق ماب؟</h2>
              <p className="mt-4 leading-8 text-slate-300">
                خدمة بسيطة تساعدك تشارك مسار الرحلة بدون تسليم الجوال، مع صلاحية مؤقتة
                وتجربة مناسبة للسائق والراكب.
              </p>
            </div>
            <div>
              <p className="mb-2 text-sm font-extrabold text-emerald-300">كيف تعمل الخدمة</p>
              <div className="grid gap-3">
                {["انسخ رابط خرائط جوجل.", "أنشئ QR مؤقت.", "السائق يمسح الكود.", "تفتح الرحلة في خرائط جوجل."].map(
                  (step, index) => (
                    <div key={step} className="flex items-center gap-3 rounded-2xl bg-white/[0.04] p-4">
                      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-blue-600 to-emerald-400 font-black text-white">
                        {index + 1}
                      </span>
                      <span className="font-bold text-slate-100">{step}</span>
                    </div>
                  ),
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {showActivation && (
        <section
          ref={activationRef}
          className="relative z-10 mx-auto max-w-2xl scroll-mt-24 px-4 py-12 sm:px-6 lg:px-8"
        >
          <div className="glass-panel rounded-[2rem] p-6 sm:p-8">
            <div className="text-center">
              <p className="mb-2 text-sm font-extrabold text-emerald-300">تفعيل الخدمة</p>
              <h2 className="text-3xl font-black text-white">20 ريال مدى الحياة</h2>
              <p className="mt-3 leading-8 text-slate-300">
                اطلب الكود عبر واتساب ثم فعّل الدخول إلى صفحة الخدمة.
              </p>
            </div>

            <a
              href={activationWhatsappUrl}
              className="glow-button mt-6 flex min-h-14 items-center justify-center gap-2 rounded-3xl px-6 py-4 font-black text-white"
            >
              <MessageCircle className="h-5 w-5" />
              اطلب كود التفعيل
            </a>

            <form onSubmit={onActivateCode} className="mt-5 grid gap-3">
              <input
                value={activationInput}
                onChange={(event) => onActivationInput(event.target.value)}
                placeholder="أدخل كود التفعيل"
                dir="ltr"
                className="h-14 rounded-2xl border border-white/10 bg-[#071221]/70 px-4 text-center text-base font-bold text-white outline-none transition placeholder:text-center placeholder:text-slate-500 focus:border-emerald-300/60 focus:ring-4 focus:ring-emerald-300/10"
              />
              <button
                type="submit"
                className="inline-flex min-h-14 items-center justify-center rounded-2xl bg-white px-5 py-3 font-black text-[#0b1a2b] transition hover:-translate-y-0.5"
              >
                تفعيل الكود
              </button>
            </form>

            {activationError && (
              <p className="mt-4 rounded-2xl border border-red-300/20 bg-red-400/10 p-4 text-sm font-bold text-red-100">
                {activationError}
              </p>
            )}
            {activationSuccess && (
              <p className="mt-4 rounded-2xl border border-emerald-300/20 bg-emerald-400/10 p-4 text-sm font-bold text-emerald-100">
                {activationSuccess}
              </p>
            )}
          </div>
        </section>
      )}
    </main>
  );
}
