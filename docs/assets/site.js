/* SpaceReloaded: тема, подсказки, фильтр рецептов, выбор слоя мультиблока, активный пункт оглавления.
   Без фреймворков; страница полностью читается и без JS. */
(function () {
  'use strict';
  var root = document.documentElement;

  /* ---- тема ---- */
  function currentTheme() {
    return root.dataset.theme ||
      (window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  }
  function syncThemeBtn(btn) {
    var dark = currentTheme() === 'dark';
    btn.setAttribute('aria-pressed', String(dark));
    btn.setAttribute('aria-label', dark ? 'Включить светлую тему' : 'Включить тёмную тему');
  }
  var themeBtn = document.querySelector('.theme');
  if (themeBtn) {
    syncThemeBtn(themeBtn);
    themeBtn.addEventListener('click', function () {
      var next = currentTheme() === 'dark' ? 'light' : 'dark';
      root.dataset.theme = next;
      try { localStorage.setItem('sr-theme', next); } catch (e) { /* приватный режим */ }
      syncThemeBtn(themeBtn);
    });
  }

  /* ---- подсказки к иконкам ---- */
  var tip = document.querySelector('.tip');
  var tipOwner = null;
  function showTip(el) {
    if (!tip) return;
    tipOwner = el;
    tip.textContent = el.getAttribute('data-tip');
    tip.hidden = false;
    var r = el.getBoundingClientRect();
    var tw = tip.offsetWidth, th = tip.offsetHeight;
    var x = Math.min(Math.max(8, r.left + r.width / 2 - tw / 2), window.innerWidth - tw - 8);
    var y = r.top - th - 8;
    if (y < 64) y = r.bottom + 8;
    tip.style.left = x + 'px';
    tip.style.top = y + 'px';
  }
  function hideTip() { if (tip) { tip.hidden = true; tipOwner = null; } }
  document.addEventListener('pointerover', function (e) {
    var el = e.target.closest && e.target.closest('[data-tip]');
    if (el && e.pointerType !== 'touch') showTip(el);
  });
  document.addEventListener('pointerout', function (e) {
    var el = e.target.closest && e.target.closest('[data-tip]');
    if (el && (!e.relatedTarget || !el.contains(e.relatedTarget))) hideTip();
  });
  document.addEventListener('click', function (e) {  // касание на телефоне
    var el = e.target.closest && e.target.closest('[data-tip]');
    if (el) { if (tipOwner === el) hideTip(); else showTip(el); } else hideTip();
  });
  document.addEventListener('focusin', function (e) {
    if (e.target.matches && e.target.matches('[data-tip]')) showTip(e.target);
  });
  document.addEventListener('focusout', hideTip);
  window.addEventListener('scroll', hideTip, { passive: true });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') hideTip(); });

  /* ---- фильтр рецептов ---- */
  var q = document.getElementById('q');
  if (q) {
    var cards = Array.prototype.slice.call(document.querySelectorAll('#recipes .card'));
    var phases = Array.prototype.slice.call(document.querySelectorAll('#recipes .phase'));
    var chips = Array.prototype.slice.call(document.querySelectorAll('.chip'));
    var shown = document.getElementById('shown');
    var empty = document.getElementById('empty');
    var kind = 'all';
    var norm = function (s) { return s.toLowerCase().replace(/ё/g, 'е').trim(); };
    cards.forEach(function (c) { c._s = norm(c.getAttribute('data-search')); });
    var apply = function () {
      var words = norm(q.value).split(/\s+/).filter(Boolean);
      var n = 0;
      cards.forEach(function (c) {
        var ok = (kind === 'all' || c.getAttribute('data-kind') === kind) &&
          words.every(function (w) { return c._s.indexOf(w) !== -1; });
        c.hidden = !ok;
        if (ok) n++;
      });
      phases.forEach(function (p) { p.hidden = !p.querySelector('.card:not([hidden])'); });
      shown.textContent = n;
      empty.hidden = n > 0;
      try { history.replaceState(null, '', q.value || kind !== 'all' ? '#q=' + encodeURIComponent(q.value) + '&k=' + kind : location.pathname); } catch (e) { }
    };
    chips.forEach(function (ch) {
      ch.addEventListener('click', function () {
        kind = ch.getAttribute('data-kind');
        chips.forEach(function (o) { o.setAttribute('aria-pressed', String(o === ch)); });
        apply();
      });
    });
    q.addEventListener('input', apply);
    var m = /^#q=([^&]*)&k=(\w+)/.exec(location.hash);
    if (m) {
      q.value = decodeURIComponent(m[1]);
      kind = m[2];
      chips.forEach(function (o) { o.setAttribute('aria-pressed', String(o.getAttribute('data-kind') === kind)); });
      apply();
    }
    document.addEventListener('keydown', function (e) {
      if (e.key === '/' && document.activeElement !== q && !/INPUT|TEXTAREA/.test(document.activeElement.tagName)) {
        e.preventDefault(); q.focus();
      }
    });
  }

  /* ---- слои мультиблоков ---- */
  Array.prototype.forEach.call(document.querySelectorAll('[data-layers]'), function (box) {
    var btns = Array.prototype.slice.call(box.querySelectorAll('.lbtn[data-layer]'));
    var panes = Array.prototype.slice.call(box.querySelectorAll('.layer'));
    var cur = 0;
    var show = function (i) {
      cur = Math.max(0, Math.min(panes.length - 1, i));
      panes.forEach(function (p, k) { if (k === cur) p.removeAttribute('data-off'); else p.setAttribute('data-off', ''); });
      btns.forEach(function (b, k) { b.setAttribute('aria-pressed', String(k === cur)); });
    };
    btns.forEach(function (b, k) { b.addEventListener('click', function () { show(k); }); });
    Array.prototype.forEach.call(box.querySelectorAll('.lbtn[data-step]'), function (b) {
      b.addEventListener('click', function () { show(cur + Number(b.getAttribute('data-step'))); });
    });
    box.addEventListener('keydown', function (e) {
      if (e.key === 'ArrowUp' || e.key === 'PageUp') { e.preventDefault(); show(cur + 1); }
      if (e.key === 'ArrowDown' || e.key === 'PageDown') { e.preventDefault(); show(cur - 1); }
    });
  });

  /* ---- оглавление свёрнуто на узком экране ---- */
  var tocDetails = document.querySelector('.toc details');
  if (tocDetails && window.matchMedia && !matchMedia('(min-width: 1000px)').matches) tocDetails.open = false;

  /* ---- активный пункт оглавления ---- */
  var tocLinks = document.querySelectorAll('.toc a[href^="#"]');
  if (tocLinks.length && 'IntersectionObserver' in window) {
    var map = {};
    Array.prototype.forEach.call(tocLinks, function (a) { map[a.getAttribute('href').slice(1)] = a; });
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          Array.prototype.forEach.call(tocLinks, function (a) { a.classList.remove('active'); });
          var a = map[en.target.id];
          if (a) a.classList.add('active');
        }
      });
    }, { rootMargin: '-20% 0px -70% 0px' });
    Object.keys(map).forEach(function (id) { var s = document.getElementById(id); if (s) io.observe(s); });
  }
})();
