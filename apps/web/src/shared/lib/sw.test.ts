import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import vm from "node:vm";
import { describe, expect, it, vi } from "vitest";

describe("service worker notification navigation", () => {
  it("opens the safe root for an external notification URL", async () => {
    type NotificationClickEvent = {
      notification: { data: { url: string }; close: () => void };
      waitUntil: (promise: Promise<unknown>) => void;
    };
    const listeners: Record<string, (event: unknown) => void> = {};
    const openWindow = vi.fn().mockResolvedValue(undefined);
    const context = {
      self: {
        location: { origin: "https://zup-zup.com" },
        addEventListener: (name: string, listener: (event: unknown) => void) => {
          listeners[name] = listener;
        },
      },
      clients: {
        matchAll: vi.fn().mockResolvedValue([]),
        openWindow,
      },
      URL,
      Promise,
      fetch: vi.fn(),
    };
    const source = readFileSync(resolve(process.cwd(), "public/sw.js"), "utf8");
    vm.runInNewContext(source, context);

    let notificationPromise: Promise<unknown> | undefined;
    const close = vi.fn();
    listeners.notificationclick({
      notification: { data: { url: "https://evil.example/" }, close },
      waitUntil: (promise: Promise<unknown>) => {
        notificationPromise = promise;
      },
    } satisfies NotificationClickEvent);
    await notificationPromise;

    expect(close).toHaveBeenCalledOnce();
    expect(openWindow).toHaveBeenCalledWith("https://zup-zup.com/");
  });
});
