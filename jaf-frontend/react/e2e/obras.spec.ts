import { test, expect } from '@playwright/test';

/**
 * Testes E2E para a gestão de Obras
 * 
 * Pré-requisitos: Usuário autenticado com permissão CRIAR_OBRA
 */
test.describe('Gestão de Obras', () => {
  test.beforeEach(async ({ page }) => {
    // Login como admin
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'admin@gmail.com');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/home');
  });

  test('deve acessar tela de nova obra', async ({ page }) => {
    await page.click('text=Nova Obra');
    await expect(page).toHaveURL('http://localhost:5173/obras/criar');
    await expect(page.locator('h1')).toContainText('Nova Obra');
  });

  test('deve criar obra com sucesso', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/criar');

    // Preencher formulário
    await page.fill('input[name="titulo"]', 'Obra Teste E2E');
    await page.fill('input[name="orcamento"]', '500000.00');
    await page.selectOption('select[name="status"]', 'EM_ANDAMENTO');
    await page.fill('input[name="dtInicio"]', '2024-01-01');
    await page.fill('input[name="dtTerminoPrevisto"]', '2024-12-31');
    await page.fill('input[name="responsavel"]', 'João Silva');
    await page.fill('input[name="endereco"]', 'Rua Teste, 123');
    await page.fill('input[name="cidade"]', 'São Paulo');
    await page.fill('input[name="estado"]', 'SP');

    await page.click('button[type="submit"]');

    // Verificar sucesso
    await expect(page.locator('text=Obra criada com sucesso')).toBeVisible();
  });

  test('deve validar data término anterior a início', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/criar');

    await page.fill('input[name="titulo"]', 'Obra Teste');
    await page.fill('input[name="dtInicio"]', '2024-12-31');
    await page.fill('input[name="dtTerminoPrevisto"]', '2024-01-01');

    await page.click('button[type="submit"]');

    // Verificar erro de validação
    await expect(page.locator('text=A data de término deve ser posterior à data de início')).toBeVisible();
  });

  test('deve validar título duplicado', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/criar');

    await page.fill('input[name="titulo"]', 'Obra Teste E2E'); // Título já existe
    await page.fill('input[name="orcamento"]', '500000.00');
    await page.selectOption('select[name="status"]', 'EM_ANDAMENTO');
    await page.fill('input[name="dtInicio"]', '2024-01-01');
    await page.fill('input[name="dtTerminoPrevisto"]', '2024-12-31');

    await page.click('button[type="submit"]');

    // Verificar erro
    await expect(page.locator('text=Obra já existente')).toBeVisible();
  });

  test('deve visualizar detalhes da obra', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/detalhamento');
    await page.click('text=Obra Teste E2E');

    await expect(page.locator('text=Obra Teste E2E')).toBeVisible();
    await expect(page.locator('text=São Paulo')).toBeVisible();
    await expect(page.locator('text=SP')).toBeVisible();
  });

  test('deve listar gastos da obra', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/1');
    await page.click('text=Gastos');

    await expect(page.locator('text=Material de construção')).toBeVisible();
  });

  test('deve listar alocações da obra', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/1');
    await page.click('text=Alocações');

    await expect(page.locator('text=João Silva')).toBeVisible();
    await expect(page.locator('text=PEDREIRO')).toBeVisible();
  });

  test('gestor deve ver apenas suas obras', async ({ page }) => {
    // Login como gestor
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'gestor@jaf.com');
    await page.fill('input[type="password"]', 'gestor123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/home');

    // Navegar para obras
    await page.click('text=Obras');

    // Verificar que não vê todas as obras
    const obraCount = await page.locator('.card-obra').count();
    expect(obraCount).toBeGreaterThan(0);
    expect(obraCount).toBeLessThan(10); // Gestor não vê todas
  });

  test('deve editar obra com sucesso', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/1');
    await page.click('text=Editar');

    await page.fill('input[name="titulo"]', 'Obra Editada');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Obra atualizada com sucesso')).toBeVisible();
    await expect(page.locator('text=Obra Editada')).toBeVisible();
  });

  test('deve deletar obra com sucesso', async ({ page }) => {
    await page.goto('http://localhost:5173/obras/1');
    await page.click('text=Deletar');

    // Confirmar diálogo
    await page.click('text=Confirmar');

    await expect(page.locator('text=Obra deletada com sucesso')).toBeVisible();
  });
});
