const http = require('http');
const fs = require('fs');
const path = require('path');
const root = __dirname;
const types = {'.html':'text/html; charset=utf-8', '.js':'text/javascript; charset=utf-8',
    '.css':'text/css; charset=utf-8', '.ttf':'font/ttf'};
http.createServer((req, res) => {
    try {
        const url = new URL(req.url, 'http://localhost');
        const pathname = decodeURIComponent(url.pathname);
        const file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
        if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
        if (!fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404).end(); return; }
        res.setHeader('Content-Type', types[path.extname(file)] || 'text/plain; charset=utf-8');
        res.setHeader('Cache-Control', 'no-store');
        fs.createReadStream(file).on('error', () => res.destroy()).pipe(res);
    } catch { res.writeHead(400).end(); }
}).listen(8765, '127.0.0.1', () => console.log('PIKOOS UX study: http://127.0.0.1:8765'));
