/**
 * 관리자 페이지에서 사용하는 데이터 포맷터 유틸리티입니다.
 */

/**
 * 숫자를 한국 로케일 형식(천 단위 콤마)으로 변환합니다.
 */
export function formatNumber(value: number | null | undefined): string {
  return typeof value === "number" ? value.toLocaleString() : "-";
}

/**
 * 날짜와 시간을 'YYYY. MM. DD. HH:mm:ss' 형식으로 변환합니다.
 */
export function formatDateTime(value?: string | null): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

/**
 * 시간을 'HH:mm:ss' 형식으로 변환합니다.
 */
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

/**
 * 현재 시간으로부터 얼마나 지났는지 상대적인 시간(초/분/시간 전)으로 표시합니다.
 */
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

/**
 * 크롤러 상태에 따른 UI 메타데이터를 반환합니다.
 */
export function getStatusMeta(status: string) {
  if (status === "RUNNING") {
    return {
      label: "정상 작동",
      badgeClass: "bg-green-100 text-green-700",
      dotClass: "bg-green-500",
    };
  }
  if (status === "DEGRADED") {
    return {
      label: "지연 감지",
      badgeClass: "bg-amber-100 text-amber-700",
      dotClass: "bg-amber-500",
    };
  }
  return {
    label: "상태 미확인",
    badgeClass: "bg-slate-200 text-slate-600",
    dotClass: "bg-slate-400",
  };
}

/**
 * 로그 레벨에 따른 UI 메타데이터를 반환합니다.
 */
export function getLogMeta(level: string) {
  if (level === "ERROR") return { label: "오류", className: "bg-red-100 text-red-800" };
  if (level === "WARN") return { label: "경고", className: "bg-amber-100 text-amber-800" };
  return { label: "정보", className: "bg-green-100 text-green-800" };
}
