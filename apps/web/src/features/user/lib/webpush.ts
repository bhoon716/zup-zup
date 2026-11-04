

import { buildClientErrorEvent, reportClientError } from "@/shared/telemetry/client-error";

const VAPID_PUBLIC_KEY = process.env.NEXT_PUBLIC_VAPID_KEY;


function isSameUint8Array(a: Uint8Array, b: Uint8Array) {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i += 1) {
    if (a[i] !== b[i]) return false;
  }
  return true;
}


function hasSameApplicationServerKey(subscription: PushSubscription, expectedKey: Uint8Array) {
  const existingKeyBuffer = subscription.options?.applicationServerKey;
  if (!existingKeyBuffer) return false;

  const existingKey = new Uint8Array(existingKeyBuffer);
  return isSameUint8Array(existingKey, expectedKey);
}


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


export function extractSubscriptionKeys(subscription: PushSubscription) {
  const json = subscription.toJSON();
  const endpoint = json.endpoint;
  const p256dh = json.keys?.p256dh;
  const auth = json.keys?.auth;

  if (!endpoint || !p256dh || !auth) {
    throw new Error("푸시 구독 정보가 올바르지 않습니다.");
  }

  return { endpoint, p256dh, auth };
}



export async function getSubscription() {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) return null;
  
  try {
    const registration = await Promise.race([
      navigator.serviceWorker.ready,
      new Promise<never>((_, reject) => setTimeout(() => reject(new Error("서비스 워커 로드 타임아웃")), 15000))
    ]);
    return await registration.pushManager.getSubscription();
  } catch (error) {
    console.warn("브라우저 웹 푸시 구독 조회 실패.",
      buildClientErrorEvent(error, { source: "web-push", operation: "subscription-lookup" }));
    return null;
  }
}


export async function subscribeToPush() {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
    throw new Error("이 브라우저에서는 웹 푸시를 사용할 수 없습니다. iPhone/iPad는 홈 화면에 추가한 뒤 실행해 주세요. 웹 푸시를 끄고 이메일 등 다른 알림으로 계속할 수 있습니다.");
  }

  const registration = await navigator.serviceWorker.register("/sw.js");
  
  await Promise.race([
    navigator.serviceWorker.ready,
    new Promise<never>((_, reject) => setTimeout(() => reject(new Error("서비스 워커 로드 타임아웃")), 15000))
  ]);

  if (!VAPID_PUBLIC_KEY) {
    throw new Error("VAPID Public Key가 설정되지 않았습니다.");
  }

  const applicationServerKey = urlBase64ToUint8Array(VAPID_PUBLIC_KEY);
  const existingSubscription = await registration.pushManager.getSubscription();

  if (existingSubscription) {
    if (hasSameApplicationServerKey(existingSubscription, applicationServerKey)) {
      return existingSubscription;
    }


    await existingSubscription.unsubscribe();
  }

  try {
    return await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey,
    });
  } catch (error) {

    if (error instanceof DOMException && error.name === "InvalidStateError") {
      const staleSubscription = await registration.pushManager.getSubscription();
      if (staleSubscription) {
        await staleSubscription.unsubscribe();
      }

      return await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey,
      });
    }

    throw error;
  }
}


export async function unsubscribeFromPush() {
  try {
    const subscription = await getSubscription();
    if (subscription) {
      await subscription.unsubscribe();
      return true;
    }
  } catch (error) {
    reportClientError(error, { source: "web-push", operation: "unsubscribe" });
  }
  return false;
}
