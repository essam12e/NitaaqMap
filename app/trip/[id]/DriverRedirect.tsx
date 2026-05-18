"use client";

import { useEffect } from "react";
import { LoaderCircle, MapPinned } from "lucide-react";

export function DriverRedirect({ url }: { url: string }) {
  useEffect(() => {
    const timeout = window.setTimeout(() => {
      window.location.replace(url);
    }, 1200);

    return () => window.clearTimeout(timeout);
  }, [url]);

  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden px-6 py-16 text-center">
      <div className="subtle-grid absolute inset-0 opacity-50" />
      <div className="absolute -top-32 right-1/2 h-80 w-80 translate-x-1/2 rounded-full bg-emerald-400/20 blur-3xl" />
      <section className="glass-panel relative z-10 w-full max-w-md rounded-[2rem] p-8">
        <div className="mx-auto mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-emerald-400/10 text-emerald-300 ring-1 ring-emerald-300/20">
          <MapPinned className="h-8 w-8" />
        </div>
        <h1 className="text-3xl font-black">جاري فتح الرحلة...</h1>
        <p className="mt-3 leading-8 text-slate-300">
          سيتم تحويل السائق الآن إلى رابط Google Maps.
        </p>
        <LoaderCircle className="mx-auto mt-7 h-8 w-8 animate-spin text-emerald-300" />
      </section>
    </main>
  );
}
