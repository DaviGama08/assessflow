import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AssessmentQuestionsPanel } from './AssessmentQuestionsPanel'
import type { Question } from '../../questions/types'

const bank: Question[] = [
  {
    id: 'q2',
    organizationId: 'org',
    text: 'Available question',
    type: 'SINGLE_CHOICE',
    difficulty: 'EASY',
    status: 'ACTIVE',
    explanation: null,
    category: { id: 'c1', organizationId: 'org', name: 'Core', slug: 'core', createdAt: '' },
    options: [],
    createdAt: '',
    updatedAt: '',
  },
]

describe('AssessmentQuestionsPanel', () => {
  it('adds, removes and reorders assessment questions', async () => {
    const user = userEvent.setup()
    const onAdd = vi.fn()
    const onRemove = vi.fn()
    const onMove = vi.fn()
    render(
      <AssessmentQuestionsPanel
        items={[
          {
            id: 'link-1',
            assessmentId: 'a1',
            questionId: 'q1',
            questionText: 'Linked question',
            type: 'SINGLE_CHOICE',
            difficulty: 'EASY',
            status: 'ACTIVE',
            points: 2,
            displayOrder: 1,
          },
        ]}
        bank={bank}
        onAdd={onAdd}
        onRemove={onRemove}
        onPoints={vi.fn()}
        onMove={onMove}
      />,
    )
    await user.click(screen.getByRole('button', { name: 'Add' }))
    expect(onAdd).toHaveBeenCalledWith('q2', 1)
    await user.click(screen.getByRole('button', { name: 'Remove' }))
    expect(onRemove).toHaveBeenCalledWith('q1')
    expect(screen.getByRole('button', { name: 'Up' })).toBeDisabled()
  })
})
