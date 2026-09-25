import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Pager } from './Pager'

describe('Pager', () => {
  afterEach(() => cleanup())

  it('stays quiet when everything fits on one page', () => {
    const { container } = render(
      <Pager page={0} size={12} totalElements={4} totalPages={1} onPage={() => undefined} />
    )
    expect(container.querySelector('nav')).toBeNull()
  })

  it('moves to the next slice and announces the range', () => {
    const onPage = vi.fn()
    render(<Pager page={0} size={12} totalElements={30} totalPages={3} onPage={onPage} />)
    expect(screen.getByText('Showing 1–12 of 30')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(onPage).toHaveBeenCalledWith(1)
  })
})
