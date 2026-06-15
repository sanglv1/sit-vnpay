import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './assets/css/sit-theme.css';

const basename = process.env.REACT_APP_BASENAME || '';
if (basename && !window.location.pathname.startsWith(basename)) {
  window.location.replace(
    basename + (window.location.pathname === '/' ? '' : window.location.pathname) + window.location.search
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
