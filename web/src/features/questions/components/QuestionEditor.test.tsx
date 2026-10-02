import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { QuestionEditor } from './QuestionEditor'

describe('QuestionEditor', () => {
  it('adds options and marks the correct answer', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(
      <QuestionEditor
        categories={[
          { id: 'cat-1', organizationId: 'org-1', name: 'Core', slug: 'core', createdAt: '' },
        ]}
        submitLabel="Create question"
        onSubmit={onSubmit}
        onCancel={() => undefined}
      />,
    )
    await user.type(screen.getByLabelText('Question text'), 'What is TCP?')
    await user.type(screen.getByLabelText('Option 1'), 'Transport')
    await user.type(screen.getByLabelText('Option 2'), 'Physical')
    await user.click(screen.getByRole('button', { name: 'Add option' }))
    await user.type(screen.getByLabelText('Option 3'), 'Application')
    await user.click(screen.getAllByLabelText('Correct')[0])
    await user.click(screen.getByRole('button', { name: 'Create question' }))
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        text: 'What is TCP?',
        type: 'SINGLE_CHOICE',
        categoryId: 'cat-1',
        options: [
          expect.objectContaining({ text: 'Transport', correct: true }),
          expect.objectContaining({ text: 'Physical', correct: false }),
          expect.objectContaining({ text: 'Application', correct: false }),
        ],
      }),
    )
  })
})
