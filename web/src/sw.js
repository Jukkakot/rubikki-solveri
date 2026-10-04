// Placeholder until the offline task: a pass-through service worker. __VERSION__ is filled in by
// the build (generateBuildInfo).
const VERSION = '__VERSION__';
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));
