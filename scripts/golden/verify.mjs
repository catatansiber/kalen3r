#!/usr/bin/env node
// Sanity checks over golden days-<year>.csv, only within the ranges declared in coverage.json. Exit code 1 on any hard failure.
// Usage: node verify.mjs [goldenDir]
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const dir = process.argv[2] ?? path.resolve(here, '../../calendar-core/src/test/resources/golden');
const HEADER = 'date,wuku,saptawara,pancawara,triwara,caturwara,astawara,sangawara,dasawara,urip,tithiType,tithiNo,sasih,caka,flags';
const SAPTA = ['Redite', 'Soma', 'Anggara', 'Buda', 'Wraspati', 'Sukra', 'Saniscara'];
const SAPTA_URIP = [5, 4, 3, 7, 8, 6, 9];
const PANCA = ['Paing', 'Pon', 'Wage', 'Keliwon', 'Umanis']; // site spelling; "Kliwon" accepted too
const PANCA_URIP = [9, 7, 4, 8, 5];
const TRI = ['Pasah', 'Beteng', 'Kajeng'];
const WUKU = ['sinta','landep','ukir','kulantir','tolu','gumbreg','wariga','warigadean','julungwangi','sungsang','dunggulan','kuningan','langkir','medangsia','pujut','pahang','krulut','merakih','tambir','medangkungan','matal','uye','menail','prangbakat','bala','ugu','wayang','kulawu','dukut','watugunung'];
const norm = (s) => (s === 'Kliwon' ? 'Keliwon' : s);

let failures = 0;
const fail = (m) => { if (failures++ < 40) console.error('FAIL', m); };
const day = (iso) => Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) / 864e5;
const iso = (d) => new Date(d * 864e5).toISOString().slice(0, 10);
const ANCHOR = day('2026-11-01');

const coverage = JSON.parse(fs.readFileSync(path.join(dir, 'coverage.json'), 'utf8'));
const rows = [];
for (const f of fs.readdirSync(dir).filter((n) => /^days-\d{4}\.csv$/.test(n)).sort()) {
  const lines = fs.readFileSync(path.join(dir, f), 'utf8').trimEnd().split('\n');
  if (lines[0] !== HEADER) fail(`${f}: bad header`);
  for (const l of lines.slice(1)) {
    const c = l.split(',');
    if (c.length !== 15) { fail(`${f}: ${c.length} columns in ${l}`); continue; }
    const [date, wuku, sapta, panca, tri, catur, asta, sanga, dasa, urip, tType, tNo, sasih, caka, flags] = c;
    rows.push({ date, wuku, sapta, panca: norm(panca), tri, catur, asta, sanga, dasa, urip, tType, tNo, sasih, caka, flags: flags ? flags.split(';') : [] });
  }
}
rows.sort((x, y) => x.date.localeCompare(y.date));

// 1. rows must cover exactly the declared ranges: no gaps, no duplicates, nothing outside
const expectedDates = [];
for (const rg of coverage) for (let d = day(rg.from); d <= day(rg.to); d++) expectedDates.push(iso(d));
if (rows.length !== expectedDates.length) fail(`row count ${rows.length} != declared coverage ${expectedDates.length}`);
rows.forEach((r, i) => { if (r.date !== expectedDates[i]) fail(`row ${i}: ${r.date} expected ${expectedDates[i]} (gap, duplicate or undeclared date)`); });
// start index of each covered range, so "consecutive day" checks do not span a gap
const rangeStart = new Set(coverage.map((rg) => rg.from));

for (let i = 0; i < rows.length; i++) {
  const r = rows[i];
  const d = day(r.date);
  const idx = (((d - ANCHOR) % 210) + 210) % 210;
  const wn = Number(r.wuku.split('.')[0]), wname = r.wuku.split('.')[1];
  // 2. pawukon indices consistent with a 210-day cycle anchored 2026-11-01 (Sinta, Redite, Paing, Pasah)
  if (wn !== Math.floor(idx / 7) + 1) fail(`${r.date} wuku ${r.wuku} != ${Math.floor(idx / 7) + 1}`);
  if (WUKU[wn - 1] !== wname) fail(`${r.date} wuku name/no mismatch ${r.wuku}`);
  if (r.sapta !== SAPTA[idx % 7]) fail(`${r.date} sapta ${r.sapta} != ${SAPTA[idx % 7]}`);
  if (r.panca !== PANCA[idx % 5]) fail(`${r.date} panca ${r.panca} != ${PANCA[idx % 5]}`);
  if (r.tri !== TRI[idx % 3]) fail(`${r.date} tri ${r.tri} != ${TRI[idx % 3]}`);
  const exp = `${SAPTA_URIP[SAPTA.indexOf(r.sapta)]}+${PANCA_URIP[PANCA.indexOf(r.panca)]}`;
  if (r.urip !== exp) fail(`${r.date} urip ${r.urip} != ${exp}`);
  for (const k of ['catur', 'asta', 'sanga', 'dasa', 'sasih', 'caka', 'tType', 'tNo']) if (!r[k]) fail(`${r.date} empty ${k}`);
  if (!['penanggal', 'pangelong'].includes(r.tType)) fail(`${r.date} tithiType ${r.tType}`);
  // 3. consecutive days: sapta, panca, wuku step
  if (i > 0 && !rangeStart.has(r.date)) {
    const p = rows[i - 1];
    if (SAPTA.indexOf(r.sapta) !== (SAPTA.indexOf(p.sapta) + 1) % 7) fail(`${r.date} saptawara not consecutive`);
    if (PANCA.indexOf(r.panca) !== (PANCA.indexOf(p.panca) + 1) % 5) fail(`${r.date} pancawara not consecutive`);
    const pw = Number(p.wuku.split('.')[0]);
    const expW = r.sapta === 'Redite' ? (pw % 30) + 1 : pw; // wuku advances every 7 days, on Redite
    if (wn !== expW) fail(`${r.date} wuku ${wn} expected ${expW}`);
    if (Number(r.caka) < Number(p.caka) || Number(r.caka) - Number(p.caka) > 1) fail(`${r.date} caka ${p.caka}->${r.caka}`);
    const nyepi = r.flags.some((f) => /^hari raya nyepi/i.test(f));
    if ((Number(r.caka) - Number(p.caka) === 1) !== nyepi) fail(`${r.date} caka change must coincide with Nyepi (${p.caka}->${r.caka}, nyepi=${nyepi})`);
  }
}
// 4. named anchors
const get = (dt) => rows.find((r) => r.date === dt);
const a = get('2026-11-01');
if (a && ( a.wuku !== '1.sinta' || a.sapta !== 'Redite' || a.panca !== 'Paing' || a.tri !== 'Pasah' || a.urip !== '5+9')) fail('anchor 2026-11-01 wrong: ' + JSON.stringify(a));
const b = get('2026-11-04');
if (b && b.panca !== 'Keliwon') fail('anchor 2026-11-04 not Kliwon');
if (a && a.tType !== 'pangelong') fail('2026-11-01 expected pangelong');

// 5. tithi/moon flags (hard): purnama == penanggal 15, tilem == pangelong 15
for (const r of rows) {
  const p = r.flags.includes('purnama'), t = r.flags.includes('tilem');
  if (p && !(r.tType === 'penanggal' && /(^|\/)15$/.test(r.tNo))) fail(`${r.date} purnama flag but ${r.tType} ${r.tNo}`);
  if (t && !(r.tType === 'pangelong' && /(^|\/)15$/.test(r.tNo))) fail(`${r.date} tilem flag but ${r.tType} ${r.tNo}`);
}
const nP = rows.filter((r) => r.flags.includes('purnama')).length, nT = rows.filter((r) => r.flags.includes('tilem')).length;
console.log(`rows=${rows.length} purnama=${nP} tilem=${nT} (expected about ${Math.round(rows.length / 29.53)} each)`);
if (failures) { console.error(`${failures} failure(s)`); process.exit(1); }
console.log('OK: golden data passes all checks');
