// storage.js - APK/浏览器双模式存档层
window.Storage = (() => {
  const cache = {};
  let serverReady = false;
  let nativeReady = !!(window.GameNative && typeof GameNative.getItem === 'function');

  function preloadNative() {
    if (!nativeReady) return false;
    try {
      const raw = GameNative.preload();
      if (raw) Object.assign(cache, JSON.parse(raw));
      return true;
    } catch(e) { console.warn('[Storage] native preload failed', e); return false; }
  }

  const _readyPromise = new Promise((resolve) => {
    if (preloadNative()) { serverReady = true; resolve(true); return; }
    if (window.__preloadPromise) {
      window.__preloadPromise.then(data => {
        if (data && typeof data === 'object') { Object.assign(cache, data); serverReady = true; }
        resolve(serverReady);
      }).catch(() => resolve(false));
    } else {
      resolve(false);
    }
  });

  function syncLoadFromServer(key) {
    try {
      if (nativeReady) {
        const v = GameNative.getItem(key);
        if (v !== null && v !== undefined && v !== '') { cache[key] = v; return v; }
        return null;
      }
      if (window.location.protocol === 'file:') {
        const v = localStorage.getItem(key);
        if (v !== null) cache[key] = v;
        return v;
      }
      const xhr = new XMLHttpRequest();
      xhr.open('GET', '/api/load?key=' + encodeURIComponent(key), false);
      xhr.timeout = 3000;
      xhr.send();
      if (xhr.status === 200) { cache[key] = xhr.responseText; return xhr.responseText; }
    } catch (e) {}
    return null;
  }

  function get(key) {
    if (key in cache) return cache[key];
    return syncLoadFromServer(key);
  }

  function set(key, value) {
    cache[key] = value;
    try {
      if (nativeReady) { GameNative.setItem(key, value); return; }
      if (window.location.protocol === 'file:') { localStorage.setItem(key, value); return; }
      fetch('/api/save?key=' + encodeURIComponent(key), { method: 'POST', headers: { 'Content-Type': 'text/plain; charset=utf-8' }, body: value }).catch(() => {});
    } catch(e) {}
  }

  function remove(key) {
    delete cache[key];
    try {
      if (nativeReady) { GameNative.removeItem(key); return; }
      if (window.location.protocol === 'file:') { localStorage.removeItem(key); return; }
      fetch('/api/delete?key=' + encodeURIComponent(key), { method: 'POST' }).catch(() => {});
    } catch(e) {}
  }

  function ready() { return _readyPromise; }
  return { get, set, remove, ready };
})();
