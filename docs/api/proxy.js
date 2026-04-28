/**
 * Local proxy server for LMPS API Tester
 * Bypasses CORS and self-signed SSL cert issues during local testing.
 *
 * Usage:  node proxy.js
 * Then open: http://localhost:3000
 */

const http  = require('http');
const https = require('https');
const fs    = require('fs');
const path  = require('path');

const PORT = 3333;

const ROUTES = {
  '/proxy/login': {
    method:   'POST',
    target:   'https://api2-uat.laobullionbank.com/lbb-customer-api/v1/auth/login',
    hostname: 'api2-uat.laobullionbank.com',
    path:     '/lbb-customer-api/v1/auth/login',
    port:     443,
  },
  '/proxy/get-member-list': {
    method:   'GET',
    target:   'https://uatapiv2.laobullionbank.com:8899/api/lmps/get-member-list',
    hostname: 'uatapiv2.laobullionbank.com',
    path:     '/api/lmps/get-member-list',
    port:     8899,
  },
};

function forwardRequest(proxyRoute, clientReq, clientRes) {
  const route   = ROUTES[proxyRoute];
  const chunks  = [];

  clientReq.on('data', c => chunks.push(c));
  clientReq.on('end', () => {
    const body = Buffer.concat(chunks);

    const headers = {
      'Content-Type':   clientReq.headers['content-type'] || 'application/json',
      'Authorization':  clientReq.headers['authorization'] || '',
      'Device-ID':      clientReq.headers['device-id']      || '',
      'Content-Length': body.length,
    };

    const options = {
      hostname:           route.hostname,
      port:               route.port,
      path:               route.path,
      method:             route.method,
      headers:            headers,
      rejectUnauthorized: false, // allow self-signed certs on UAT
    };

    const upstream = https.request(options, upstreamRes => {
      const parts = [];
      upstreamRes.on('data', c => parts.push(c));
      upstreamRes.on('end', () => {
        const responseBody = Buffer.concat(parts);
        clientRes.writeHead(upstreamRes.statusCode, {
          'Content-Type':                'application/json',
          'Access-Control-Allow-Origin': '*',
        });
        clientRes.end(responseBody);
      });
    });

    upstream.on('error', err => {
      console.error('[proxy error]', err.message);
      clientRes.writeHead(502, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
      clientRes.end(JSON.stringify({ error: err.message }));
    });

    upstream.write(body);
    upstream.end();
  });
}

const server = http.createServer((req, res) => {
  // CORS preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin':  '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, Device-ID',
    });
    return res.end();
  }

  // Proxy routes
  if (ROUTES[req.url]) {
    return forwardRequest(req.url, req, res);
  }

  // Serve the HTML tester page
  if (req.url === '/' || req.url === '/index.html') {
    const file = path.join(__dirname, 'test-get-member-list.html');
    fs.readFile(file, (err, data) => {
      if (err) { res.writeHead(404); return res.end('Not found'); }
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(data);
    });
    return;
  }

  res.writeHead(404);
  res.end('Not found');
});

server.listen(PORT, () => {
  console.log(`API Tester running at http://localhost:${PORT}`);
});
