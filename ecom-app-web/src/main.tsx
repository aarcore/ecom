import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

/* Condensed display face carries the race-plate voice; Barlow handles body copy. */
import '@fontsource/barlow-condensed/400.css';
import '@fontsource/barlow-condensed/600.css';
import '@fontsource/barlow-condensed/700.css';
import '@fontsource/barlow/400.css';
import '@fontsource/barlow/500.css';
import '@fontsource/barlow/600.css';

import App from './App';
import './styles/tokens.css';
import './styles/global.css';
import './styles/chrome.css';
import './styles/masthead.css';
import './styles/home.css';
import './styles/listing.css';
import './styles/product.css';
import './styles/trade.css';
import './styles/account.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);
