// Strips the bulky D = {…} payload out of a v2.20 page so the remaining markup, styles and
// interaction logic can be reviewed directly.
//
// Usage: node tools/strip_data.js <source.html> <out.txt>
const fs = require('fs');
const path = require('path');

function extractLiteral(html, name) {
  const re = new RegExp(`const\\s+${name}\\s*=\\s*`);
  const m = re.exec(html);
  if (!m) return null;
  const start = m.index + m[0].length;
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
    if (ch === '"' || ch === "'" || ch === '`') { inString = ch; continue; }
    if (ch === '{' || ch === '[') depth++;
    else if (ch === '}' || ch === ']') {
      depth--;
      if (depth === 0) return { start, end: i + 1 };
    }
  }
  return null;
}

const src = process.argv[2];
const out = process.argv[3] || path.join(require('os').tmpdir(), 'stripped.txt');
if (!src) {
  console.error('usage: node tools/strip_data.js <source.html> [out.txt]');
  process.exit(1);
}

const html = fs.readFileSync(src, 'utf8');
const span = extractLiteral(html, 'D');
if (!span) {
  console.error('could not find const D');
  process.exit(1);
}

const stripped =
  html.slice(0, span.start) +
  '/* ===== D payload omitted (' + (span.end - span.start) + ' chars) ===== */' +
  html.slice(span.end);

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, stripped, 'utf8');
console.log('source   : %s', src);
console.log('omitted  : %d chars of data', span.end - span.start);
console.log('stripped : %s (%s KB)', out, (stripped.length / 1024).toFixed(1));
