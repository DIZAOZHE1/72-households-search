// Compiles every extracted dataset into the compact assets shipped inside the APK.
//
// Sources live in data/extracted/<dataset>.json. Two shapes are supported:
//
//   《外来媳妇本地郎》 (tools/extract_data.js)
//     { "sections": ["第十二部：…", …],
//       "episodes": [{ section, s, e, ep, title, raw }, …] }
//
//   《七十二家房客》 (tools/extract_qiershi.js)
//     { episodes: [{ se, s, e, t, y, c[], g[] }], characters: [...], tags: [...],
//       seasonTitles: {…}, main: [...], marks: [[{l,n}]] }
//
// Outputs go to app/src/main/assets/. Every file is tab separated, one record per line, and no
// field ever contains a tab or newline, so the app parses a record with a single split:
//
//   <dataset>.tsv              section <TAB> first <TAB> last <TAB> label <TAB> title <TAB> raw
//   <dataset>.sections.txt     one section heading per line
//   <dataset>.detail.tsv       index <TAB> synopsis <TAB> chars <TAB> tags <TAB> mainlines
//   <dataset>.chars.tsv        index <TAB> name <TAB> category <TAB> aliases <TAB> description
//   <dataset>.tags.txt         one tag per line
//   <dataset>.lines.tsv        lineIndex <TAB> section <TAB> name <TAB> summary <TAB> nodes
//                              where nodes = nodeCount then, per node, group <US> text <US> eps
const fs = require('fs');
const path = require('path');
const common = require('./lib/data_common');

const outDir = common.EXTRACTED_DIR;
const assetDir = common.ASSETS_DIR;

const US = '\u001f'; // unit separator: groups the node lists inside one line
const RS = '\u001e'; // record separator: separates nodes

// Datasets that should be bundled with the app.
const DATASETS = [
  { name: 'wailai', label: '外来媳妇本地郎', required: true, shape: 'wailai' },
  { name: 'qiershi', label: '七十二家房客', required: true, shape: 'qiershi' },
];

const clean = (s) => String(s ?? '').replace(/[\t\r\n\u001f\u001e]+/g, ' ').trim();
const write = (file, text) => {
  fs.writeFileSync(path.join(assetDir, file), text, 'utf8');
  return fs.statSync(path.join(assetDir, file)).size;
};

fs.mkdirSync(assetDir, { recursive: true });

let built = 0;
const missingRequired = [];

for (const dataset of DATASETS) {
  const sourcePath = path.join(outDir, `${dataset.name}.json`);
  if (!fs.existsSync(sourcePath)) {
    if (dataset.required) missingRequired.push(sourcePath);
    else console.log('[build_asset] skip %s (%s) - no source yet', dataset.name, dataset.label);
    continue;
  }

  const source = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));
  if (dataset.shape === 'wailai') {
    buildWailai(dataset, source);
  } else {
    buildQiershi(dataset, source);
  }
  built += 1;
}

/** 《外来媳妇本地郎》: cards plus the section headings. No characters, tags or storylines. */
function buildWailai(dataset, source) {
  const episodes = Array.isArray(source) ? source : source.episodes || [];
  const sections = Array.isArray(source) ? [] : source.sections || [];

  const tsv = episodes.map((d) =>
    [d.section, d.s, d.e, clean(d.ep), clean(d.title), clean(d.raw)].join('\t')
  );
  const tsvBytes = write(`${dataset.name}.tsv`, tsv.join('\n') + '\n');
  write(`${dataset.name}.sections.txt`, sections.map(clean).join('\n') + (sections.length ? '\n' : ''));
  // No per-card extras and no characters for this drama; the app falls back to the basics.
  write(`${dataset.name}.detail.tsv`, '');

  console.log(
    '[build_asset] %s (%s): %d records, %d sections -> %s KB',
    dataset.name, dataset.label, episodes.length, sections.length, (tsvBytes / 1024).toFixed(1)
  );
}

/** 《七十二家房客》: cards plus synopses, characters, tags, season titles and storylines. */
function buildQiershi(dataset, source) {
  const eps = source.episodes || [];
  const chars = source.characters || [];
  const tags = source.tags || [];
  const seasonTitles = source.seasonTitles || {};
  const main = source.main || [];
  const marks = source.marks || [];

  const tsv = eps.map((d, i) =>
    [d.se, d.s, d.e, d.e > d.s ? `${d.s}-${d.e}` : String(d.s), clean(d.t), ''].join('\t')
  );
  const tsvBytes = write(`${dataset.name}.tsv`, tsv.join('\n') + '\n');

  // Section headings double as the season labels: "第3季：烟馆风波与多方势力初现".
  const seasons = eps.map((d) => d.se);
  const maxSeason = seasons.length ? Math.max(...seasons) : 0;
  const sections = [];
  for (let s = 1; s <= maxSeason; s++) {
    const title = clean(seasonTitles[s] || seasonTitles[String(s)] || '');
    sections.push(title ? `第${s}季：${title}` : `第${s}季`);
  }
  write(`${dataset.name}.sections.txt`, sections.join('\n') + '\n');

  write(`${dataset.name}.tags.txt`, tags.map(clean).join('\n') + (tags.length ? '\n' : ''));
  write(
    `${dataset.name}.chars.tsv`,
    chars.map((c, i) =>
      [i, clean(c.n), c.cat ?? 0, clean((c.a || []).join('、')), clean(c.desc)].join('\t')
    ).join('\n') + '\n'
  );
  write(
    `${dataset.name}.detail.tsv`,
    eps.map((d, i) => {
      const m = marks[i] || [];
      const mainRefs = m.map((x) => `${x.l}:${x.n}`).join(',');
      return [i, clean(d.y), d.c.join(','), d.g.join(','), mainRefs].join('\t');
    }).join('\n') + '\n'
  );
  write(
    `${dataset.name}.lines.tsv`,
    main.map((L, li) => {
      const nodes = L.nodes
        .map((nd) => [clean(nd.g), clean(nd.x), nd.eps.join(',')].join(US))
        .join(RS);
      return [li, clean(L.sec), clean(L.n), clean(L.d), nodes].join('\t');
    }).join('\n') + '\n'
  );

  console.log(
    '[build_asset] %s (%s): %d cards, %d seasons, %d tags, %d characters, %d storylines -> %s KB',
    dataset.name, dataset.label, eps.length, maxSeason, tags.length, chars.length, main.length,
    (tsvBytes / 1024).toFixed(1)
  );
  console.log(
    '[build_asset] %s detail=%s KB chars=%s KB lines=%s KB',
    dataset.name,
    (fs.statSync(path.join(assetDir, `${dataset.name}.detail.tsv`)).size / 1024).toFixed(1),
    (fs.statSync(path.join(assetDir, `${dataset.name}.chars.tsv`)).size / 1024).toFixed(1),
    (fs.statSync(path.join(assetDir, `${dataset.name}.lines.tsv`)).size / 1024).toFixed(1)
  );
}

if (missingRequired.length) {
  console.error('[build_asset] required source(s) missing:');
  for (const p of missingRequired) console.error('  %s', p);
  console.error('[build_asset] run:  node tools/extract_data.js && node tools/extract_qiershi.js');
  process.exit(1);
}

console.log('[build_asset] done: %d dataset(s) built', built);

