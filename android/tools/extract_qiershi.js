// Extracts the 《七十二家房客》 dataset out of a committed v2.20 page into
// data/extracted/qiershi.json, the intermediate consumed by build_asset.js, applying the same
// normalisations the web page performs at runtime:
//
//   1. Character ids in eps[].c use the legacy ordering and are remapped through
//      LEGACY_CHAR_TO_CURRENT, exactly as `E.forEach(d => d.c = d.c.map(...))` does.
//   2. Each main-line node's episode index list is precomputed from its [season, from, to] ranges,
//      which the page builds in the MK/nd.eps pass. Rendering it at runtime would need the whole
//      episode list in memory on every screen.
//
// Usage: node tools/extract_qiershi.js [source.html]
const fs = require('fs');
const path = require('path');
const common = require('./lib/data_common');

const SOURCE_NAME = 'qiershi_traditional_v2.20.html';
const sourcePath = common.resolveSource(process.argv[2], SOURCE_NAME);
const html = fs.readFileSync(sourcePath, 'utf8');

const literal = common.extractLiteral(html, 'D');
if (!literal) {
  console.error('[extract_qiershi] could not locate const D in %s', sourcePath);
  process.exit(1);
}
const D = JSON.parse(literal);

const legacyLiteral = common.extractLiteral(html, 'LEGACY_CHAR_TO_CURRENT');
if (!legacyLiteral) {
  console.error('[extract_qiershi] could not locate LEGACY_CHAR_TO_CURRENT');
  process.exit(1);
}
const LEGACY = JSON.parse(legacyLiteral);

const rawEps = D.eps || [];
const chars = D.chars || [];
const tags = D.tags || [];
const main = D.main || [];
const seasonTitles = D.st || {};

// 1. Remap legacy character ids.
const eps = rawEps.map((d) => ({
  se: d.se,
  s: d.s,
  e: d.e,
  t: d.t,
  y: d.y || '',
  c: (d.c || []).map((oldId) => (LEGACY[oldId] !== undefined ? LEGACY[oldId] : oldId)),
  g: d.g || [],
}));

// 2. Precompute the episode indices for every main-line node, and the per-episode main-line marks.
const mainOut = main.map((line) => ({
  sec: line.sec,
  n: line.n,
  d: line.d || '',
  nodes: (line.nodes || []).map((nd) => {
    const hits = [];
    for (const range of nd.r || []) {
      const [se, from, to] = range;
      if (!from) continue;
      eps.forEach((d, i) => {
        if (d.se === se && d.s <= to && d.e >= from) hits.push(i);
      });
    }
    return {
      g: nd.g || '',
      x: nd.x || '',
      r: nd.r || [],
      eps: [...new Set(hits)].sort((a, b) => a - b),
    };
  }),
}));

const marks = eps.map(() => []);
mainOut.forEach((line, li) => {
  line.nodes.forEach((nd, ni) => {
    nd.eps.forEach((i) => {
      if (!marks[i].some((m) => m.l === li)) marks[i].push({ l: li, n: ni });
    });
  });
});

// 3. Validate before writing, so a bad source fails loudly instead of shipping a broken asset.
const charCount = chars.map((_, ci) => eps.filter((d) => d.c.includes(ci)).length);
const problems = [];
if (!eps.length) problems.push('no episodes');
if (!chars.length) problems.push('no characters');
eps.forEach((d, i) => {
  if (!d.t) problems.push(`episode ${i} has no title`);
  if (d.e < d.s) problems.push(`episode ${i} has an inverted range`);
  for (const c of d.c) if (c < 0 || c >= chars.length) problems.push(`episode ${i} references character ${c}`);
  for (const g of d.g) if (g < 0 || g >= tags.length) problems.push(`episode ${i} references tag ${g}`);
});
mainOut.forEach((line, li) => {
  if (!line.nodes.length) problems.push(`main line ${li} has no nodes`);
});

const payload = {
  episodes: eps,
  characters: chars,
  tags,
  seasonTitles,
  main: mainOut,
  marks,
};

const outPath = path.join(common.EXTRACTED_DIR, 'qiershi.json');
common.writeJson(outPath, payload);

const totalEpisodes = eps.reduce((sum, d) => sum + (d.e - d.s + 1), 0);
const perSeason = {};
eps.forEach((d) => { perSeason[d.se] = (perSeason[d.se] || 0) + 1; });
const hash = common.sha256(sourcePath);

console.log('[extract_qiershi] source        = %s', sourcePath);
console.log('[extract_qiershi] sha256        = %s', hash);
console.log('[extract_qiershi] story cards   = %d covering %d episodes', eps.length, totalEpisodes);
console.log('[extract_qiershi] seasons       = %d  %s', Object.keys(perSeason).length, JSON.stringify(perSeason));
console.log('[extract_qiershi] characters    = %d', chars.length);
console.log('[extract_qiershi] tags          = %d  %s', tags.length, JSON.stringify(tags));
console.log('[extract_qiershi] main lines    = %d with %d nodes',
  mainOut.length, mainOut.reduce((n, l) => n + l.nodes.length, 0));
console.log('[extract_qiershi] episodes w/主线 = %d', marks.filter((m) => m.length).length);
console.log('[extract_qiershi] character casts=%d..%d', Math.min(...charCount), Math.max(...charCount));
console.log('[extract_qiershi] 大杂院 in data  = %d (expected 0)', literal.split('大杂院').length - 1);
console.log('[extract_qiershi] written       = %s (%s KB)', outPath, (fs.statSync(outPath).size / 1024).toFixed(1));

if (problems.length) {
  console.error('[extract_qiershi] VALIDATION FAILED (%d):', problems.length);
  problems.slice(0, 20).forEach((p) => console.error('  - %s', p));
  process.exit(1);
}

common.writeProvenance({
  qiershi: {
    source: path.basename(sourcePath),
    sha256: hash,
    storyCards: eps.length,
    episodeSpan: totalEpisodes,
    seasons: Object.keys(perSeason).length,
    characters: chars.length,
    tags: tags.length,
    storylines: mainOut.length,
  },
});
console.log('[extract_qiershi] validation OK');
