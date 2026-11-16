import { CircleAlert, CircleCheck, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";

export const notificationInputClassName =
  "h-[38px] rounded-[14px] border-[#dde0e6] bg-white px-[11px] text-sm shadow-none placeholder:text-[#6b7280] dark:border-input dark:bg-card dark:placeholder:text-muted-foreground";

export const notificationButtonClassName =
  "h-[38px] gap-1.5 rounded-[14px] px-[13px] text-[13px] font-semibold shadow-none has-[>svg]:px-[13px] data-[variant=outline]:border-[#e0d8e5] data-[variant=outline]:bg-white data-[variant=outline]:text-primary data-[variant=outline]:hover:bg-[#f4eef7] dark:data-[variant=outline]:border-border dark:data-[variant=outline]:bg-card dark:data-[variant=outline]:hover:bg-accent";

export const notificationDiscordButtonClassName =
  "bg-[#5865f2] text-white hover:bg-[#4c58de]";

interface NotificationSettingsLayoutProps {
  description: string;
  standalone?: boolean;
  children: ReactNode;
}

export function NotificationSettingsLayout({ description, standalone, children }: NotificationSettingsLayoutProps) {
  return (
    <div className={cn(
      "bg-[#fafbfc] px-3 py-3 text-[#101828] min-[621px]:px-4 min-[621px]:py-6 dark:bg-background dark:text-foreground",
      standalone ? "min-h-svh" : "min-h-[calc(100svh-4rem)]",
    )}>
      <main className="mx-auto w-full max-w-[744px] rounded-3xl border border-[#e5e7eb] bg-card px-[13px] py-4 shadow-[0_6px_18px_#1018280b] min-[621px]:p-6 dark:border-border">
        <h1 className="text-[23px] leading-[1.35] font-bold tracking-[-0.025em] min-[621px]:text-[26px]">알림 설정</h1>
        <p className="mt-[5px] text-sm leading-normal text-muted-foreground">{description}</p>
        {children}
      </main>
    </div>
  );
}

interface NotificationChannelProps {
  id: string;
  title: string;
  description: string;
  icon: LucideIcon;
  iconClassName: string;
  toggle: ReactNode;
  children: ReactNode;
}

export function NotificationChannel({ id, title, description, icon: Icon, iconClassName, toggle, children }: NotificationChannelProps) {
  const headingId = `${id}-heading`;

  return (
    <section
      aria-labelledby={headingId}
      className="grid grid-cols-[minmax(0,1fr)_auto] items-start gap-x-1.5 gap-y-[7px] rounded-[20px] border border-[#e8eaee] bg-[#faf9fc] p-[9px] min-[621px]:grid-cols-[minmax(0,216px)_minmax(0,1fr)_auto] min-[621px]:gap-3 min-[621px]:px-[13px] min-[621px]:py-[11px] dark:border-border dark:bg-muted/50"
    >
      <div className="flex min-w-0 items-center gap-[9px]">
        <div className={cn("grid size-[34px] shrink-0 place-items-center rounded-full", iconClassName)}>
          <Icon aria-hidden="true" className="size-[17px]" />
        </div>
        <div className="min-w-0">
          <h2 id={headingId} className="text-sm leading-6 font-bold">{title}</h2>
          <p className="mt-0.5 text-xs leading-normal text-muted-foreground">{description}</p>
        </div>
      </div>
      <div className="col-span-2 min-w-0 min-[621px]:col-span-1 min-[621px]:col-start-2 min-[621px]:row-start-1">
        {children}
      </div>
      <div className="col-start-2 row-start-1 mt-[3px] justify-self-end min-[621px]:col-start-3">{toggle}</div>
    </section>
  );
}

interface NotificationDeviceAliasProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  onRegister: () => void;
  buttonLabel?: string;
  disabled?: boolean;
  registerDisabled?: boolean;
  invalid?: boolean;
}

export function NotificationDeviceAlias({
  id, value, onChange, onRegister, buttonLabel = "현재 기기 등록", disabled, registerDisabled, invalid,
}: NotificationDeviceAliasProps) {
  return (
    <div>
      <label htmlFor={id} className="mx-0.5 mb-[5px] block text-xs font-semibold text-muted-foreground">
        기기 별칭<span aria-hidden="true" className="ms-1 text-[11px] text-primary">필수</span>
      </label>
      <div className="flex items-center gap-1.5">
        <Input
          id={id}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key !== "Enter" || event.nativeEvent.isComposing) return;
            event.preventDefault();
            if (!disabled && !registerDisabled) onRegister();
          }}
          aria-required="true"
          aria-invalid={invalid || undefined}
          aria-describedby={invalid ? `${id}-error` : undefined}
          placeholder="예시: 노트북"
          maxLength={40}
          disabled={disabled}
          className={notificationInputClassName}
        />
        <Button
          type="button"
          variant="outline"
          className={notificationButtonClassName}
          disabled={disabled || registerDisabled}
          onClick={onRegister}
        >
          {buttonLabel}
        </Button>
      </div>
    </div>
  );
}

export function NotificationEmailStatus({ verified }: { verified: boolean }) {
  const Icon = verified ? CircleCheck : CircleAlert;
  const label = verified ? "인증된 이메일" : "인증이 필요한 이메일";

  return (
    <span
      title={label}
      role="img"
      aria-label={label}
      className={cn("pointer-events-none absolute end-[11px] top-1/2 -translate-y-1/2", verified
        ? "text-[#067347] dark:text-emerald-400"
        : "text-[#926018] dark:text-amber-400")}
    >
      <Icon aria-hidden="true" className="size-[17px]" />
    </span>
  );
}

export function NotificationActions({ children }: { children: ReactNode }) {
  return (
    <footer className="mt-[18px] flex flex-wrap items-center justify-end gap-2 border-t border-[#e8eaee] pt-3.5 max-[380px]:gap-1.5 max-[380px]:[&>button]:gap-1 max-[380px]:[&>button]:px-2 max-[380px]:[&>button]:text-xs dark:border-border">
      {children}
    </footer>
  );
}
