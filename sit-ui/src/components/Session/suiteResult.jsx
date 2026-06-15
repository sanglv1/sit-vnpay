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
          <div className="d-flex gap-2">
            <button type="button" className="btn btn-light-primary btn-sm" onClick={() => navigate(`/sessions/${sessionId}/auto`)}>
              Quay lại phiên
            </button>
            <Link to="/tests/history" className="btn btn-primary btn-sm">Lịch sử</Link>
          </div>
        </div>
      </div>
      <div className="card-body">
        <div className="table-wrap">
          <table className="table table-striped">
            <thead>
              <tr>
                <th>Bước</th>
                <th>Case</th>
                <th>Kịch bản</th>
                <th className="text-center">RspCode Kỳ vọng</th>
                <th className="text-center">RspCode Thực tế</th>
                <th className="text-center">HTTP</th>
                <th className="text-center">Kết quả</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {suite.steps.map((step) => (
                <tr key={`${step.step}-${step.testCase}`}>
                  <td>{step.step}</td>
                  <td><span className="badge badge-info">Case {step.caseCode}</span></td>
                  <td>{step.testCaseLabel}</td>
                  <td className="text-center"><strong>{step.expectedRspCode ?? '—'}</strong></td>
                  <td className="text-center">{step.actualRspCode ?? '—'}</td>
                  <td className="text-center">{step.httpStatus ?? '—'}</td>
                  <td className="text-center">
                    <span className={`badge ${step.passed ? 'badge-success' : 'badge-danger'}`}>
                      {step.passed ? 'PASS' : 'FAIL'}
                    </span>
                  </td>
                  <td>
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
