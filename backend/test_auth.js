const http = require('http');

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body }));
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function parseCookies(setCookieArr) {
  const jar = {};
  for (const line of (setCookieArr || [])) {
    const [nameVal] = line.split(';');
    const [name, ...rest] = nameVal.split('=');
    jar[name.trim()] = rest.join('=');
  }
  return jar;
}

async function run() {
  let cookies = {};

  console.log('=== STEP 1: Login ===');
  let r1 = await request({
    hostname: 'localhost', port: 8080, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({ email: 'demo@shopai.com', password: 'User1234!' }));

  console.log(`Status: ${r1.statusCode}`);
  Object.assign(cookies, parseCookies(r1.headers['set-cookie']));
  console.log('Cookies after login:', cookies);

  for (let i = 1; i <= 6; i++) {
    const cookieHeader = Object.entries(cookies).map(([k, v]) => `${k}=${v}`).join('; ');
    const xsrf = cookies['XSRF-TOKEN'] || '';
    console.log(`\n=== STEP ${i + 1}: POST /api/cart/items (XSRF=${xsrf.substring(0, 8)}...) ===`);

    let r = await request({
      hostname: 'localhost', port: 8080, path: '/api/cart/items', method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Cookie': cookieHeader,
        'X-XSRF-TOKEN': xsrf
      }
    }, JSON.stringify({ productId: 1, quantity: 1 }));

    console.log(`Status: ${r.statusCode}`);
    const newCookies = parseCookies(r.headers['set-cookie']);
    if (Object.keys(newCookies).length) {
      console.log('Set-Cookies from response:', newCookies);
      Object.assign(cookies, newCookies);
      // If Max-Age=0 sent, token was cleared — capture it properly
      const setCookieLines = r.headers['set-cookie'] || [];
      for (const line of setCookieLines) {
        if (line.includes('Max-Age=0') && line.includes('XSRF-TOKEN')) {
          console.log('⚠️  XSRF-TOKEN cookie was CLEARED by server!');
        }
      }
    }
  }
}
run().catch(console.error);
