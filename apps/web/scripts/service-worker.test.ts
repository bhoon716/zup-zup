import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import vm from "node:vm";
import { describe, expect, it, vi } from "vitest";

type ServiceWorkerEventHandler = (event: {
  data?: { json: () => Record<string, unknown> };
  waitUntil: (work: Promise<unknown>) => void;
}) => void;

const serviceWorkerSource = readFileSync(
  resolve(process.cwd(), "public/sw.js"),
  "utf8",
);

function loadServiceWorker() {
  const listeners = new Map<string, ServiceWorkerEventHandler>();
  const showNotification = vi.fn().mockResolvedValue(undefined);
  const postMessage = vi.fn();
  const matchAll = vi.fn().mockResolvedValue([{ postMessage }]);

  const workerScope = {
    addEventListener: (type: string, listener: ServiceWorkerEventHandler) => listeners.set(type, listener),
    skipWaiting: vi.fn(),
    clients: {
      claim: vi.fn(),
      matchAll,
    },
    registration: { showNotification },
    location: { origin: "https://zup-zup.example" },
  };

  vm.runInNewContext(serviceWorkerSource, {
    self: workerScope,
    clients: workerScope.clients,
    fetch: vi.fn(),
    URL,
  });

  return { listeners, showNotification };
}

async function dispatchPush(
  listener: ServiceWorkerEventHandler,
  payload: Record<string, unknown>,
) {
  let work: Promise<unknown> | undefined;
  listener({
    data: { json: () => payload },
    waitUntil: (promise) => {
      work = promise;
    },
  });
  await work;
}

describe("service worker push notification", () => {
  it("uses the delivery idempotency key as a stable notification tag without renotifying", async () => {
    const { listeners, showNotification } = loadServiceWorker();
    const idempotencyKey = "0b6dceb7-35ae-42d5-bc79-9185240b23c1";

    await dispatchPush(listeners.get("push")!, {
      title: "여석 발생",
      body: "알고리즘 수강 가능",
      idempotencyKey,
      url: "/courses/CS-101",
    });

    expect(showNotification).toHaveBeenCalledWith("여석 발생", expect.objectContaining({
      tag: idempotencyKey,
      renotify: false,
    }));
  });

  it("keeps legacy payloads independent when they do not contain an idempotency key", async () => {
    const { listeners, showNotification } = loadServiceWorker();

    await dispatchPush(listeners.get("push")!, {
      title: "여석 발생",
      body: "알고리즘 수강 가능",
    });

    const notificationOptions = showNotification.mock.calls[0][1];
    expect(notificationOptions).not.toHaveProperty("tag");
    expect(notificationOptions).not.toHaveProperty("renotify");
  });
});
