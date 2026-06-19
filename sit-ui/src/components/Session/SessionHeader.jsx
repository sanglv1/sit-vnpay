import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useExportMinutesMutation, useManualAcceptanceQuery } from '../../api/hooks';
import { useI18n } from '../../i18n/useI18n';
import { appActions } from '../../stores';

const SessionHeader = ({ session, onRerunAll }) => {
  const dispatch = useDispatch();
  const { t } = useI18n();
  const exportMinutes = useExportMinutesMutation();
  const { data: qcResult } = useManualAcceptanceQuery(session?.id, { enabled: Boolean(session?.id) });
  const qcSaved = Boolean(qcResult?.id);

  const onExportMinutes = async () => {
    if (!qcSaved) {
      dispatch(appActions.flash(t('sessions.exportQcRequired'), 'warning'));
      return;
    }
    try {
      const filename = `VNPAYGW-${session.tmnCode}-SIT.docx`;
      await exportMinutes.mutateAsync({ sessionId: session.id, filename });
      dispatch(appActions.flash(t('sessions.exportSuccess'), 'success'));
    } catch {
      // mutation cache handles API errors
    }
  };

  return (
    <div className="session-header">
      <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
        <div>
          <div className="d-flex align-items-center gap-2 mb-1">
            <h2 className="session-title mb-0">
              {t('sessions.headerTitle', { id: session.id })}
            </h2>
            <span className="badge badge-info">{session.tmnCode}</span>
          </div>
          <p className="sit-page-subtitle mb-0">
            {session.partnerName}
            {' — '}
            {t('sessions.headerSubtitle')}
          </p>
          <div className="sit-meta-line mt-1">
            {t('sessions.headerAutoProgress', {
              passed: session.autoPassed,
              total: session.autoTotal,
            })}
          </div>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <button
            type="button"
            className="btn btn-light-primary btn-sm"
            onClick={onExportMinutes}
            disabled={!qcSaved}
            title={qcSaved ? undefined : t('sessions.exportQcRequired')}
          >
            <i className="ri-file-download-line" />
            {' '}
            {t('sessions.exportMinutes')}
          </button>
          {onRerunAll && (
            <button type="button" className="btn btn-primary btn-sm" onClick={onRerunAll}>
              <i className="ri-refresh-line" />
              {' '}
              {t('sessions.rerunAll')}
            </button>
          )}
          <Link to="/sessions" className="btn btn-light-primary btn-sm">
            {t('sessions.backToList')}
          </Link>
        </div>
      </div>
    </div>
  );
};

export default SessionHeader;
