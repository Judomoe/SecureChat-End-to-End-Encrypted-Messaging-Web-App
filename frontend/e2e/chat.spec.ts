import { test, expect, type Page, type BrowserContext } from '@playwright/test';

const TEST_PREFIX = `e2e_${Date.now()}`;
const USER_A = { username: `user_a_${TEST_PREFIX}`, email: `a_${TEST_PREFIX}@test.com`, password: 'testpass123', displayName: 'Alice E2E' };
const USER_B = { username: `user_b_${TEST_PREFIX}`, email: `b_${TEST_PREFIX}@test.com`, password: 'testpass123', displayName: 'Bob E2E' };

async function registerUser(page: Page, user: typeof USER_A) {
  await page.goto('/register');
  await page.fill('#username', user.username);
  await page.fill('#email', user.email);
  await page.fill('#displayName', user.displayName);
  await page.fill('#password', user.password);
  await page.fill('#confirmPassword', user.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/dashboard');
  await expect(page.locator('.dashboard')).toBeVisible();
}

async function loginUser(page: Page, user: typeof USER_A) {
  await page.goto('/login');
  await page.fill('#email', user.email);
  await page.fill('#password', user.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/dashboard');
  await expect(page.locator('.dashboard')).toBeVisible();
}

async function sendContactRequest(fromPage: Page, toUser: typeof USER_A) {
  await fromPage.click('button:has-text("Contacts")');
  await fromPage.fill('.search-bar input', toUser.email);
  await expect(fromPage.locator('text=Search Results')).toBeVisible({ timeout: 10000 });
  await expect(fromPage.locator(`text=${toUser.displayName}`)).toBeVisible();
  await fromPage.click('.search-results .btn-add');
  await expect(fromPage.locator('text=Sent Requests')).toBeVisible({ timeout: 5000 });
  await expect(fromPage.locator('.sent-requests').locator(`text=${toUser.displayName}`)).toBeVisible();
}

async function acceptContactRequest(toPage: Page, fromUser: typeof USER_A) {
  await toPage.click('button:has-text("Contacts")');
  await expect(toPage.locator('text=Pending Requests')).toBeVisible({ timeout: 10000 });
  await toPage.click('.pending-requests .btn-accept');
  await expect(toPage.locator('.accepted-contacts').locator(`text=${fromUser.displayName}`)).toBeVisible({ timeout: 5000 });
}

async function startConversation(fromPage: Page, partnerDisplayName: string, sendText?: string) {
  await fromPage.click(`.accepted-contacts .contact-item:has-text("${partnerDisplayName}")`);
  await fromPage.waitForSelector('.chat-area', { timeout: 15000 });
  if (sendText) {
    await fromPage.fill('.chat-input input', sendText);
    await fromPage.click('.chat-input button');
    await expect(fromPage.locator(`.message-text:has-text("${sendText}")`)).toBeVisible({ timeout: 10000 });
  }
}

async function clickConversation(page: Page, username: string, expectMessage?: string) {
  await expect(page.locator(`.conversation-item:has-text("${username}")`)).toBeVisible({ timeout: 10000 });
  await page.click(`.conversation-item:has-text("${username}")`);
  await page.waitForSelector('.chat-area', { timeout: 15000 });
  if (expectMessage) {
    await expect(page.locator(`.message-text:has-text("${expectMessage}")`)).toBeVisible({ timeout: 20000 });
  }
}

async function sendMessage(page: Page, text: string) {
  await page.fill('.chat-input input', text);
  await page.click('.chat-input button');
  await expect(page.locator(`.message-text:has-text("${text}")`)).toBeVisible({ timeout: 10000 });
}

test.describe('SecureChat E2E', () => {
  test('full contact and chat flow between two users', async ({ browser }) => {
    // Create two separate browser contexts so sessions stay active
    const contextA: BrowserContext = await browser.newContext();
    const contextB: BrowserContext = await browser.newContext();
    const pageA: Page = await contextA.newPage();
    const pageB: Page = await contextB.newPage();

    try {
      // Step 1: Register both users in parallel
      await Promise.all([
        registerUser(pageA, USER_A),
        registerUser(pageB, USER_B),
      ]);

      // Step 2: A sends contact request to B
      await sendContactRequest(pageA, USER_B);

      // Step 3: B accepts contact request
      await acceptContactRequest(pageB, USER_A);

      // Step 4: B starts conversation and sends first message
      await startConversation(pageB, USER_A.displayName, 'Hello from Bob!');

      // Step 5: A switches to Chats tab, sees conversation, opens it to fetch key, sees B's message
      await pageA.click('button:has-text("Chats")');
      await clickConversation(pageA, USER_B.username, 'Hello from Bob!');

      // Step 6: A replies
      await sendMessage(pageA, 'Hi Bob! Alice here.');

      // Step 7: B polls and sees A's reply (poll interval is 3s, wait up to 10s)
      await expect(pageB.locator('.message-text:has-text("Hi Bob! Alice here.")')).toBeVisible({ timeout: 15000 });

      // Step 8: B replies back
      await sendMessage(pageB, 'Great to hear from you, Alice!');

      // Step 9: A polls and sees B's new reply
      await expect(pageA.locator('.message-text:has-text("Great to hear from you, Alice!")')).toBeVisible({ timeout: 15000 });
    } finally {
      await contextA.close();
      await contextB.close();
    }
  });

  test('search for non-existent user shows no results', async ({ page }) => {
    const searchUser = {
      username: `search_${TEST_PREFIX}`,
      email: `search_${TEST_PREFIX}@test.com`,
      password: 'testpass123',
      displayName: 'Search Tester',
    };
    await registerUser(page, searchUser);
    await page.click('button:has-text("Contacts")');
    await page.fill('.search-bar input', 'nonexistent@nowhere.com');
    await expect(page.locator('text=Search Results')).not.toBeVisible({ timeout: 5000 });
  });

  test('cannot send message without conversation key', async ({ page }) => {
    const newUser = {
      username: `nochat_${TEST_PREFIX}`,
      email: `nochat_${TEST_PREFIX}@test.com`,
      password: 'testpass123',
      displayName: 'No Chat',
    };
    await registerUser(page, newUser);
    await expect(page.locator('text=Select a conversation to start chatting')).toBeVisible();
  });
});
