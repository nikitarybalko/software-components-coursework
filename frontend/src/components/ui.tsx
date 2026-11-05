"use client";

import { Loader2 } from "lucide-react";
import { useEffect, useState, type ButtonHTMLAttributes, type ReactNode } from "react";
import { TONE_CLASSES, cn, type Tone } from "@/lib/presentation";

export function Panel({ title, icon, actions, children, className }: {
  title: string;
  icon?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("rounded-xl border border-slate-800 bg-slate-900/70 shadow-lg shadow-black/20", className)}>
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 px-4 py-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-slate-300">
          {icon}
          {title}
        </h2>
        {actions}
      </header>
      <div className="p-4">{children}</div>
    </section>
  );
}

export function Badge({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-medium", TONE_CLASSES[tone], className)}>
      {children}
    </span>
  );
}

type ButtonVariant = "primary" | "secondary" | "danger";

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: "border-sky-600 bg-sky-600 text-white hover:bg-sky-500",
  secondary: "border-slate-700 bg-slate-800 text-slate-100 hover:bg-slate-700",
  danger: "border-rose-700 bg-rose-700 text-white hover:bg-rose-600",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  icon?: ReactNode;
  busy?: boolean;
}

export function Button({ variant = "secondary", icon, busy, disabled, className, children, ...props }: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || busy}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400",
        "disabled:cursor-not-allowed disabled:opacity-40",
        BUTTON_VARIANTS[variant],
        className,
      )}
      {...props}
    >
      {busy ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : icon}
      {children}
    </button>
  );
}

const CONFIRM_WINDOW_MS = 3000;

/** First click arms the button, the second click within a few seconds performs the action. */
export function ConfirmButton({ onConfirm, confirmLabel = "Підтвердити?", variant = "secondary", ...props }: ButtonProps & {
  onConfirm: () => void;
  confirmLabel?: string;
}) {
  const [armed, setArmed] = useState(false);

  useEffect(() => {
    if (!armed) {
      return;
    }
    const timer = setTimeout(() => setArmed(false), CONFIRM_WINDOW_MS);
    return () => clearTimeout(timer);
  }, [armed]);

  const handleClick = () => {
    if (armed) {
      setArmed(false);
      onConfirm();
    } else {
      setArmed(true);
    }
  };

  return (
    <Button {...props} variant={armed ? "danger" : variant} onClick={handleClick}>
      {armed ? confirmLabel : props.children}
    </Button>
  );
}
