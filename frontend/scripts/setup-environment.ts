import * as fs from 'node:fs';

const apiBaseUrl: string = process.env.API_BASE_URL || 'http://localhost:8080';

if (!apiBaseUrl) {
  console.error('API_BASE_URL environment variable is required.');
  process.exit(1);
}

console.log(`apiBaseUrl: ${apiBaseUrl}`);

const content: string = `export const environment = {
  production: true,
  apiBaseUrl: '${apiBaseUrl}',
};\n`;

fs.mkdirSync('src/environment', { recursive: true });
fs.writeFileSync('src/environment/environment.ts', content);
