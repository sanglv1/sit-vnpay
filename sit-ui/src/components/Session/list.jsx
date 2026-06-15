import { Link, useNavigate } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useSessionsQuery } from '../../api/hooks';

const SessionList = () => {
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const { data } = useSessionsQuery({ page: 0, size: 50 });

  const formatDate = (v) => (
    v ? new Date(v).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') : t('common.empty')
  );

  return (
    <>
      <Breadcrumbs title={t('sessions.title')} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h3 className="card-title mb-0">{t('sessions.title')}</h3>
            <p className="text-muted mb-0 mt-1" style={{ fontSize: 13 }}>
              {t('sessions.subtitle')}
            </p>
          </div>
          <Link to="/sessions/new" className="btn btn-primary btn-sm">
            <i className="ri-add-line" /> {t('sessions.create')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        {data && (
          <div className="table-wrap">
            <table className="table table-striped">
              <thead>
                <tr>
                  <th>{t('common.id')}</th>
                  <th>{t('sessions.terminal')}</th>
                  <th>TMN</th>
                  <th>{t('sessions.auto')}</th>
                  <th>{t('common.status')}</th>
                  <th>{t('sessions.updated')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr key={s.id}>
                    <td>#{s.id}</td>
                    <td>{s.partnerName}</td>
                    <td><span className="badge badge-info">{s.tmnCode}</span></td>
                    <td>{s.autoPassed}/{s.autoTotal} {t('common.pass')}</td>
                    <td>{s.status}</td>
                    <td>{formatDate(s.updatedAt)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-light-primary btn-sm"
                        onClick={() => navigate(`/sessions/${s.id}/auto`)}
                      >
                        {t('sessions.open')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {data.content.length === 0 && (
              <p className="text-muted text-center">
                {t('sessions.empty')}{' '}
                <Link to="/sessions/new">{t('sessions.emptyLink')}</Link>
              </p>
            )}
          </div>
        )}
      </div>
    </>
  );
};

export default SessionList;
