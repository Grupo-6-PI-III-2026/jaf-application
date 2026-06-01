import { test, expect } from '@playwright/test';

/**
 * Testes E2E para a gestão de Funcionários
 * 
 * Pré-requisitos: Usuário autenticado com permissão CRIAR_FUNCIONARIO
 */
test.describe('Gestão de Funcionários', () => {
  test.beforeEach(async ({ page }) => {
    // Login como admin
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/home');
  });

  test('deve acessar tela de novo funcionário', async ({ page }) => {
    await page.click('text=Novo Funcionário');
    await expect(page).toHaveURL('http://localhost:5173/funcionarios/novo');
    await expect(page.locator('h1')).toContainText('Novo Funcionário');
  });

  test('deve criar funcionário interno com sucesso', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    // Preencher formulário
    await page.fill('input[name="nome"]', 'João Teste');
    await page.fill('input[name="email"]', 'joao@teste.com');
    await page.fill('input[name="senha"]', 'Senha123');
    await page.selectOption('select[name="cargo"]', 'ADMIN');
    await page.selectOption('select[name="tipoFuncionario"]', 'INTERNO');

    await page.click('button[type="submit"]');

    // Verificar sucesso
    await expect(page.locator('text=Funcionário criado com sucesso')).toBeVisible();
  });

  test('deve criar funcionário externo com sucesso', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    // Preencher formulário
    await page.fill('input[name="nome"]', 'Pedro Externo');
    await page.fill('input[name="documento"]', '12345678901');
    await page.selectOption('select[name="tipoFuncionario"]', 'EXTERNO');

    await page.click('button[type="submit"]');

    // Verificar sucesso
    await expect(page.locator('text=Funcionário criado com sucesso')).toBeVisible();
  });

  test('deve validar nome duplicado', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    await page.fill('input[name="nome"]', 'Admin'); // Nome já existe
    await page.fill('input[name="email"]', 'novo@teste.com');
    await page.fill('input[name="senha"]', 'Senha123');
    await page.selectOption('select[name="cargo"]', 'ADMIN');
    await page.selectOption('select[name="tipoFuncionario"]', 'INTERNO');

    await page.click('button[type="submit"]');

    // Verificar erro
    await expect(page.locator('text=Usuário já existe')).toBeVisible();
  });

  test('deve validar email duplicado', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    await page.fill('input[name="nome"]', 'Nome Único');
    await page.fill('input[name="email"]', 'admin@gmail.com'); // Email já existe
    await page.fill('input[name="senha"]', 'Senha123');
    await page.selectOption('select[name="cargo"]', 'ADMIN');
    await page.selectOption('select[name="tipoFuncionario"]', 'INTERNO');

    await page.click('button[type="submit"]');

    // Verificar erro
    await expect(page.locator('text=E-mail já cadastrado')).toBeVisible();
  });

  test('deve validar campos obrigatórios', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    await page.click('button[type="submit"]');

    // Verificar validações
    await expect(page.locator('text=Nome é obrigatório')).toBeVisible();
    await expect(page.locator('text=Email é obrigatório')).toBeVisible();
    await expect(page.locator('text=Senha é obrigatória')).toBeVisible();
  });

  test('funcionário externo não deve ter senha', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios/novo');

    await page.fill('input[name="nome"]', 'Pedro Externo');
    await page.selectOption('select[name="tipoFuncionario"]', 'EXTERNO');

    // Campo senha deve ser desabilitado ou oculto
    const senhaField = page.locator('input[name="senha"]');
    await expect(senhaField).not.toBeVisible();
  });

  test('deve listar funcionários internos', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios');
    await page.click('text=Internos');

    await expect(page.locator('text=Admin')).toBeVisible();
  });

  test('deve listar funcionários externos', async ({ page }) => {
    await page.goto('http://localhost:5173/funcionarios');
    await page.click('text=Externos');

    await expect(page.locator('text=Pedro Externo')).toBeVisible();
  });
});
