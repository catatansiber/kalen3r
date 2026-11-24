#!/usr/bin/env node
// Golden data collector. Collects DATE FACTS only (wewaran names, wuku, tithi number/type,
// sasih, Caka year, purnama/tilem/hari raya names) from kalenderbali.com month pages.
// Descriptive text (wewukon, dewasa, ingkel, pararasan, ...) is never read into output.
//
// Usage: node collect.mjs [--from 2024] [--to 2035] [--month 2026-11] [--delay 10]
//                         [--out <dir>] [--cache <dir>]
// Requires Playwright (resolved via require, so NODE_PATH style installs work).
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const here = path.dirname(fileURLToPath(import.meta.url));
const args = Object.fromEntries(
  process.argv.slice(2).reduce((acc, a, i, arr) => {
    if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1]]);
    return acc;
  }, []),
);
const FROM = Number(args.from ?? 2024);
const TO = Number(args.to ?? 2035);
// robots.txt of the site declares Crawl-delay: 10 -> default 10 s, never below 2 s.
const DELAY_MS = Math.max(2, Number(args.delay ?? 10)) * 1000;
const OUT = args.out ?? path.resolve(here, '../../calendar-core/src/test/resources/golden');
const CACHE = args.cache ?? null; // optional dir with one raw JSON per month (resume)
const HEADER = 'date,wuku,saptawara,pancawara,triwara,caturwara,astawara,sangawara,dasawara,urip,tithiType,tithiNo,sasih,caka,flags';

const MONTHS = { jan: 1, peb: 2, feb: 2, mar: 3, apr: 4, mei: 5, jun: 6, jul: 7, agu: 8, sep: 9, okt: 10, nov: 11, des: 12 };
const WUKU = ['sinta','landep','ukir','kulantir','tolu','gumbreg','wariga','warigadean','julungwangi','sungsang','dungulan','kuningan','langkir','medangsia','pujut','pahang','krulut','merakih','tambir','medangkungan','matal','uye','menail','prangbakat','bala','ugu','wayang','kulawu','dukut','watugunung'];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const pad = (n) => String(n).padStart(2, '0');

async function loadMonth(page, y, m) {
  const url = `https://kalenderbali.com/?bl=${m}&th=${y}`;
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
  // The site may show a short "One moment, please..." interstitial that proceeds by itself.
  // We only wait passively for it. We never interact with or try to bypass a challenge.
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    try {
      if (await page.locator('a.kbd-day').count()) break;
      const body = (await page.evaluate(() => document.body?.innerText ?? '')).slice(0, 400);
      if (/captcha|are you (a )?human|checkbox|cloudflare/i.test(body) || (await page.locator('iframe[src*="captcha" i],iframe[src*="challenge" i]').count())) {
        throw new Error('BLOCKED: interactive bot-verification screen at ' + url);
      }
    } catch (e) {
      if (String(e.message).startsWith('BLOCKED')) throw e;
      // navigation in progress: just wait
    }
    await sleep(1500);
  }
  if (!(await page.locator('a.kbd-day').count())) throw new Error('No calendar grid (verification never cleared?) at ' + url);
  return page.evaluate(() => {
    const header = document.querySelector('.judulkalenderright')?.textContent ?? '';
    const heads = [...document.querySelectorAll('#tabelkalender > tbody > tr:first-child > td')].map(
      (td) => td.querySelector('.wukured')?.textContent.trim() ?? '',
    );
    const cells = [...document.querySelectorAll('a.kbd-day')].map((a) => {
      const d = JSON.parse(a.dataset.detail);
      const outer = a.closest('table').closest('td');
      const tithiTitle = [...outer.querySelectorAll('[title^="Penanggal:"],[title^="Pangelong:"]')].map((e) => e.getAttribute('title'));
      return {
        tgl: a.dataset.tgl,
        col: outer.cellIndex,
        // whitelist of date facts only
        sapta: d['Sapta Wara'], panca: d['Panca Wara'], tri: d['Tri Wara'], catur: d['Catur Wara'],
        asta: d['Asta Wara'], sanga: d['Sanga Wara'], dasa: d['Dasa Wara'], urip: d['Urip (Sapta+Panca)'],
        tithi: d['Penanggal/Pangelong'], sasih: d['Sasih'], rerainan: d['Rerainan'] ?? '', tithiTitle,
      };
    });
    return { header, heads, cells };
  });
}

function parseFlags(rerainan) {
  const flags = [];
  for (let part of String(rerainan).split(/,\s*/)) {
    part = part.replace(/\s*\([^)]*\)/g, '').trim();
    if (!part) continue;
    if (/^purnama\b/i.test(part)) flags.push('purnama');
    else if (/^tilem\b/i.test(part)) flags.push('tilem');
    else if (/^kajeng kel?iwon$/i.test(part)) flags.push('kajeng-kliwon');
    else if (/^anggara kasih\b/i.test(part)) flags.push('anggara-kasih');
    else {
      // drop the default "<saptawara> <pancawara> <wuku>" filler the site prints on ordinary days
      const w = part.toLowerCase().split(/\s+/);
      const isFiller = w.length === 3 && WUKU.includes(w[2].replace(/[^a-z]/g, '')) &&
        ['redite','soma','anggara','buda','wraspati','sukra','saniscara'].includes(w[0]);
      if (!isFiller) flags.push(part);
    }
  }
  return [...new Set(flags)];
}

function toRows(y, m, raw) {
  const nums = (raw.header.match(/\d{4}/g) ?? []).map(Number);
  const wukuByCol = raw.heads.map((h) => (h.match(/^\s*(\d+)\.\s*(.+)$/) ? { no: Number(RegExp.$1), name: RegExp.$2.trim() } : null));
  const rows = [];
  for (const c of raw.cells) {
    const mm = c.tgl.match(/^(\d+)\s+(\S+)\s+(\d{4})$/);
    if (!mm) throw new Error('bad tgl ' + c.tgl);
    const mon = MONTHS[mm[2].slice(0, 3).toLowerCase()];
    if (mon !== m || Number(mm[3]) !== y) continue; // neighbouring-month filler cell
    const wk = wukuByCol[c.col];
    if (!wk) throw new Error('no wuku column for ' + c.tgl);
    const t = c.tithiTitle.length ? c.tithiTitle[0].match(/^(Penanggal|Pangelong):\s*(\d+)/) : null;
    const flags = parseFlags(c.rerainan);
    rows.push({
      date: `${y}-${pad(m)}-${pad(Number(mm[1]))}`,
      wuku: `${wk.no}.${wk.name.toLowerCase()}`,
      sapta: c.sapta, panca: c.panca, tri: c.tri, catur: c.catur, asta: c.asta, sanga: c.sanga, dasa: c.dasa,
      urip: String(c.urip ?? '').replace(/^Urip\s*=\s*/, '').replace(/\s+/g, ''),
      tithiType: t ? t[1].toLowerCase() : '',
      tithiNo: t ? t[2] : String(c.tithi ?? ''),
      sasih: c.sasih ?? '',
      flags,
    });
  }
  // Caka year: the page header prints the Caka year valid at the start of the month. The year
  // changes on Nyepi day (Caka new year), whose rerainan text names the new year ("... Saka 1946").
  const nyepi = rows.find((r) => r.flags.some((f) => /^(hari raya )?nyepi\b/i.test(f)));
  for (const r of rows) {
    if (!nums.length) { r.caka = ''; continue; } // not on the page: left empty on purpose
    if (nyepi && r.date >= nyepi.date) {
      const named = nyepi.flags.join(' ').match(/Saka\s+(\d{4})/i);
      r.caka = String(named ? Number(named[1]) : nums[nums.length - 1] + (nums.length === 1 ? 1 : 0));
    } else r.caka = String(nums[0]);
  }
  return { rows, nums };
}

const csvLine = (r) => [r.date, r.wuku, r.sapta, r.panca, r.tri, r.catur, r.asta, r.sanga, r.dasa, r.urip, r.tithiType, r.tithiNo, r.sasih, r.caka, r.flags.join(';')].join(',');

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  if (CACHE) fs.mkdirSync(CACHE, { recursive: true });
  const targets = [];
  if (args.month) { const [y, m] = args.month.split('-').map(Number); targets.push([y, m]); }
  else for (let y = FROM; y <= TO; y++) for (let m = 1; m <= 12; m++) targets.push([y, m]);

  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const byYear = new Map();
  let first = true;
  try {
    for (const [y, m] of targets) {
      const cf = CACHE && path.join(CACHE, `${y}-${pad(m)}.json`);
      let raw;
      if (cf && fs.existsSync(cf)) raw = JSON.parse(fs.readFileSync(cf, 'utf8'));
      else {
        if (!first) await sleep(DELAY_MS); // one request at a time, polite delay
        first = false;
        // If the site's verification interstitial does not clear by itself, loadMonth throws and the
        // run stops (resume later with --cache). Challenges are never bypassed or retried around.
        raw = await loadMonth(page, y, m);
        if (cf) fs.writeFileSync(cf, JSON.stringify(raw));
      }
      const { rows, nums } = toRows(y, m, raw);
      console.error(`${y}-${pad(m)}: ${rows.length} days, caka header ${JSON.stringify(nums)}`);
      if (!byYear.has(y)) byYear.set(y, []);
      byYear.get(y).push(...rows);
    }
  } finally {
    await browser.close();
  }
  for (const [y, rows] of byYear) {
    const full = !args.month;
    const file = path.join(OUT, args.month ? `days-${args.month}.partial.csv` : `days-${y}.csv`);
    fs.writeFileSync(file, [HEADER, ...rows.sort((a, b) => a.date.localeCompare(b.date)).map(csvLine)].join('\n') + '\n');
    console.error('wrote', file, full ? '' : '(partial)');
  }
}

main().catch((e) => { console.error(e.message); process.exit(1); });
