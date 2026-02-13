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
    "fb_iab", // Facebook In-App Browser
    "messenger",
    "discord",
    "line",
    "naver",
    "daum",
    "thunderbird",
    "everytime", // 에브리타임
    "whale",
    "wv", // Android WebView marker
    "bridge", // Some internal webviews
  ];
  
  // Chrome on iOS (CriOS) or Safari on iOS are NOT in-app webviews in this context
  // but they often have 'safari' or 'crios'.
  // However, WebViews usually LACK the standalone 'safari' string while having 'applewebkit'
  
  const isWebView = (ua.includes('iphone') || ua.includes('ipad') || ua.includes('ipod')) 
    && ua.includes('applewebkit') 
    && !ua.includes('safari');

  return inAppPatterns.some(pattern => ua.includes(pattern)) || isWebView;
}
