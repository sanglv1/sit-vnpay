import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';
import SessionHeader from './SessionHeader';
import AcceptanceTabs from './AcceptanceTabs';
import {
  useManualAcceptanceQuery,
  useSaveManualAcceptanceMutation,
  useSessionQuery,
} from '../../api/hooks';
import { appActions } from '../../stores';

const readImageAsDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(reader.result);
  reader.onerror = reject;
  reader.readAsDataURL(file);
});

const ImageUpload = ({ label, preview, onChange }) => (
  <div className="manual-upload">
    <label className="form-label">{label}</label>
    <input type="file" accept="image/*" className="form-control" onChange={onChange} />
    {preview && (
      <a href={preview} target="_blank" rel="noreferrer" className="manual-thumb-wrap">
        <img src={preview} alt={label} className="manual-thumb" />
      </a>
    )}
  </div>
);

const RadioPair = ({ name, label, description, register, trueLabel, falseLabel }) => (
  <div className="manual-check-card">
    <div className="manual-check-title">{label}</div>
    {description && <p className="manual-check-desc">{description}</p>}
    <div className="manual-radio-group">
      <label className="manual-radio">
        <input type="radio" value="true" {...register(name)} />
        {trueLabel}
      </label>
      <label className="manual-radio">
        <input type="radio" value="false" {...register(name)} />
        {falseLabel}
      </label>
    </div>
  </div>
);

const SessionManual = () => {
  const { sessionId } = useParams();
  const dispatch = useDispatch();
  const [successPreview, setSuccessPreview] = useState(null);
  const [failedPreview, setFailedPreview] = useState(null);
  const { register, handleSubmit, reset, setValue } = useForm();
  const { data: session } = useSessionQuery(sessionId);
  const { data: acceptance, isFetched: acceptanceLoaded } = useManualAcceptanceQuery(sessionId, {
    enabled: Boolean(session),
  });
  const saveAcceptance = useSaveManualAcceptanceMutation(sessionId);

  useEffect(() => {
    if (!session || !acceptanceLoaded) return;
    if (!acceptance) {
      reset({
        partnerId: session.partnerId,
        sessionId: session.id,
        exceptionHandled: 'true',
        whitelistIpPassed: 'true',
        logStoragePassed: 'true',
      });
      setSuccessPreview(null);
      setFailedPreview(null);
      return;
    }
    reset({
      id: acceptance.id,
      partnerId: session.partnerId,
      sessionId: session.id,
      returnSuccessTxnRef: acceptance.returnSuccessTxnRef || '',
      returnFailedTxnRef: acceptance.returnFailedTxnRef || '',
      exceptionHandled: acceptance.exceptionHandled != null ? String(acceptance.exceptionHandled) : 'true',
      whitelistIpPassed: acceptance.whitelistIpPassed != null ? String(acceptance.whitelistIpPassed) : 'true',
      logStoragePassed: acceptance.logStoragePassed != null ? String(acceptance.logStoragePassed) : 'true',
      note: acceptance.note || '',
    });
    setSuccessPreview(acceptance.returnSuccessImage);
    setFailedPreview(acceptance.returnFailedImage);
  }, [session, acceptance, acceptanceLoaded, reset]);

  const onImageChange = async (e, field, setPreview) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      dispatch(appActions.flash('Ảnh tối đa 2MB', 'danger'));
      return;
    }
    const dataUrl = await readImageAsDataUrl(file);
    setValue(field, dataUrl);
    setPreview(dataUrl);
  };

  const onSubmit = async (values) => {
    try {
      const payload = {
        id: values.id ? Number(values.id) : null,
        partnerId: session.partnerId,
        sessionId: Number(sessionId),
        returnSuccessTxnRef: values.returnSuccessTxnRef,
        returnFailedTxnRef: values.returnFailedTxnRef,
        returnSuccessImage: values.returnSuccessImage || successPreview,
        returnFailedImage: values.returnFailedImage || failedPreview,
        exceptionHandled: values.exceptionHandled === 'true',
        whitelistIpPassed: values.whitelistIpPassed === 'true',
        logStoragePassed: values.logStoragePassed === 'true',
        note: values.note,
      };
      const saved = await saveAcceptance.mutateAsync(payload);
      setValue('id', saved.id);
      dispatch(appActions.flash('Lưu kết quả QC thành công'));
    } catch {
      // mutation cache handles API errors
    }
  };

  if (!session) return null;

  return (
    <>
      <div className="card-body pb-0">
        <SessionHeader session={session} />
      </div>
      <AcceptanceTabs />

      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <h3 className="card-title mb-0">Nghiệm thu thủ công</h3>
          <button type="button" className="btn btn-primary btn-sm" onClick={handleSubmit(onSubmit)}>
            <i className="ri-save-line" /> Lưu kết quả QC
          </button>
        </div>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit(onSubmit)}>
          <input type="hidden" {...register('id')} />
          <input type="hidden" {...register('partnerId')} />

          <div className="row manual-columns">
            <div className="col-lg-6">
              <div className="manual-section">
                <h4 className="manual-section-title">1. Return URL (Website/app của merchant)</h4>
                <div className="manual-subsection">
                  <div className="manual-subtitle">A. TxnRef GD Thành công</div>
                  <input className="form-control mb-2" placeholder="Mã giao dịch thành công" {...register('returnSuccessTxnRef')} />
                  <ImageUpload label="Ảnh Return URL Thành công" preview={successPreview} onChange={(e) => onImageChange(e, 'returnSuccessImage', setSuccessPreview)} />
                  <input type="hidden" {...register('returnSuccessImage')} />
                </div>
                <div className="manual-subsection">
                  <div className="manual-subtitle">B. TxnRef GD Thất bại</div>
                  <input className="form-control mb-2" placeholder="Mã giao dịch thất bại" {...register('returnFailedTxnRef')} />
                  <ImageUpload label="Ảnh Return URL Thất bại" preview={failedPreview} onChange={(e) => onImageChange(e, 'returnFailedImage', setFailedPreview)} />
                  <input type="hidden" {...register('returnFailedImage')} />
                </div>
              </div>
            </div>
            <div className="col-lg-6">
              <div className="manual-section">
                <h4 className="manual-section-title">2. Kiểm tra nghiệp vụ hệ thống</h4>
                <RadioPair
                  name="exceptionHandled"
                  label="A. Xác nhận xử lý lỗi ngoại lệ ở đầu IPN URL (case ex → RspCode 99)"
                  description="Merchant xử lý exception tại IPN URL. Lỗi không được xử lý phải trả RspCode = 99 cho VNPAY."
                  register={register}
                  trueLabel="Đã xử lý"
                  falseLabel="Chưa xử lý"
                />
                <RadioPair
                  name="whitelistIpPassed"
                  label="B. Trạng thái Whitelist IP"
                  description="Merchant đã whitelist dải IP của VNPAY cho API IPN."
                  register={register}
                  trueLabel="Đạt"
                  falseLabel="Không đạt"
                />
                <RadioPair
                  name="logStoragePassed"
                  label="C. Trạng thái Lưu Log"
                  description="Merchant lưu log toàn bộ request từ VNPAY và ngược lại."
                  register={register}
                  trueLabel="Đạt"
                  falseLabel="Không đạt"
                />
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-12">
              <label className="form-label">Ghi chú QC</label>
              <textarea className="form-control" rows={2} {...register('note')} />
            </div>
          </div>
          <div className="form-footer">
            <button type="submit" className="btn btn-primary">
              <i className="ri-save-line" /> Lưu kết quả QC
            </button>
          </div>
        </form>
      </div>
    </>
  );
};

export default SessionManual;
