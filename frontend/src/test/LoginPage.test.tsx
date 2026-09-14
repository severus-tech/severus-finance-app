import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { LoginPage } from '../pages/LoginPage';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';

vi.mock('../lib/api', () => ({
  api: {
    post: vi.fn().mockRejectedValue({ response: { data: { message: 'Invalid email or password' } } }),
  },
  setAccessToken: vi.fn(),
}));

describe('LoginPage Component', () => {
  it('renders login form with title and input fields', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByText(/SEVERUS FINANCE/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Authenticate & Unlock/i })).toBeInTheDocument();
  });
});
