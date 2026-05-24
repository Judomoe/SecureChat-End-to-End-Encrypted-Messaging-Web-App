import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 120000,
  expect: {
    timeout: 15000,
  },
  retries: 0,
  use: {
    baseURL: 'http://localhost:5173',
    channel: 'chrome',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },
});
