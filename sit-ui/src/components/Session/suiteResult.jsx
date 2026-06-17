import { Link, useNavigate, useParams } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useSuiteResultQuery } from '../../api/hooks';

const SessionSuiteResult = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { data: suite, error, isLoading } = useSuiteResultQuery(sessionId);

  if (isLoading) {
    return (
      <>
        <Breadcrumbs title="Kết quả kiểm tra tự động" />
        <div className="card-body text-center text-muted">{t('common.loading')}</div>
      </>
    );
  }

  if (!suite) {
    return (
      <>
        <Breadcrumbs title="Kết quả kiểm tra tự động" />
        <div className="card-body text-center text-muted">
          {error?.message || 'Không có dữ liệu suite.'}{' '}
          <Link to={`/sessions/${sessionId}/auto`}>Quay lại phiên kiểm thử</Link>
        </div>
      </>
    );
  }

  return (
    <>
      <Breadcrumbs title={`Phiên #${sessionId} — Kết quả tự động`} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h3 className="card-title mb-0">
              Suite IPN — {suite.partnerName}
              {' '}
              <span className={`badge ${suite.allPassed ? 'badge-success' : 'badge-danger'}`}>
                {suite.passedSteps}/{suite.totalSteps} PASS
              </span>
            </h3>
            <div className="text-muted mt-1" style={{ fontSize: 13 }}>
              TxnRef: <strong>{suite.txnRef}</strong>
            </div>
          </div>
          <button type="button" className="btn btn-light-primary btn-sm" onClick={() => navigate(`/sessions/${sessionId}/auto`)}>
            Quay lại phiên
          </button>
        </div>
      </div>
      <div className="card-body">
        <div className="table-wrap">
          <table className="table table-striped data-table suite-result-table">
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
                <th className="text-center">Bước</th>
                <th className="text-center">Case</th>
                <th>Kịch bản</th>
                <th className="text-center">RspCode<br />kỳ vọng</th>
                <th className="text-center">RspCode<br />thực tế</th>
                <th className="text-center">HTTP</th>
                <th className="text-center">Kết quả</th>
                <th className="text-end" />
              </tr>
            </thead>
            <tbody>
              {suite.steps.map((step) => (
                <tr key={`${step.step}-${step.testCase}`}>
                  <td className="text-center">{step.step}</td>
                  <td className="text-center">
                    <span className="badge badge-info">Case {step.caseCode}</span>
                  </td>
                  <td className="suite-scenario-cell" title={step.testCaseLabel}>{step.testCaseLabel}</td>
                  <td className="text-center"><strong>{step.expectedRspCode ?? '—'}</strong></td>
                  <td className="text-center">{step.actualRspCode ?? '—'}</td>
                  <td className="text-center">{step.httpStatus ?? '—'}</td>
                  <td className="text-center">
                    <span className={`badge ${step.passed ? 'badge-success' : 'badge-danger'}`}>
                      {step.passed ? 'PASS' : 'FAIL'}
                    </span>
                  </td>
                  <td className="text-end">
                    {step.testRunId && (
                      <Link to={`/tests/${step.testRunId}`} className="btn btn-icon" title="Chi tiết">
                        <i className="ri-eye-line text-primary" />
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
            Một hoặc nhiều case chưa PASS. Kiểm tra chi tiết từng bước.
          </div>
        )}
        {suite.allPassed && (
          <div className="alert alert-success mt-3">
            Tất cả case IPN tự động đã PASS.
          </div>
        )}
      </div>
    </>
  );
};

export default SessionSuiteResult;
