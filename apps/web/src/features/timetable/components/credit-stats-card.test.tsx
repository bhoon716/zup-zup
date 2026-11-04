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
