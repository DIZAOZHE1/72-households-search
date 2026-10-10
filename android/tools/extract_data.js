// Extracts the 《外来媳妇本地郎》 catalogue from its single-file page into
// data/extracted/wailai.json, the intermediate consumed by build_asset.js.
//
// The page embeds the whole dataset as JavaScript literals:
//   const SECTION_LABELS = [ … ];
//   const DATA = [ {"section":1,"s":1,"e":1,"ep":"1","title":"…","raw":"…"}, … ];
//
// Usage:  node tools/extract_data.js [path-to-page.html]
const fs = require('fs');
const path = require('path');
const common = require('./lib/data_common');

const SOURCE_NAME = 'wailai_episodes.html';
const sourcePath = common.resolveSource(process.argv[2], SOURCE_NAME);
const html = fs.readFileSync(sourcePath, 'utf8');

const labelsLiteral = common.extractLiteral(html, 'SECTION_LABELS');
const dataLiteral = common.extractLiteral(html, 'DATA');
if (!labelsLiteral || !dataLiteral) {
  console.error('[extract_data] could not locate SECTION_LABELS / DATA in %s', sourcePath);
  process.exit(1);
}

const sections = JSON.parse(labelsLiteral);
const episodes = JSON.parse(dataLiteral);

const problems = [];
if (!episodes.length) problems.push('no episodes extracted');
if (!sections.length) problems.push('no section labels extracted');
episodes.forEach((d, i) => {
  if (typeof d.s !== 'number' || typeof d.e !== 'number') problems.push(`record ${i} has a non-numeric range`);
  if (d.e < d.s) problems.push(`record ${i} has an inverted range`);
  if (!d.title) problems.push(`record ${i} has no title`);
  if (!(d.section >= 1)) problems.push(`record ${i} has no section`);
});

const outPath = path.join(common.EXTRACTED_DIR, 'wailai.json');
const bytes = common.writeJson(outPath, { sections, episodes });

const perSection = {};
for (const row of episodes) perSection[row.section] = (perSection[row.section] || 0) + 1;
const span = episodes.reduce((sum, d) => sum + (d.e - d.s + 1), 0);
const hash = common.sha256(sourcePath);

console.log('[extract_data] source     = %s', sourcePath);
console.log('[extract_data] sha256     = %s', hash);
console.log('[extract_data] sections   = %d', sections.length);
console.log('[extract_data] records    = %d covering %d episodes', episodes.length, span);
console.log('[extract_data] per-section= %s', JSON.stringify(perSection));
console.log('[extract_data] written    = %s (%s KB)', outPath, (bytes / 1024).toFixed(1));

if (problems.length) {
  console.error('[extract_data] VALIDATION FAILED (%d):', problems.length);
  problems.slice(0, 20).forEach((p) => console.error('  - %s', p));
  process.exit(1);
}

common.writeProvenance({
  wailai: {
    source: path.basename(sourcePath),
    sha256: hash,
    records: episodes.length,
    sections: sections.length,
    episodeSpan: span,
  },
});
console.log('[extract_data] validation OK');
