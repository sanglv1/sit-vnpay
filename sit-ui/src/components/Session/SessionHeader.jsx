import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useExportMinutesMutation } from '../../api/hooks';
import { appActions } from '../../stores';

const SessionHeader = ({ session, onRerunAll }) => {
  const dispatch = useDispatch();
  const exportMinutes = useExportMinutesMutation();

  const onExportMinutes = async () => {
    try {
      const filename = `VNPAYGW-${session.tmnCode}-SIT.docx`;
      await exportMinutes.mutateAsync({ sessionId: session.id, filename });
      dispatch(appActions.flash('Xuất biên bản thành công', 'success'));
    } catch {
      // mutation cache handles API errors
    }
  };

  return (
  <div className="session-header">
    <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
      <div>
        <div className="d-flex align-items-center gap-2 mb-1">
          <h2 className="session-title mb-0">Phiên kiểm thử #{session.id}</h2>
          <span className="badge badge-info">{session.tmnCode}</span>
        </div>
        <p className="session-subtitle mb-0">
          {session.partnerName} — Chi tiết và thực thi các kịch bản nghiệm thu Merchant IPN.
        </p>
        <div className="text-muted mt-1" style={{ fontSize: 12 }}>
          Tiến độ tự động: <strong>{session.autoPassed}/{session.autoTotal}</strong> case PASS
        </div>
      </div>
      <div className="d-flex gap-2">
        <button type="button" className="btn btn-light-primary btn-sm" onClick={onExportMinutes}>
          <i className="ri-file-download-line" /> Xuất biên bản
        </button>
        {onRerunAll && (
          <button type="button" className="btn btn-primary btn-sm" onClick={onRerunAll}>
            <i className="ri-refresh-line" /> Chạy lại tất cả
          </button>
        )}
        <Link to="/sessions" className="btn btn-light-primary btn-sm">
          ← Danh sách phiên
        </Link>
      </div>
    </div>
  </div>
  );
};

export default SessionHeader;
