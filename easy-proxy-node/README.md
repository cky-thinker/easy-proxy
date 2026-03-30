# easy-proxy-node

Node.js SDK for Easy Proxy.

## Installation

```bash
npm install easy-proxy-node
```

## Usage

```javascript
const { EasyProxyClient } = require('easy-proxy-node');

const client = new EasyProxyClient({
    serverIp: '127.0.0.1',
    serverPort: 21090,
    webPort: 21092,
    token: 'your-client-token',
    configDir: './config' // Optional, defaults to './config'
});

client.on('connected', () => {
    console.log('Successfully connected to Easy Proxy Server');
});

client.on('error', (err) => {
    console.error('Error:', err);
});

client.start();
```

## License

ISC
