/* Service worker: lets the web app install and open offline.
   It only ever touches same-origin GET requests, so Supabase, the build Worker and
   signed download links always go straight to the network and are never cached. */
const CACHE = 'zip-to-apk-v2';
const SHELL = ['./', './index.html', './manifest.webmanifest', './favicon.svg', './icons/icon-192.png', './icons/icon-512.png'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => Promise.allSettled(SHELL.map((url) => cache.add(url))))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith('zip-to-apk-') && k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

function remember(request, response) {
  if (response && response.ok && response.type === 'basic') {
    const copy = response.clone();
    caches.open(CACHE).then((cache) => cache.put(request, copy)).catch(() => {});
  }
  return response;
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // Pages: network first, fall back to the cached shell when offline.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((res) => remember(request, res))
        .catch(() => caches.match(request).then((hit) => hit || caches.match('./index.html'))),
    );
    return;
  }

  // Hashed build assets never change: serve from cache, refresh in the background.
  if (url.pathname.includes('/assets/')) {
    event.respondWith(
      caches.match(request).then((hit) => {
        const fresh = fetch(request).then((res) => remember(request, res)).catch(() => hit);
        return hit || fresh;
      }),
    );
    return;
  }

  event.respondWith(fetch(request).then((res) => remember(request, res)).catch(() => caches.match(request)));
});
