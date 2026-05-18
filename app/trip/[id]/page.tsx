import Link from "next/link";
import { redirect } from "next/navigation";
import { AlertTriangle, ArrowRight } from "lucide-react";
import { verifyTripToken } from "@/lib/trip-token";

type TripPageProps = {
  params: Promise<{
    id: string;
  }>;
};

export default async function TripPage({ params }: TripPageProps) {
  const { id } = await params;
  const trip = verifyTripToken(id);

  if (trip.ok) {
    redirect(trip.url);
  }

  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden px-6 py-16 text-center">
      <div className="subtle-grid absolute inset-0 opacity-45" />
      <div className="absolute -top-28 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-blue-500/20 blur-3xl" />
      <div className="absolute bottom-0 right-0 h-72 w-72 rounded-full bg-emerald-400/15 blur-3xl" />

      <section className="glass-panel relative z-10 w-full max-w-lg rounded-[2rem] p-8 sm:p-10">
        <div className="mx-auto mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-amber-400/10 text-amber-200 ring-1 ring-amber-200/20">
          <AlertTriangle className="h-8 w-8" />
        </div>
        <h1 className="text-3xl font-black sm:text-4xl">انتهت صلاحية رابط الرحلة</h1>
        <p className="mt-4 text-lg leading-9 text-slate-300">
          اطلب من الراكب إنشاء كود جديد.
        </p>
        <Link
          href="/"
          className="glow-button mt-8 inline-flex items-center justify-center gap-2 rounded-2xl px-7 py-4 text-base font-extrabold text-white transition hover:-translate-y-0.5 hover:shadow-emerald-400/30"
        >
          <ArrowRight className="h-5 w-5" />
          رجوع للرئيسية
        </Link>
      </section>
    </main>
  );
}
