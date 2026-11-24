#!/usr/bin/env node
// Compares golden/ (kalenderbali.com) against golden2/ (independent sources) and prints every
// disagreement as markdown. Writes golden2/moon-diff.csv with every moon-phase offset != 0.
// Usage: node compare.mjs [goldenDir]
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const dir = process.argv[2] ?? path.resolve(here, '../../calendar-core/src/test/resources/golden');
const day = (iso) => Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) / 864e5;
const iso = (d) => new Date(d * 864e5).toISOString().slice(0, 10);

const site = [];
for (let y = 2024; y <= 2035; y++)
  for (const l of fs.readFileSync(path.join(dir, `days-${y}.csv`), 'utf8').trimEnd().split('\n').slice(1)) {
    const c = l.split(',');
    site.push({ date: c[0], flags: c[14] ? c[14].split(';') : [] });
  }
const byDate = new Map(site.map((r) => [r.date, r]));
const EVENT = {
  purnama: (f) => f === 'purnama',
  tilem: (f) => f === 'tilem',
  nyepi: (f) => /^hari raya nyepi/i.test(f),
  galungan: (f) => /^hari raya galungan/i.test(f),
  kuningan: (f) => /^hari raya kuningan/i.test(f),
  saraswati: (f) => /^hari raya saraswati/i.test(f) || /^saraswati/i.test(f),
  pagerwesi: (f) => /pagerwesi/i.test(f),
  siwaratri: (f) => /siwa\s?ratri/i.test(f),
  'ngembak-geni': (f) => /ngembak geni/i.test(f),
};
const datesOf = (ev) => site.filter((r) => r.flags.some(EVENT[ev])).map((r) => r.date);

const out = [];
const g2 = fs.readFileSync(path.join(dir, 'golden2/hari-raya.csv'), 'utf8').trimEnd().split('\n').slice(1).map((l) => { const [date, event, source] = l.split(','); return { date, event, source }; });

out.push('### Published holiday lists vs kalenderbali.com', '', '| event | published date | source | kalenderbali.com on that date | verdict |', '|---|---|---|---|---|');
let agree = 0, dis = 0;
for (const r of g2) {
  if (!EVENT[r.event]) continue;
  const s = byDate.get(r.date);
  const ok = s && s.flags.some(EVENT[r.event]);
  if (ok) { agree++; continue; }
  dis++;
  const near = datesOf(r.event).filter((d) => Math.abs(day(d) - day(r.date)) <= 40);
  out.push(`| ${r.event} | ${r.date} | ${r.source} | ${s ? s.flags.join('; ') || '(no flag)' : '(date outside range)'} | DISAGREE; site has ${r.event} on ${near.join(', ') || 'none within 40 days'} |`);
}
out.push('', `Agreements: ${agree}; disagreements: ${dis}.`, '');

// Moon phases (USNO astronomical instants, WITA date) vs Purnama / Tilem days.
const moon = fs.readFileSync(path.join(dir, 'golden2/moon-phases.csv'), 'utf8').trimEnd().split('\n').slice(1).map((l) => { const [phase, utc, wita] = l.split(','); return { phase, utc, wita }; });
const diffRows = ['site_event,site_date,usno_phase_utc,usno_wita_date,offset_days'];
const dist = { purnama: {}, tilem: {} };
const unmatched = [];
for (const [ev, ph] of [['purnama', 'full'], ['tilem', 'new']]) {
  const ms = moon.filter((m) => m.phase === ph);
  const sd = datesOf(ev);
  for (const d of sd) {
    let best = null;
    for (const m of ms) { const off = day(d) - day(m.wita); if (!best || Math.abs(off) < Math.abs(best.off)) best = { m, off }; }
    if (!best || Math.abs(best.off) > 3) { unmatched.push(`${ev} ${d}`); continue; }
    dist[ev][best.off] = (dist[ev][best.off] ?? 0) + 1;
    if (best.off !== 0) diffRows.push(`${ev},${d},${best.m.utc},${best.m.wita},${best.off}`);
  }
  // USNO phases with no site day within 3 days
  for (const m of ms) if (!sd.some((d) => Math.abs(day(d) - day(m.wita)) <= 3)) unmatched.push(`USNO ${ph} ${m.utc} has no site ${ev}`);
}
fs.writeFileSync(path.join(dir, 'golden2/moon-diff.csv'), diffRows.join('\n') + '\n');
out.push('### Purnama/Tilem vs USNO astronomical full/new moon (WITA date)', '',
  'Offset = site date minus WITA calendar date of the astronomical instant. Offset 0 means the same day; the Balinese rule is tithi-based so small offsets are expected. Every non-zero row is listed in `golden2/moon-diff.csv`.', '',
  `Purnama offsets: ${JSON.stringify(dist.purnama)}`, '', `Tilem offsets: ${JSON.stringify(dist.tilem)}`, '',
  `Unmatched (beyond 3 days): ${unmatched.length ? unmatched.join('; ') : 'none'}`, '');
console.log(out.join('\n'));
