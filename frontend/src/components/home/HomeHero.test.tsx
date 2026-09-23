import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { HomeHero } from './HomeHero'
import { useAuthStore } from '../../store/authStore'

describe('HomeHero', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
    useAuthStore.getState().setBootstrapped(true)
  })

  it('renders the hero scene and shop call to action', () => {
    render(
      <MemoryRouter>
        <HomeHero />
      </MemoryRouter>
    )

    expect(screen.getByRole('img', { name: 'Marni eco organic store' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: /Organic food, directly from trusted farms/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Shop now' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeTruthy()
    expect(document.querySelector('img[src="/images/home-hero.png"]')).toBeTruthy()
  })
})
