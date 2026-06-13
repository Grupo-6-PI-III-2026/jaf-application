import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import StatCard from './src/Components/StatCard/StatCard'

describe('StatCard Component', () => {
  it('deve renderizar o componente com label e valor', () => {
    render(<StatCard label="Total de Obras" value="10" />)

    expect(screen.getByText('Total de Obras')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
  })

  it('deve renderizar sem barra de progresso quando progress não é fornecido', () => {
    render(<StatCard label="Sem Progresso" value="100%" />)

    expect(screen.getByText('Sem Progresso')).toBeInTheDocument()
    expect(screen.getByText('100%')).toBeInTheDocument()
  })

  it('deve renderizar action quando fornecido', () => {
    render(
      <StatCard
        label="Com Action"
        value="Test"
        action={<button type="button">Clique</button>}
      />
    )

    expect(screen.getByText('Clique')).toBeInTheDocument()
  })

  it('deve renderizar corretamente com todos os props', () => {
    render(
      <StatCard
        label="Card Completo"
        value="75%"
        progress={75}
        action={<button type="button">Ação</button>}
      />
    )

    expect(screen.getByText('Card Completo')).toBeInTheDocument()
    expect(screen.getByText('75%')).toBeInTheDocument()
    expect(screen.getByText('Ação')).toBeInTheDocument()
  })
})