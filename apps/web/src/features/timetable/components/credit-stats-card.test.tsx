import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { CreditStatsCard } from './credit-stats-card';
import { TooltipProvider } from '@/shared/ui/tooltip';

/**
 * CreditStatsCard 컴포넌트의 학점 표시 및 경고 로직을 검증하는 테스트입니다.
 */
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
    
    // Warning icon should not be visible by default (it's only shown if isCreditWarning is true)
    const infoIcon = document.querySelector('svg.text-yellow-500');
    expect(infoIcon).toBeNull();
  });

  it('renders lower limit warning correctly', () => {
    renderWithProvider(<CreditStatsCard totalCredits={3} />);
    
    const creditCount = screen.getByText('3');
    expect(creditCount.className).toContain('text-yellow-500');
  });

  it('renders upper limit warning correctly', () => {
    renderWithProvider(<CreditStatsCard totalCredits={21} />);
    
    const creditCount = screen.getByText('21');
    expect(creditCount.className).toContain('text-yellow-500');
  });
});
