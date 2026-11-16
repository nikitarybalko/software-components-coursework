"use client";

import { Loader2 } from "lucide-react";
import { useEffect, useState, type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/presentation";

export function Panel({ index, title, aside, children, className, bodyClassName }: {
  index: number;
  title: string;
  aside?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
}) {
  return (
    <section className={cn("border border-ink bg-sheet", className)}>
      <header className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 border-b border-ink px-3 py-1.5">
        <h2 className="font-heading text-[13px] font-semibold uppercase text-ink">
          <span className="mr-1.5 font-mono font-normal text-ink-faint">{index}.</span>
          {title}
        </h2>
        {aside}
      </header>
      <div className={cn("p-3", bodyClassName)}>{children}</div>
    </section>
  );
}

type ButtonVariant = "primary" | "secondary" | "danger";

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: "border-ink bg-ink text-sheet hover:bg-olive hover:border-olive",
  secondary: "border-ink bg-sheet text-ink hover:bg-paper",
  danger: "border-alert bg-alert text-sheet",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  busy?: boolean;
}

export function Button({ variant = "secondary", busy, disabled, className, children, ...props }: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || busy}
      className={cn(
        "inline-flex items-center justify-center gap-2 border px-3 py-1.5 font-heading text-sm font-medium",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-friend",
        "disabled:cursor-not-allowed disabled:opacity-35",
        BUTTON_VARIANTS[variant],
        className,
      )}
      {...props}
    >
      {busy && <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden />}
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

/** Small filled square used as a side or state marker next to text. */
export function Marker({ color, className }: { color: string; className?: string }) {
  return <span className={cn("inline-block h-2 w-2 shrink-0 border border-ink", className)} style={{ backgroundColor: color }} aria-hidden />;
}
