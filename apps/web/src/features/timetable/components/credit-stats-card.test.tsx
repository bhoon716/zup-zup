import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { CreditStatsCard } from './credit-stats-card';
import { TooltipProvider } from '@/shared/ui/tooltip';

describe('CreditStatsCard', () => {
  const renderWithProvider = (ui: React.ReactElement) => {
    return render(
      <TooltipProvider>
        {ui}
      </TooltipProvider>
    );
  };

  it('renders normal credit state correctly', () => {
    renderWithProvider(<CreditStatsCard totalCredits={15} />);

    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('/ 18')).toBeInTheDocument();

    expect(screen.queryByRole('button', { name: /학점 경고/ })).not.toBeInTheDocument();
  });

  it('renders lower limit warning correctly', () => {
    renderWithProvider(<CreditStatsCard totalCredits={3} />);

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '하한 학점 경고' })).toBeInTheDocument();
  });

  it('renders upper limit warning correctly', () => {
    renderWithProvider(<CreditStatsCard totalCredits={21} />);

    expect(screen.getByText('21')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '상한 학점 경고' })).toBeInTheDocument();
  });
});
