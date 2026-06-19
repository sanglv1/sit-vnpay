import { NavLink, useParams } from 'react-router-dom';
import { useI18n } from '../../i18n/useI18n';

const AcceptanceTabs = () => {
  const { sessionId } = useParams();
  const { t } = useI18n();

  return (
    <div className="acceptance-tabs">
      <NavLink
        to={`/sessions/${sessionId}/auto`}
        className={({ isActive }) => `acceptance-tab${isActive ? ' active' : ''}`}
      >
        {t('sessions.acceptanceTabAuto')}
      </NavLink>
      <NavLink
        to={`/sessions/${sessionId}/manual`}
        className={({ isActive }) => `acceptance-tab${isActive ? ' active' : ''}`}
      >
        {t('sessions.acceptanceTabManual')}
      </NavLink>
    </div>
  );
};

export default AcceptanceTabs;
