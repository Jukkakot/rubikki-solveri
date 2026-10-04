// Offline support. The build writes the version into CACHE (generateBuildInfo) and lists the
// distribution's files in precache.json (writePrecache). A new build is fetched in the background
// when the page starts and used from the start after that.
const CACHE = 'rubikki-__VERSION__';

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE);
    const files = await (await fetch('precache.json', { cache: 'no-cache' })).json();
    await cache.addAll(files.map((f) => new Request(f, { cache: 'reload' })));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    for (const key of await caches.keys()) {
      if (key.startsWith('rubikki-') && key !== CACHE) await caches.delete(key);
    }
    await self.clients.claim();
  })());
});

// Pages and scripts: network first (a fresh build when online), the cache when offline.
// Everything else (wasm, fonts, texts, icons): cache first; their names change with their content
// or they come with the build's precache.
self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET' || new URL(request.url).origin !== location.origin) return;
  const path = new URL(request.url).pathname;
  const networkFirst = request.mode === 'navigate' || path.endsWith('.js') || path.endsWith('.mjs');
  event.respondWith(networkFirst ? fromNetwork(request) : fromCache(request));
});

async function fromNetwork(request) {
  const cache = await caches.open(CACHE);
  try {
    const response = await fetch(request);
    if (response.ok) cache.put(request, response.clone());
    return response;
  } catch (e) {
    const cached = await cache.match(request, { ignoreSearch: true });
    if (cached) return cached;
    if (request.mode === 'navigate') {
      const index = await cache.match('index.html');
      if (index) return index;
    }
    throw e;
  }
}

async function fromCache(request) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(request, { ignoreSearch: true });
  if (cached) return cached;
  const response = await fetch(request);
  if (response.ok) cache.put(request, response.clone());
  return response;
}
