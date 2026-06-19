import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';
import SessionHeader from './SessionHeader';
import AcceptanceTabs from './AcceptanceTabs';
import { useI18n } from '../../i18n/useI18n';
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
  const { t } = useI18n();
  const [successPreview, setSuccessPreview] = useState(null);
  const [failedPreview, setFailedPreview] = useState(null);
  const { register, handleSubmit, reset, setValue } = useForm();
  const { data: session } = useSessionQuery(sessionId);
  const { data: acceptance, isFetched: acceptanceLoaded } = useManualAcceptanceQuery(sessionId, {
    enabled: Boolean(sessionId),
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
      dispatch(appActions.flash(t('sessions.manualImageMaxSize'), 'danger'));
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
      dispatch(appActions.flash(t('sessions.manualSaveSuccess')));
    } catch {
      // mutation cache handles API errors
    }
  };

  if (!session) {
    return (
      <>
        <AcceptanceTabs />
        <div className="card-body">
          <div className="sit-list-loading sit-list-loading--compact">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="card-body pb-0">
        <SessionHeader session={session} />
      </div>
      <AcceptanceTabs />

      <div className="card-header sit-page-header">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div>
            <h3 className="card-title mb-0">{t('sessions.manualTitle')}</h3>
            <p className="sit-page-subtitle mb-0">{t('sessions.manualSubtitle')}</p>
          </div>
          <button type="button" className="btn btn-primary btn-sm" onClick={handleSubmit(onSubmit)}>
            <i className="ri-save-line" /> {t('sessions.saveQc')}
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
                <h4 className="manual-section-title">{t('sessions.manualReturnSection')}</h4>
                <div className="manual-subsection">
                  <div className="manual-subtitle">{t('sessions.manualSuccessTxn')}</div>
                  <input
                    className="form-control mb-2"
                    placeholder={t('sessions.manualSuccessTxnPlaceholder')}
                    {...register('returnSuccessTxnRef')}
                  />
                  <ImageUpload
                    label={t('sessions.manualSuccessImage')}
                    preview={successPreview}
                    onChange={(e) => onImageChange(e, 'returnSuccessImage', setSuccessPreview)}
                  />
                  <input type="hidden" {...register('returnSuccessImage')} />
                </div>
                <div className="manual-subsection">
                  <div className="manual-subtitle">{t('sessions.manualFailedTxn')}</div>
                  <input
                    className="form-control mb-2"
                    placeholder={t('sessions.manualFailedTxnPlaceholder')}
                    {...register('returnFailedTxnRef')}
                  />
                  <ImageUpload
                    label={t('sessions.manualFailedImage')}
                    preview={failedPreview}
                    onChange={(e) => onImageChange(e, 'returnFailedImage', setFailedPreview)}
                  />
                  <input type="hidden" {...register('returnFailedImage')} />
                </div>
              </div>
            </div>
            <div className="col-lg-6">
              <div className="manual-section">
                <h4 className="manual-section-title">{t('sessions.manualSystemSection')}</h4>
                <RadioPair
                  name="exceptionHandled"
                  label={t('sessions.manualExceptionLabel')}
                  description={t('sessions.manualExceptionDesc')}
                  register={register}
                  trueLabel={t('common.handled')}
                  falseLabel={t('common.notHandled')}
                />
                <RadioPair
                  name="whitelistIpPassed"
                  label={t('sessions.manualWhitelistLabel')}
                  description={t('sessions.manualWhitelistDesc')}
                  register={register}
                  trueLabel={t('common.yesDone')}
                  falseLabel={t('common.noFailed')}
                />
                <RadioPair
                  name="logStoragePassed"
                  label={t('sessions.manualLogLabel')}
                  description={t('sessions.manualLogDesc')}
                  register={register}
                  trueLabel={t('common.yesDone')}
                  falseLabel={t('common.noFailed')}
                />
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-12">
              <label className="form-label">{t('sessions.manualQcNote')}</label>
              <textarea className="form-control" rows={2} {...register('note')} />
            </div>
          </div>
          <div className="form-footer">
            <button type="submit" className="btn btn-primary">
              <i className="ri-save-line" /> {t('sessions.saveQc')}
            </button>
          </div>
        </form>
      </div>
    </>
  );
};

export default SessionManual;
