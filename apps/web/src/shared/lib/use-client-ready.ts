"use client";

import { useEffect, useSyncExternalStore } from "react";

let isClientReady = false;
const listeners = new Set<() => void>();

const subscribe = (listener: () => void) => {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
};

const getSnapshot = () => isClientReady;





export const useClientReady = () => {
  const ready = useSyncExternalStore(subscribe, getSnapshot, () => false);

  useEffect(() => {
    if (isClientReady) {
      return;
    }

    isClientReady = true;
    listeners.forEach((listener) => listener());
  }, []);

  return ready;
};
