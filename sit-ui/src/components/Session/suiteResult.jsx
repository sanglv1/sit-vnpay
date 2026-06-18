import { Link, useNavigate, useParams } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useSuiteResultQuery } from '../../api/hooks';

const SuiteStepBadge = ({ passed, t }) => (
  <span className={`badge sit-status-badge ${passed ? 'badge-success' : 'badge-danger'}`}>
    <i className={passed ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'} aria-hidden="true" />
    {passed ? t('common.pass') : t('common.fail')}
  </span>
);

const SessionSuiteResult = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { data: suite, error, isLoading } = useSuiteResultQuery(sessionId);

  if (isLoading) {
    return (
      <>
        <Breadcrumbs title={t('sessions.suiteResultTitle')} />
        <div className="card-body">
          <div className="sit-list-loading">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        </div>
      </>
    );
  }

  if (!suite) {
    return (
      <>
        <Breadcrumbs title={t('sessions.suiteResultTitle')} />
        <div className="card-body">
          <div className="sit-list-empty">
            <i className="ri-file-list-3-line" aria-hidden="true" />
            <p>
              {error?.message || t('sessions.suiteNoData')}{' '}
              <Link to={`/sessions/${sessionId}/auto`}>{t('sessions.backToSession')}</Link>
            </p>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Breadcrumbs title={`${t('sessions.title')} #${sessionId} — ${t('sessions.suiteResultTitle')}`} />
      <div className="card-header sit-page-header">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h3 className="card-title mb-0">
              {t('sessions.suiteResultHeading')} — {suite.partnerName}
              {' '}
              <span className={`badge sit-status-badge ${suite.allPassed ? 'badge-success' : 'badge-danger'}`}>
                <i className={suite.allPassed ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'} aria-hidden="true" />
                {suite.passedSteps}/{suite.totalSteps} {t('common.pass')}
              </span>
            </h3>
            <p className="sit-meta-line mb-0">
              {t('sessions.suiteTxnSuccess')}: <strong>{suite.txnRef}</strong>
              {suite.failedTxnRef && (
                <>
                  {' · '}
                  {t('sessions.suiteTxnFailed')}: <strong>{suite.failedTxnRef}</strong>
                </>
              )}
            </p>
          </div>
          <button
            type="button"
            className="btn btn-light-primary btn-sm"
            onClick={() => navigate(`/sessions/${sessionId}/auto`)}
          >
            <i className="ri-arrow-left-line" aria-hidden="true" />
            {t('sessions.backToSession')}
          </button>
        </div>
      </div>
      <div className="card-body">
        <div className="sit-list-table-wrap">
          <table className="table data-table sit-data-table suite-result-table">
            <colgroup>
              <col className="col-step" />
              <col className="col-badge" />
              <col className="col-scenario" />
              <col className="col-rsp-expected" />
              <col className="col-rsp-actual" />
              <col className="col-http" />
              <col className="col-result" />
              <col className="col-action" />
            </colgroup>
            <thead>
              <tr>
                <th className="suite-col suite-col--step">{t('common.step')}</th>
                <th className="suite-col suite-col--badge">{t('common.case')}</th>
                <th className="suite-col suite-col--scenario">{t('common.scenario')}</th>
                <th className="suite-col suite-col--rsp">{t('tests.rspExpected')}</th>
                <th className="suite-col suite-col--rsp">{t('tests.rspActual')}</th>
                <th className="suite-col suite-col--http">{t('tests.httpStatus')}</th>
                <th className="suite-col suite-col--result">{t('tests.result')}</th>
                <th className="suite-col suite-col--action">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {suite.steps.map((step) => (
                <tr key={`${step.step}-${step.testCase}`} className="sit-data-row">
                  <td className="suite-col suite-col--step">{step.step}</td>
                  <td className="suite-col suite-col--badge">
                    <span className="badge badge-info">Case {step.caseCode}</span>
                  </td>
                  <td className="suite-col suite-col--scenario suite-scenario-cell" title={step.testCaseLabel}>
                    {step.testCaseLabel}
                  </td>
                  <td className="suite-col suite-col--rsp"><strong>{step.expectedRspCode ?? '—'}</strong></td>
                  <td className="suite-col suite-col--rsp">{step.actualRspCode ?? '—'}</td>
                  <td className="suite-col suite-col--http">{step.httpStatus ?? '—'}</td>
                  <td className="suite-col suite-col--result">
                    <SuiteStepBadge passed={step.passed} t={t} />
                  </td>
                  <td className="suite-col suite-col--action">
                    {step.testRunId && (
                      <Link
                        to={`/tests/${step.testRunId}`}
                        className="btn btn-light-primary btn-sm sit-open-btn"
                        title={t('common.detail')}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <i className="ri-eye-line" aria-hidden="true" />
                        <span>{t('common.detail')}</span>
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!suite.allPassed && (
          <div className="alert alert-danger mt-3">
            {t('sessions.suiteNotAllPassed')}
          </div>
        )}
        {suite.allPassed && (
          <div className="alert alert-success mt-3">
            {t('sessions.suiteAllPassed')}
          </div>
        )}
      </div>
    </>
  );
};

export default SessionSuiteResult;
