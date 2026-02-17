/** 웹 푸시 관련 유틸 함수 모음. */

const VAPID_PUBLIC_KEY = process.env.NEXT_PUBLIC_VAPID_KEY;

/** 공개키 문자열을 푸시 구독에 필요한 바이트 배열로 변환한다. */
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

/** 구독 객체에서 서버 등록에 필요한 키를 추출한다. */
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


/** 현재 브라우저의 푸시 구독을 조회한다. */
export async function getSubscription() {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) return null;
  
  try {
    const registration = await Promise.race([
      navigator.serviceWorker.ready,
      new Promise<never>((_, reject) => setTimeout(() => reject(new Error("Service Worker timeout")), 5000))
    ]);
    return await registration.pushManager.getSubscription();
  } catch (error) {
    console.warn("Failed to get push subscription:", error);
    return null;
  }
}

/** 서비스 워커를 등록하고 푸시 구독을 생성한다. */
export async function subscribeToPush() {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
    throw new Error("브라우저가 푸시 알림을 지원하지 않습니다.");
  }

  const registration = await navigator.serviceWorker.register("/sw.js");
  
  await Promise.race([
    navigator.serviceWorker.ready,
    new Promise<never>((_, reject) => setTimeout(() => reject(new Error("서비스 워커 로드 타임아웃")), 5000))
  ]);

  if (!VAPID_PUBLIC_KEY) {
    throw new Error("VAPID Public Key가 설정되지 않았습니다.");
  }

  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
  });

  return subscription;
}

/** 현재 푸시 구독을 해제한다. */
export async function unsubscribeFromPush() {
  try {
    const subscription = await getSubscription();
    if (subscription) {
      await subscription.unsubscribe();
      return true;
    }
  } catch (error) {
    console.error("Failed to unsubscribe:", error);
  }
  return false;
}
