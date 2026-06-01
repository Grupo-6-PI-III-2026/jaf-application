import { test, expect } from '@playwright/test';

/**
 * Testes E2E para a tela de Login
 * 
 * Para executar: npx playwright test login.spec.ts
 */
test.describe('Tela de Login', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:5173/login');
  });

  test('deve exibir o formulário de login', async ({ page }) => {
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('deve realizar login com credenciais válidas', async ({ page }) => {
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');

    // Verificar redirecionamento para a home
    await expect(page).toHaveURL('http://localhost:5173/home');
    await expect(page.locator('text=Admin')).toBeVisible();
  });

  test('deve exibir erro com credenciais inválidas', async ({ page }) => {
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.fill('input[type="password"]', 'senha_errada');
    await page.click('button[type="submit"]');

    // Verificar mensagem de erro
    await expect(page.locator('text=Credenciais inválidas')).toBeVisible();
  });

  test('deve validar campo email vazio', async ({ page }) => {
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');

    // Verificar validação
    await expect(page.locator('text=Email é obrigatório')).toBeVisible();
  });

  test('deve validar campo senha vazio', async ({ page }) => {
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.click('button[type="submit"]');

    // Verificar validação
    await expect(page.locator('text=Senha é obrigatória')).toBeVisible();
  });

  test('deve validar formato de email inválido', async ({ page }) => {
    await page.fill('input[type="email"]', 'email-invalido');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');

    // Verificar validação
    await expect(page.locator('text=Email inválido')).toBeVisible();
  });

  test('deve bloquear acesso a rotas protegidas sem autenticação', async ({ page }) => {
    await page.goto('http://localhost:5173/home');

    // Verificar redirecionamento para login
    await expect(page).toHaveURL('http://localhost:5173/login');
  });

  test('deve manter sessão após refresh da página', async ({ page }) => {
    // Login
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');

    await page.waitForURL('http://localhost:5173/home');

    // Refresh
    await page.reload();

    // Verificar que ainda está logado
    await expect(page).toHaveURL('http://localhost:5173/home');
  });
});
