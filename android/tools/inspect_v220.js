// Parses the D = { eps, chars, tags, main } payload out of the v2.20 pages and reports shape,
// counts, and whether the two files share identical data.
const fs = require('fs');
const path = require('path');
const common = require('./lib/data_common');

const FILES = {
  traditional: path.join(common.SOURCES_DIR, 'qiershi_traditional_v2.20.html'),
  modern: path.join(common.SOURCES_DIR, 'qiershi_modern_v2.20.html'),
};

/** Extracts the object literal assigned to `const <name>=` by brace matching. */
function extractLiteral(html, name) {
  const re = new RegExp(`const\\s+${name}\\s*=\\s*`);
  const m = re.exec(html);
  if (!m) return null;
  const start = m.index + m[0].length;
  const open = html[start];
  if (open !== '{' && open !== '[') return null;

  let depth = 0;
  let inString = null;
  let escaped = false;
  for (let i = start; i < html.length; i++) {
    const ch = html[i];
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === '\\') escaped = true;
      else if (ch === inString) inString = null;
      continue;
    }
    if (ch === '"' || ch === "'" || ch === '`') {
      inString = ch;
      continue;
    }
    if (ch === '{' || ch === '[') depth++;
    else if (ch === '}' || ch === ']') {
      depth--;
      if (depth === 0) return html.slice(start, i + 1);
    }
  }
  return null;
}

const parsed = {};

for (const [key, file] of Object.entries(FILES)) {
  console.log('='.repeat(64));
  if (!fs.existsSync(file)) {
    console.log('## %s MISSING', key);
    continue;
  }
  const html = fs.readFileSync(file, 'utf8');
  console.log('## %s  (%s KB utf8)', key, (Buffer.byteLength(html, 'utf8') / 1024).toFixed(1));

  const literal = extractLiteral(html, 'D');
  if (!literal) {
    console.log('   FAILED to extract D');
    continue;
  }

  let D;
  try {
    D = JSON.parse(literal);
  } catch (e) {
    console.log('   D is not plain JSON: %s', e.message);
    console.log('   head: %s', literal.slice(0, 300));
    continue;
  }

  parsed[key] = D;
  console.log('   D keys        : %s', Object.keys(D).join(', '));
  for (const [k, v] of Object.entries(D)) {
    if (Array.isArray(v)) {
      console.log('   D.%-6s      : %d entries', k, v.length);
      console.log('      sample     : %s', JSON.stringify(v[0]).slice(0, 260));
    } else {
      console.log('   D.%-6s      : %s', k, JSON.stringify(v).slice(0, 200));
    }
  }

  // Season breakdown of the episode list.
  const eps = D.eps || [];
  const bySeason = {};
  for (const e of eps) bySeason[e.se] = (bySeason[e.se] || 0) + 1;
  console.log('   episodes/season: %s', JSON.stringify(bySeason));
  const totalEpisodes = eps.reduce((sum, e) => sum + (e.e - e.s + 1), 0);
  console.log('   episode span   : %d story cards covering %d episodes', eps.length, totalEpisodes);
  console.log('   first/last     : %s … %s', JSON.stringify(eps[0] && eps[0].t), JSON.stringify(eps[eps.length - 1] && eps[eps.length - 1].t));

  // Which fields does an entry carry, and are synopses present everywhere?
  const fields = new Set();
  eps.forEach((e) => Object.keys(e).forEach((f) => fields.add(f)));
  console.log('   entry fields   : %s', [...fields].join(', '));
  console.log('   entries with 简介: %d / %d', eps.filter((e) => e.y && e.y.length).length, eps.length);
  console.log('   entries with 标签: %d / %d', eps.filter((e) => Array.isArray(e.g) && e.g.length).length, eps.length);
  console.log('   entries with 角色: %d / %d', eps.filter((e) => Array.isArray(e.c) && e.c.length).length, eps.length);

  // Wording checks.
  for (const needle of ['大杂院', '大院']) {
    const n = literal.split(needle).length - 1;
    console.log('   D contains "%s": %d', needle, n);
  }
  console.log('   whole file "%s": %d', '大杂院', html.split('大杂院').length - 1);
  console.log('   whole file "%s": %d', '大院', html.split('大院').length - 1);
}

// Are the two datasets the same?
console.log('='.repeat(64));
console.log('## comparison');
if (parsed.traditional && parsed.modern) {
  const a = parsed.traditional;
  const b = parsed.modern;
  for (const k of ['eps', 'chars', 'tags', 'main']) {
    const sa = JSON.stringify(a[k]);
    const sb = JSON.stringify(b[k]);
    console.log('   %-6s identical: %s  (%d vs %d chars)', k, sa === sb, sa ? sa.length : 0, sb ? sb.length : 0);
  }
}
