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
    "fbav",
    "fban",
    "fb_iab",
    "messenger",
    "discord",
    "line",
    "naver",
    "daum",
    "thunderbird",
    "everytime",
    "wv",
    "bridge",
  ];

  const isWebView = (ua.includes('iphone') || ua.includes('ipad') || ua.includes('ipod')) 
    && ua.includes('applewebkit') 
    && !ua.includes('safari');

  return inAppPatterns.some(pattern => ua.includes(pattern)) || isWebView;
}
