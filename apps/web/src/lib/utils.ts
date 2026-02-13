import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function isInAppBrowser() {
  if (typeof window === "undefined") return false;
  const ua = window.navigator.userAgent.toLowerCase();
  
  const inAppPatterns = [
    "kakaotalk",
    "instagram",
    "fbav", // Facebook App for Android/iOS
    "fban", // Facebook App for iOS
    "discord",
    "line",
    "naver",
    "daum",
    "thunderbird"
  ];
  
  return inAppPatterns.some(pattern => ua.includes(pattern));
}
