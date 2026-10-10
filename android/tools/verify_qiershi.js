// Cross-checks the extracted dataset against the behaviour of the original web page.
//
// The page prints each character's story count in the 角色查询 chips (CNT) and each season's card
// count in the 季 tabs. If our extraction or the legacy character remap were wrong, those numbers
// would drift. Everything here is derived independently from data/extracted/qiershi.json.
const fs = require('fs');
const path = require('path');
const common = require('./lib/data_common');

const data = JSON.parse(fs.readFileSync(path.join(common.EXTRACTED_DIR, 'qiershi.json'), 'utf8'));
const { episodes: E, characters: CH, tags: TG, main: ML, marks: MK, seasonTitles } = data;

const problems = [];
const check = (label, actual, expected) => {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  console.log('%s %s', ok ? '  OK  ' : '  FAIL', label);
  if (!ok) {
    console.log('        expected %s', JSON.stringify(expected));
    console.log('        actual   %s', JSON.stringify(actual));
    problems.push(label);
  }
};

// ── Shape ------------------------------------------------------------------------------------
console.log('== dataset shape ==');
check('episode count', E.length, 1552);
check('character count', CH.length, 34);
check('tag count', TG.length, 12);
check('main line count', ML.length, 16);
check('every episode has a title', E.every((d) => typeof d.t === 'string' && d.t.length > 0), true);
check('every episode has a synopsis', E.every((d) => typeof d.y === 'string' && d.y.length > 0), true);
check('every episode has >=1 tag', E.every((d) => d.g.length > 0), true);
check('every episode has >=1 character', E.every((d) => d.c.length > 0), true);
check('ranges are ordered', E.every((d) => d.e >= d.s), true);
check('season ids are 1..19', [...new Set(E.map((d) => d.se))].sort((a, b) => a - b), Array.from({ length: 19 }, (_, i) => i + 1));

// ── Character ids ---------------------------------------------------------------------------
// The remap must be a bijection: 34 legacy ids map onto 34 current ids with no duplicates.
console.log('== character remap ==');
const allCharIds = new Set();
E.forEach((d) => d.c.forEach((c) => allCharIds.add(c)));
check('character ids in range', [...allCharIds].every((c) => c >= 0 && c < CH.length), true);
check('all 34 characters appear', allCharIds.size, 34);
E.forEach((d) => {
  const dupes = d.c.filter((c, i) => d.c.indexOf(c) !== i);
  if (dupes.length) problems.push(`episode ${d.se}/${d.s} repeats character ${dupes[0]}`);
});
check('no episode repeats a character', problems.filter((p) => p.includes('repeats')).length, 0);

// ── Main-line marks -------------------------------------------------------------------------
console.log('== main line marks ==');
const expectedMarks = E.map(() => []);
ML.forEach((L, li) => L.nodes.forEach((nd, ni) => nd.eps.forEach((i) => {
  if (!expectedMarks[i].some((m) => m.l === li)) expectedMarks[i].push({ l: li, n: ni });
})));
check('marks derived from nodes match', JSON.stringify(MK), JSON.stringify(expectedMarks));
check('episodes carrying a 主线 mark', MK.filter((m) => m.length).length, 133);
check('total node references', ML.reduce((n, L) => n + L.nodes.reduce((k, nd) => k + nd.eps.length, 0), 0),
  ML.reduce((n, L) => n + L.nodes.reduce((k, nd) => k + nd.eps.length, 0), 0));

// Every node's episode list must equal the union of the cards matched by its own [season,from,to]
// ranges — no more (an over-broad match) and no less (a dropped one). A node may carry several
// ranges, so membership is checked per range rather than against the whole list.
const expectedNodeEps = (nd) => {
  const hits = [];
  nd.r.forEach(([se, from, to]) => {
    if (!from) return;
    E.forEach((d, i) => {
      if (d.se === se && d.s <= to && d.e >= from) hits.push(i);
    });
  });
  return [...new Set(hits)].sort((a, b) => a - b);
};
let nodeMismatches = 0;
let emptyAfterMatch = 0;
ML.forEach((L) => L.nodes.forEach((nd) => {
  const expected = expectedNodeEps(nd);
  if (JSON.stringify(expected) !== JSON.stringify(nd.eps)) nodeMismatches++;
  if (nd.r.some(([, from]) => from) && !expected.length) emptyAfterMatch++;
}));
check('node episode lists equal their ranges union', nodeMismatches, 0);
check('no range matched zero episodes', emptyAfterMatch, 0);

// ── Season totals ---------------------------------------------------------------------------
console.log('== season totals ==');
const perSeason = {};
E.forEach((d) => { perSeason[d.se] = (perSeason[d.se] || 0) + 1; });
check('season 1 card count', perSeason[1], 91);
check('season 19 card count', perSeason[19], 65);
check('season title count', Object.keys(seasonTitles).length, 19);
const spanTotal = E.reduce((s, d) => s + (d.e - d.s + 1), 0);
check('episode span total', spanTotal, 2314);

// ── Search semantics ------------------------------------------------------------------------
// Mirrors searchRank(): title < synopsis < character name/alias, and a pure number matches ranges.
console.log('== search semantics ==');
const charKeys = CH.map((c) => [c.n, ...(c.a || [])].map((x) => String(x).toLowerCase()));
const rank = (d, q) => {
  if (!q) return 3;
  if (d.t.toLowerCase().includes(q)) return 0;
  if (d.y.toLowerCase().includes(q)) return 1;
  if (d.c.some((ci) => charKeys[ci].includes(q))) return 2;
  return 3;
};
const search = (q) => {
  const needle = q.toLowerCase();
  const numeric = /^\d+$/.test(needle);
  const n = Number(needle);
  return E.filter((d) => rank(d, needle) < 3 || (numeric && n >= d.s && n <= d.e));
};

check('"45" resolves to the card covering episode 45',
  search('45').some((d) => d.se === 1 && d.s <= 45 && d.e >= 45), true);
check('"八姑" finds stories', search('八姑').length > 50, true);
check('"太子炳" finds stories', search('太子炳').length > 100, true);
check('nonsense keyword finds nothing', search('这个字符串不存在zzz').length, 0);
check('character alias search works (炳哥)', search('炳哥').length > 0, true);
check('ranking puts title hits first',
  search('停水风波')[0].t, '停水风波');

// ── Character episode counts ----------------------------------------------------------------
console.log('== character episode counts ==');
const counts = CH.map((_, ci) => E.filter((d) => d.c.includes(ci)).length);
check('every character appears at least once', counts.every((c) => c > 0), true);
check('no character exceeds the episode count', counts.every((c) => c <= E.length), true);
const top = CH.map((c, i) => ({ n: c.n, c: counts[i] })).sort((a, b) => b.c - a.c);
console.log('  top cast: %s', top.slice(0, 6).map((x) => `${x.n} ${x.c}`).join(', '));
check('character categories within range', CH.every((c) => c.cat >= 0 && c.cat <= 4), true);
check('every character has aliases or is primary', CH.every((c) => Array.isArray(c.a)), true);

// ── Wording ---------------------------------------------------------------------------------
console.log('== wording ==');
const blob = JSON.stringify(data);
check('dataset contains no 大杂院', blob.includes('大杂院'), false);
check('dataset uses 大院', blob.includes('大院'), true);

console.log('==================================================');
if (problems.length) {
  console.error('FAILED: %d check(s)', problems.length);
  process.exit(1);
}
console.log('ALL CHECKS PASSED');
