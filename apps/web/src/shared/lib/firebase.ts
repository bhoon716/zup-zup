import { FirebaseApp, getApp, getApps, initializeApp } from "firebase/app";
import { Messaging, getMessaging, isSupported } from "firebase/messaging";

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

const hasFirebaseConfig = Object.values(firebaseConfig).every(Boolean);
let cachedMessagingPromise: Promise<Messaging | null> | null = null;

export function getFirebaseApp(): FirebaseApp | null {
  if (!hasFirebaseConfig) {
    return null;
  }

  if (getApps().length > 0) {
    return getApp();
  }

  return initializeApp(firebaseConfig);
}

export async function getFirebaseMessaging(): Promise<Messaging | null> {
  if (typeof window === "undefined") {
    return null;
  }

  const app = getFirebaseApp();
  if (!app) {
    return null;
  }

  if (!cachedMessagingPromise) {
    cachedMessagingPromise = isSupported().then((supported) => {
      if (!supported) {
        return null;
      }
      return getMessaging(app);
    });
  }

  return cachedMessagingPromise;
}
