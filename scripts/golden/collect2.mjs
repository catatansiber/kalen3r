#!/usr/bin/env node
// Second-source collector: official US Naval Observatory (USNO) moon phase API.
// Writes golden2/moon-phases.csv with full/new moon instants (UTC) and the WITA (UTC+8) calendar date.
// Usage: node collect2.mjs [--from 2024] [--to 2035] [--out <golden2 dir>]
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const argv = process.argv.slice(2);
const arg = (k, d) => { const i = argv.indexOf('--' + k); return i >= 0 ? argv[i + 1] : d; };
const FROM = Number(arg('from', 2024)), TO = Number(arg('to', 2035));
const OUT = arg('out', path.resolve(here, '../../calendar-core/src/test/resources/golden/golden2'));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const p2 = (n) => String(n).padStart(2, '0');

const lines = ['phase,utc,wita_date'];
// fetch one year before/after so WITA date shifts at year edges are covered
for (let y = FROM - 1; y <= TO + 1; y++) {
  const res = await fetch(`https://aa.usno.navy.mil/api/moon/phases/year?year=${y}`);
  if (!res.ok) throw new Error('USNO HTTP ' + res.status + ' for ' + y);
  const j = await res.json();
  for (const e of j.phasedata) {
    if (e.phase !== 'Full Moon' && e.phase !== 'New Moon') continue;
    const utc = new Date(Date.UTC(e.year, e.month - 1, e.day, ...e.time.split(':').map(Number)));
    const wita = new Date(utc.getTime() + 8 * 3600e3);
    const wd = `${wita.getUTCFullYear()}-${p2(wita.getUTCMonth() + 1)}-${p2(wita.getUTCDate())}`;
    if (Number(wd.slice(0, 4)) < FROM || Number(wd.slice(0, 4)) > TO) continue;
    lines.push(`${e.phase === 'Full Moon' ? 'full' : 'new'},${utc.toISOString().slice(0, 16)}Z,${wd}`);
  }
  await sleep(2500);
}
fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, 'moon-phases.csv'), lines.join('\n') + '\n');
console.error('wrote', lines.length - 1, 'rows');
