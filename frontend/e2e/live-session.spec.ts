import { expect, test, type APIRequestContext } from '@playwright/test'

const api = process.env.E2E_API_URL ?? 'http://127.0.0.1:8080/api/v1'

async function json(response: Awaited<ReturnType<APIRequestContext['post']>>) {
  expect(response.ok(), await response.text()).toBeTruthy()
  return response.json()
}

test('host and guest can run a live session in the browser', async ({ page, request, browser }) => {
  const stamp = Date.now()
  const email = `e2e-${stamp}@example.com`
  const password = 'secure-password-123'
  const session = await json(
    await request.post(`${api}/auth/register`, {
      data: { email, password, displayName: 'E2E Host' },
    }),
  )
  const auth = { Authorization: `Bearer ${session.accessToken}` }
  const org = await json(
    await request.post(`${api}/organizations`, {
      headers: auth,
      data: { name: 'E2E Org', slug: `e2e-org-${stamp}` },
    }),
  )
  const category = await json(
    await request.post(`${api}/organizations/${org.id}/question-categories`, {
      headers: auth,
      data: { name: 'Core', slug: 'core' },
    }),
  )
  const question = await json(
    await request.post(`${api}/organizations/${org.id}/questions`, {
      headers: auth,
      data: {
        text: 'Is AssessFlow live?',
        type: 'SINGLE_CHOICE',
        difficulty: 'EASY',
        status: 'ACTIVE',
        categoryId: category.id,
        options: [
          { text: 'Yes', correct: true },
          { text: 'No', correct: false },
        ],
      },
    }),
  )
  const assessment = await json(
    await request.post(`${api}/organizations/${org.id}/assessments`, {
      headers: auth,
      data: { title: 'E2E Live' },
    }),
  )
  expect(
    (
      await request.post(
        `${api}/organizations/${org.id}/assessments/${assessment.id}/questions/${question.id}`,
        { headers: auth, data: { points: 1 } },
      )
    ).ok(),
  ).toBeTruthy()
  expect(
    (
      await request.post(`${api}/organizations/${org.id}/assessments/${assessment.id}/publish`, {
        headers: auth,
      })
    ).ok(),
  ).toBeTruthy()
  const live = await json(
    await request.post(
      `${api}/organizations/${org.id}/assessments/${assessment.id}/live-sessions`,
      { headers: auth },
    ),
  )

  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await page.waitForURL(/\/app/)
  await page.goto(`/app/organizations/${org.id}/live-sessions/${live.id}`)
  await expect(page.getByText(`Code ${live.joinCode}`)).toBeVisible({ timeout: 20_000 })

  const guest = await browser.newPage()
  await guest.goto(`/join/${live.joinCode}`)
  await guest.getByLabel('Your name').fill('Guest')
  await guest.getByRole('button', { name: 'Join session' }).click()
  await expect(guest.getByText("You're in!")).toBeVisible({ timeout: 20_000 })

  await page.getByRole('button', { name: 'Start Session' }).click()
  await expect(guest.getByRole('heading', { name: 'Is AssessFlow live?' })).toBeVisible({
    timeout: 20_000,
  })
  await guest.getByRole('button', { name: 'Yes' }).click()
  await guest.getByRole('button', { name: 'Submit' }).click()
  await expect(guest.getByText('Answer submitted')).toBeVisible()
  await page.getByRole('button', { name: 'End question' }).click()
  await page.getByRole('button', { name: 'Finish' }).click()
  await expect(page.getByText(/Status: FINISHED/i)).toBeVisible({ timeout: 20_000 })
  await guest.close()
})
