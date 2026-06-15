import { useParams } from 'react-router-dom';
import { Link } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useTestRunQuery } from '../../api/hooks';

const TestDetail = () => {
  const { id } = useParams();
  const { t, locale } = useI18n();
  const { data: run } = useTestRunQuery(id);

  const formatDate = (value) => (
    value ? new Date(value).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') : t('common.empty')
  );

  if (!run) return <Breadcrumbs title={t('tests.detailTitle', { id })} />;

  return (
    <>
      <Breadcrumbs title={t('tests.detailTitle', { id: run.id })} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <h3 className="card-title mb-0">
            {t('tests.detailHeading', { id: run.id })}
            {' '}
            <span className={`badge ${run.passed ? 'badge-success' : 'badge-danger'}`}>
              {run.passed ? t('common.pass') : t('common.fail')}
            </span>
          </h3>
          <Link to="/tests/history" className="btn btn-light-primary btn-sm">{t('tests.backHistory')}</Link>
        </div>
      </div>
      <div className="card-body">
        <div className="row mb-3">
          <div className="col-lg-3"><strong>{t('tests.terminal')}:</strong> {run.partnerName}</div>
          <div className="col-lg-3"><strong>{t('common.flow')}:</strong> {run.flow}</div>
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
