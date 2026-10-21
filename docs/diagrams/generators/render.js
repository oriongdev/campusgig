// Renders SVG files to PNG with headless Chromium (needs: npm install playwright).
// usage: node render.js <scale> file.svg ...
const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const scale = parseFloat(process.argv[2]);
  const browser = await chromium.launch();
  for (const file of process.argv.slice(3)) {
    const svg = fs.readFileSync(file, 'utf8');
    const m = svg.match(/viewBox="[-\d.]+ [-\d.]+ ([\d.]+) ([\d.]+)"/);
    const page = await browser.newPage({ viewport: { width: Math.ceil(+m[1]), height: Math.ceil(+m[2]) }, deviceScaleFactor: scale });
    await page.setContent(`<html><body style="margin:0;background:#fff">${svg}</body></html>`);
    await page.waitForTimeout(150);
    await page.screenshot({ path: file.replace(/\.svg$/, '.png'), clip: { x: 0, y: 0, width: Math.ceil(+m[1]), height: Math.ceil(+m[2]) } });
    await page.close();
  }
  await browser.close();
})();
