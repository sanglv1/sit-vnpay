import UserList from '../components/User';
import Home from '../components/Home';
import PartnerList from '../components/Partner';
import PartnerForm from '../components/Partner/form';
import SessionList from '../components/Session/list';
import SessionCreate from '../components/Session/create';
import SessionAuto from '../components/Session/auto';
import SessionManual from '../components/Session/manual';
import SessionSuiteResult from '../components/Session/suiteResult';
import TestHistory from '../components/Test/history';
import TestDetail from '../components/Test/detail';
import Guide from '../components/Guide';

const routes = [
  { path: '/', component: Home },
  { path: '/guide', component: Guide },
  { path: '/partners', component: PartnerList },
  { path: '/partners/create', component: PartnerForm, adminOnly: true },
  { path: '/partners/:id/edit', component: PartnerForm, adminOnly: true },
  { path: '/users', component: UserList, adminOnly: true },
  { path: '/sessions', component: SessionList },
  { path: '/sessions/new', component: SessionCreate },
  { path: '/sessions/:sessionId/auto', component: SessionAuto },
  { path: '/sessions/:sessionId/manual', component: SessionManual },
  { path: '/sessions/:sessionId/suite-result', component: SessionSuiteResult },
  { path: '/tests/history', component: TestHistory },
  { path: '/tests/:id', component: TestDetail },
  // legacy redirects handled in App or keep old paths mapping to list
  { path: '/tests/auto', component: SessionList },
  { path: '/tests/manual', component: SessionList },
  { path: '/tests/run', component: SessionList },
];

export default routes;
