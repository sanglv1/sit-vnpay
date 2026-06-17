import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('./components/Auth/AuthBootstrap', () => ({
  __esModule: true,
  default: ({ children }) => <div data-testid="auth-bootstrap">{children}</div>,
}));

jest.mock('./components/Auth/Login', () => ({
  __esModule: true,
  default: () => <div>Login Page</div>,
}));

jest.mock('./components/Auth/PrivateRoute', () => ({
  __esModule: true,
  default: ({ children }) => <div data-testid="private-route">{children}</div>,
}));

jest.mock('./components/Layout/Layout', () => ({
  __esModule: true,
  default: () => <div>Layout Root</div>,
}));

jest.mock('./providers/QueryProvider', () => ({
  __esModule: true,
  default: ({ children }) => <div data-testid="query-provider">{children}</div>,
}));

jest.mock('./stores', () => ({
  store: {
    getState: () => ({}),
    dispatch: () => {},
    subscribe: () => () => {},
  },
}));

jest.mock('./configs/routes', () => ([
  { path: 'home', adminOnly: false, component: () => <div>Home Route</div> },
]));

describe('App smoke', () => {
  test('renders app shell without crashing', () => {
    render(<App />);
    expect(screen.getByTestId('query-provider')).toBeInTheDocument();
    expect(screen.getByTestId('auth-bootstrap')).toBeInTheDocument();
  });
});
