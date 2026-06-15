import { Link, useSearchParams } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useTestHistoryQuery } from '../../api/hooks';

const TestHistory = () => {
  const { t, locale } = useI18n();
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get('page') || 0);
  const { data } = useTestHistoryQuery({ page, size: 20 });

  const formatDate = (value) => (
    value ? new Date(value).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') : t('common.empty')
  );

  return (
    <>
      <Breadcrumbs title={t('tests.historyTitle')} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <h3 className="card-title mb-0">{t('tests.historyTitle')}</h3>
          <Link to="/sessions/new" className="btn btn-primary btn-sm">
            <i className="ri-play-circle-line" /> {t('tests.runNew')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        {data && (
          <>
            <div className="table-wrap">
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>{t('common.id')}</th>
                    <th>{t('tests.terminal')}</th>
                    <th>{t('common.flow')}</th>
                    <th>{t('tests.type')}</th>
                    <th>{t('tests.testCase')}</th>
                    <th>{t('tests.txnRef')}</th>
                    <th className="text-center">{t('tests.result')}</th>
                    <th>{t('tests.time')}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((run) => (
                    <tr key={run.id}>
                      <td>{run.id}</td>
                      <td>{run.partnerName}</td>
                      <td><span className="badge badge-info">{run.flow}</span></td>
                      <td>{run.callbackType}</td>
                      <td>{run.testCase}</td>
                      <td>{run.txnRef}</td>
                      <td className="text-center">
                        <span className={`badge ${run.passed ? 'badge-success' : 'badge-danger'}`}>
                          {run.passed ? t('common.pass') : t('common.fail')}
                        </span>
                      </td>
                      <td>{formatDate(run.createdAt)}</td>
                      <td>
                        <Link to={`/tests/${run.id}`} className="btn btn-icon" title={t('common.view')}>
                          <i className="ri-eye-line text-primary" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="d-flex gap-2 mt-3">
              <button
                type="button"
                className="btn btn-light-primary btn-sm"
                disabled={page <= 0}
                onClick={() => setSearchParams({ page: page - 1 })}
              >
                {t('common.previous')}
              </button>
              <span className="align-items-center d-flex text-muted">
                {t('common.pageOf', { page: data.page + 1, total: Math.max(data.totalPages, 1) })}
              </span>
              <button
                type="button"
                className="btn btn-light-primary btn-sm"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setSearchParams({ page: page + 1 })}
              >
                {t('common.next')}
              </button>
            </div>
          </>
        )}
      </div>
    </>
  );
};

export default TestHistory;
