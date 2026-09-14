import { describe, it, expect } from 'vitest';
import { formatINR, formatDate } from '../lib/utils';

describe('Frontend Utils', () => {
  it('formats INR numbers correctly', () => {
    expect(formatINR(500000)).toMatch(/5,00,000/);
    expect(formatINR(0)).toMatch(/0/);
    expect(formatINR(null)).toBe('₹0');
  });

  it('formats dates into readable format', () => {
    const formatted = formatDate('2026-09-05');
    expect(formatted).toContain('Sep');
    expect(formatted).toContain('2026');
  });
});
