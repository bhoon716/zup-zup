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

/**
 * 브라우저에서 첫 마운트 이후에만 true가 되는 클라이언트 준비 상태를 제공합니다.
 * strict mode의 첫 번째 throwaway mount에서 네트워크를 쏘지 않기 위한 용도입니다.
 */
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
