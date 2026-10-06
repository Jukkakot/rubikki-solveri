// The scan worker's browser calls (the page side is in the web module's platform.mjs). A picture
// arrives as an ImageBitmap (or, where the page cannot make one, as RGBA bytes); its pixels go to
// Kotlin, whose answer (the faces as numbers) goes back to the page.

let canvas = null;
let ctx = null;

function pixels(msg) {
  if (msg.bitmap) {
    const b = msg.bitmap;
    if (!canvas || canvas.width !== b.width || canvas.height !== b.height) {
      canvas = new OffscreenCanvas(b.width, b.height);
      ctx = canvas.getContext('2d', { willReadFrequently: true });
    }
    ctx.drawImage(b, 0, 0);
    b.close();
    return { width: canvas.width, height: canvas.height, data: ctx.getImageData(0, 0, canvas.width, canvas.height).data };
  }
  return { width: msg.width, height: msg.height, data: new Uint8ClampedArray(msg.data) };
}

/** [find] gets each picture (width, height, RGBA bytes) and returns the faces as numbers. */
export function workerListen(find) {
  self.onmessage = (e) => {
    try {
      const p = pixels(e.data);
      self.postMessage({ faces: find(p.width, p.height, new Int8Array(p.data.buffer, p.data.byteOffset, p.data.length)) });
    } catch (err) {
      self.postMessage({ error: (err && (err.name + ': ' + err.message)) || String(err) });
    }
  };
  self.postMessage({ ready: true });
}

export function now() {
  return performance.now();
}
