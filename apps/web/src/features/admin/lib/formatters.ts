export function formatTime(value?: string | null): string {
  if (!value) return "--:--:--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--:--:--";

  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

export function formatRelative(value?: string | null): string {
  if (!value) return "기록 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "기록 없음";

  const diffMs = Date.now() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return `${diffSec}초 전`;

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;

  const diffHour = Math.floor(diffMin / 60);
  return `${diffHour}시간 전`;
}

export function getStatusMeta(status: string) {
  if (status === "RUNNING") {
    return {
      label: "정상 작동",
      badgeClass: "bg-green-100 text-green-700",
    };
  }
  if (status === "DEGRADED") {
    return {
      label: "지연 감지",
      badgeClass: "bg-amber-100 text-amber-700",
    };
  }
  return {
    label: "상태 미확인",
    badgeClass: "bg-slate-200 text-slate-600",
  };
}
