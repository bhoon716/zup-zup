/**
 * Web Push utility functions
 */

// Placeholder VAPID primary key - In production, this should be in .env
const VAPID_PUBLIC_KEY = process.env.NEXT_PUBLIC_VAPID_KEY || "BPEg3-f1G-h9l_Z6_k-G2m7v_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_Xv_X";

/**
 * Convert base64 string to Uint8Array for VAPID key
 */
function urlBase64ToUint8Array(base64String: string) {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

/**
 * Register Service Worker and subscribe to Push Manager
 */
export async function subscribeToPush() {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
    throw new Error("Push notifications are not supported in this browser.");
  }

  const registration = await navigator.serviceWorker.register("/sw.js");
  
  // Wait for service worker to be ready
  await navigator.serviceWorker.ready;

  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
  });

  return subscription;
}

/**
 * Get current push subscription if exists
 */
export async function getSubscription() {
  if (!("serviceWorker" in navigator)) return null;
  
  const registration = await navigator.serviceWorker.ready;
  return await registration.pushManager.getSubscription();
}

/**
 * Unsubscribe from Push Manager
 */
export async function unsubscribeFromPush() {
  const subscription = await getSubscription();
  if (subscription) {
    await subscription.unsubscribe();
    return true;
  }
  return false;
}
