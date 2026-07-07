import '@testing-library/jest-dom';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => {
  cleanup();
});

// JSDOM does not implement document.elementFromPoint
if (typeof document !== 'undefined') {
  document.elementFromPoint = (x: number, y: number) => {
    return document.querySelector(`[data-x="${x}"][data-y="${y}"]`) || null;
  };
}
