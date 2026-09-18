import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { JoinPage } from './JoinPages'

describe('JoinPage', () => {
  it('continues to the public code route without login', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/join']}>
        <Routes>
          <Route path="/join" element={<JoinPage />} />
          <Route path="/join/:code" element={<p>Name form</p>} />
        </Routes>
      </MemoryRouter>,
    )
    await user.type(screen.getByLabelText('Code'), 'x7k92p')
    await user.click(screen.getByRole('button', { name: 'Continue' }))
    expect(screen.getByText('Name form')).toBeInTheDocument()
  })
})
