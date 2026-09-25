// @ts-check
const { test, expect } = require('@playwright/test');

test.describe('Frontend do Sentiment API (servido pelo Spring Boot, não mockado)', () => {
  test('clica em analisar (positivo), o resultado aparece e os contadores atualizam', async ({ page }) => {
    await page.goto('/');

    const totalAntes = await page.locator('#total').textContent();

    await page.getByRole('button', { name: '😊 Positivo' }).click();
    await expect(page.locator('#texto')).not.toHaveValue('');

    await page.getByRole('button', { name: 'Analisar Sentimento' }).click();

    await expect(page.locator('#resultado')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#previsao')).toContainText('Positivo');
    await expect(page.locator('#probabilidade')).toContainText('Probabilidade');

    // carregarStats() roda depois da análise — o total deve ter mudado.
    await expect(page.locator('#total')).not.toHaveText(totalAntes ?? '', { timeout: 10000 });
  });

  test('clica em analisar (negativo) e o resultado aparece', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: '😠 Negativo' }).click();
    await page.getByRole('button', { name: 'Analisar Sentimento' }).click();

    await expect(page.locator('#resultado')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#previsao')).toContainText('Negativo');
  });

  test('caminho de erro: texto vazio dispara alerta e o resultado não aparece', async ({ page }) => {
    await page.goto('/');

    let dialogMessage = null;
    page.on('dialog', async (dialog) => {
      dialogMessage = dialog.message();
      await dialog.accept();
    });

    await page.getByRole('button', { name: 'Analisar Sentimento' }).click();

    await expect.poll(() => dialogMessage).toBe('Por favor, digite um texto para análise.');
    await expect(page.locator('#resultado')).toBeHidden();
  });
});
