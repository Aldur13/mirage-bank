const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;

// Map of file extensions to MIME types
const mimeTypes = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
  // Parse the request URL
  let filePath = path.join(__dirname, req.url);

  // If the URL has no extension or is a directory, try index.html
  if (req.url === '/' || !path.extname(filePath)) {
    filePath = path.join(__dirname, 'index.html');
  }

  // Security: prevent directory traversal
  if (!filePath.startsWith(__dirname)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('Forbidden');
    return;
  }

  // Read and serve the file
  fs.readFile(filePath, (err, data) => {
    if (err) {
      // If file not found and it's not index.html, serve index.html for SPA routing
      if (err.code === 'ENOENT' && req.url !== '/' && !path.extname(req.url)) {
        serveHtmlFile(path.join(__dirname, 'index.html'), res);
        return;
      }

      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }

    // Determine MIME type
    const ext = path.extname(filePath);
    const contentType = mimeTypes[ext] || 'application/octet-stream';

    // For HTML files, inject the backend URL from environment
    if (ext === '.html') {
      serveHtmlFile(filePath, res, data);
    } else {
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': 'public, max-age=3600'
      });
      res.end(data);
    }
  });
});

// Helper function to inject backend URL into HTML
function serveHtmlFile(filePath, res, data) {
  if (!data) {
    data = fs.readFileSync(filePath);
  }

  let html = data.toString();
  const backendUrl = process.env.BACKEND_URL || '';

  // Inject backend URL into the HTML before the closing body tag
  if (backendUrl) {
    const injectedScript = `<script>window.__BACKEND_URL__ = '${backendUrl}';</script>`;
    html = html.replace('</body>', injectedScript + '\n</body>');
  }

  res.writeHead(200, {
    'Content-Type': 'text/html',
    'Cache-Control': 'public, max-age=3600'
  });
  res.end(html);
});

server.listen(PORT, () => {
  console.log(`Frontend server running on port ${PORT}`);
});
