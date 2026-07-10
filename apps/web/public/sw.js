self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

// PWA 설치 요건을 충족하기 위한 fetch 이벤트 핸들러 추가
self.addEventListener("fetch", (event) => {
  // 현재는 특별한 캐싱 로직 없이 네트워크 요청을 그대로 통과시킵니다.
  // PWA로 인식되기 위해 핸들러가 존재하는 것 자체가 중요합니다.
  event.respondWith(fetch(event.request));
});

self.addEventListener("push", (event) => {
  if (!event.data) return;

  const data = event.data.json();
  const options = {
    body: data.body,
    icon: "/zub-zub-logo.png",
    badge: "/zub-zub-logo.png",
    vibrate: [100, 50, 100],
    data: {
      dateOfArrival: Date.now(),
      primaryKey: 1,
      url: data.url || "/",
    },
    requireInteraction: true,
  };

  event.waitUntil(
    Promise.all([
      self.registration.showNotification(data.title, options),
      self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
        clients.forEach(client => {
          client.postMessage({
            type: 'PUSH_NOTIFICATION',
            title: data.title,
            body: data.body,
            url: data.url
          });
        });
      })
    ])
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const requestedUrl = event.notification.data?.url;
  const fallbackUrl = new URL("/", self.location.origin).href;
  let urlToOpen = fallbackUrl;

  try {
    const url = new URL(requestedUrl || "/", self.location.origin);
    const allowedExactPaths = ["/", "/search", "/notifications", "/timetable"];
    const allowedPathPrefixes = ["/courses/", "/announcements/"];
    const isAllowedPath = allowedExactPaths.includes(url.pathname)
      || allowedPathPrefixes.some((prefix) => url.pathname.startsWith(prefix)
        && url.pathname.length > prefix.length);

    if ((url.protocol === "http:" || url.protocol === "https:")
      && url.origin === self.location.origin
      && isAllowedPath) {
      urlToOpen = url.href;
    }
  } catch {
    urlToOpen = fallbackUrl;
  }

  const promiseChain = clients
    .matchAll({
      type: "window",
      includeUncontrolled: true,
    })
    .then((windowClients) => {
      let matchingClient = null;
      for (let i = 0; i < windowClients.length; i++) {
        const windowClient = windowClients[i];
        if (windowClient.url === urlToOpen) {
          matchingClient = windowClient;
          break;
        }
      }

      if (matchingClient) {
        return matchingClient.focus();
      } else {
        return clients.openWindow(urlToOpen);
      }
    });

  event.waitUntil(promiseChain);
});
