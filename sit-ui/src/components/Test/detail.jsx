import { useParams, Link } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { getFlowLabel } from '../../i18n/flowLabels';
import { useTestRunQuery } from '../../api/hooks';

const TestDetail = () => {
  const { id } = useParams();
  const { t, locale } = useI18n();
  const { data: run, isLoading } = useTestRunQuery(id);

  const formatDate = (value) => (
    value ? new Date(value).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') : t('common.empty')
  );

  if (isLoading) {
    return (
      <>
        <Breadcrumbs title={t('tests.detailTitle', { id })} />
        <div className="card-body">
          <div className="sit-list-loading">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        </div>
      </>
    );
  }

  if (!run) {
    return (
      <>
        <Breadcrumbs title={t('tests.detailTitle', { id })} />
        <div className="card-body">
          <div className="sit-list-empty">
            <i className="ri-file-search-line" aria-hidden="true" />
            <p>{t('common.searchEmpty')}</p>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Breadcrumbs title={t('tests.detailTitle', { id: run.id })} />
      <div className="card-header sit-page-header">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h3 className="card-title mb-0">
              {t('tests.detailHeading', { id: run.id })}
              {' '}
              <span className={`badge sit-status-badge ${run.passed ? 'badge-success' : 'badge-danger'}`}>
                <i className={run.passed ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'} aria-hidden="true" />
                {run.passed ? t('common.pass') : t('common.fail')}
              </span>
            </h3>
            <p className="sit-page-subtitle mb-0">{t('tests.detailSubtitle')}</p>
          </div>
          <Link to="/sessions" className="btn btn-light-primary btn-sm">{t('tests.backSessions')}</Link>
        </div>
      </div>
      <div className="card-body">
        <div className="row mb-3">
          <div className="col-lg-3"><strong>{t('tests.terminal')}:</strong> {run.partnerName}</div>
          <div className="col-lg-3"><strong>{t('common.flow')}:</strong> {getFlowLabel(t, run.flow)}</div>
          <div className="col-lg-3"><strong>{t('tests.type')}:</strong> {run.callbackType}</div>
          <div className="col-lg-3"><strong>{t('tests.testCase')}:</strong> {run.testCaseLabel || run.testCase}</div>
          <div className="col-lg-3"><strong>{t('tests.txnRef')}:</strong> {run.txnRef}</div>
          <div className="col-lg-3"><strong>{t('tests.httpStatus')}:</strong> {run.httpStatus ?? t('common.empty')}</div>
          <div className="col-lg-3"><strong>{t('tests.duration')}:</strong> {run.durationMs} ms</div>
          <div className="col-lg-3"><strong>{t('tests.runAt')}:</strong> {formatDate(run.createdAt)}</div>
          {run.callbackType === 'IPN' && (
            <>
              <div className="col-lg-3"><strong>{t('tests.expectedRsp')}:</strong> {run.expectedRspCode ?? t('common.empty')}</div>
              <div className="col-lg-3"><strong>{t('tests.actualRsp')}:</strong> {run.actualRspCode ?? t('common.empty')}</div>
            </>
          )}
        </div>
        <div className="mb-3">
          <strong>{t('tests.targetUrl')}:</strong>
          <div className="text-muted" style={{ wordBreak: 'break-all' }}>{run.targetUrl}</div>
        </div>
        {run.errorMessage && (
          <div className="alert alert-danger mb-3">{run.errorMessage}</div>
        )}
        <div className="mb-3">
          <strong>{t('tests.requestParams')}</strong>
          <pre className="code-block">{run.requestParams}</pre>
        </div>
        <div>
          <strong>{t('tests.responseBody')}</strong>
          <pre className="code-block">{run.responseBody || t('tests.emptyResponse')}</pre>
        </div>
      </div>
    </>
  );
};

export default TestDetail;
