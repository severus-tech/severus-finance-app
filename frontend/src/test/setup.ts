import '@testing-library/jest-dom';

// Global mock for ResizeObserver (needed for Recharts in jsdom)
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
