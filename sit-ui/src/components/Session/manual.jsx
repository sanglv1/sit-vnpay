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
  useSessionWorkspaceQuery,
} from '../../api/hooks';
import { appActions } from '../../stores';

const TOKEN_SCENARIOS = [
  { key: 'TOKEN_CREATE_SUCCESS', titleKey: 'sessions.tokenScenario1' },
  { key: 'TOKEN_CREATE_FAILED', titleKey: 'sessions.tokenScenario2' },
  { key: 'PAY_AND_CREATE_SUCCESS', titleKey: 'sessions.tokenScenario3' },
  { key: 'PAY_AND_CREATE_FAILED', titleKey: 'sessions.tokenScenario4' },
  { key: 'TOKEN_PAY_SUCCESS', titleKey: 'sessions.tokenScenario5' },
  { key: 'TOKEN_PAY_FAILED', titleKey: 'sessions.tokenScenario6' },
  { key: 'TOKEN_REMOVE_SUCCESS', titleKey: 'sessions.tokenScenario7' },
  { key: 'TOKEN_REMOVE_FAILED', titleKey: 'sessions.tokenScenario8' },
];

const RECURRING_SCENARIOS = [
  { key: 'TOKEN_AUTH_SUCCESS', titleKey: 'sessions.recurringScenario1' },
  { key: 'TOKEN_AUTH_FAILED', titleKey: 'sessions.recurringScenario2' },
  { key: 'REGISTER_SUCCESS', titleKey: 'sessions.recurringScenario3' },
  { key: 'REGISTER_FAILED', titleKey: 'sessions.recurringScenario4' },
  { key: 'CARD_VERIFY_SUCCESS', titleKey: 'sessions.recurringScenario5', showScreenshot: true },
  { key: 'CARD_VERIFY_FAILED', titleKey: 'sessions.recurringScenario6', showScreenshot: true },
  { key: 'RECURRING_PAY_SUCCESS', titleKey: 'sessions.recurringScenario7' },
  { key: 'RECURRING_PAY_FAILED', titleKey: 'sessions.recurringScenario8' },
  { key: 'UPDATE_CARD_SUCCESS', titleKey: 'sessions.recurringScenario9' },
  { key: 'UPDATE_CARD_FAILED', titleKey: 'sessions.recurringScenario10' },
  { key: 'UPDATE_PERIOD_SUCCESS', titleKey: 'sessions.recurringScenario11' },
  { key: 'UPDATE_PERIOD_FAILED', titleKey: 'sessions.recurringScenario12' },
  { key: 'CANCEL_REGISTER_SUCCESS', titleKey: 'sessions.recurringScenario13' },
  { key: 'CANCEL_REGISTER_FAILED', titleKey: 'sessions.recurringScenario14' },
];

const INSTALMENT_SCENARIOS = [
  { key: 'TOKEN_AUTH_SUCCESS', titleKey: 'sessions.instalmentScenario1' },
  { key: 'TOKEN_AUTH_FAILED', titleKey: 'sessions.instalmentScenario2' },
  { key: 'QUERY_CONFIG_SUCCESS', titleKey: 'sessions.instalmentScenario3', showScreenshot: true },
  { key: 'QUERY_CONFIG_FAILED', titleKey: 'sessions.instalmentScenario4' },
  { key: 'CREATE_TXN_SUCCESS', titleKey: 'sessions.instalmentScenario5' },
  { key: 'CREATE_TXN_FAILED', titleKey: 'sessions.instalmentScenario6' },
  { key: 'PAY_SUCCESS', titleKey: 'sessions.instalmentScenario7', showScreenshot: true },
  { key: 'PAY_FAILED', titleKey: 'sessions.instalmentScenario8', showScreenshot: true },
];

const EMPTY_SCENARIO_EVIDENCE = { requestLog: '', responseLog: '', image: '' };

const emptyScenarioEvidence = (scenarios) =>
  scenarios.reduce((acc, { key }) => {
    acc[key] = { requestLog: '', responseLog: '', image: '' };
    return acc;
  }, {});

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

const TokenScenarioBlock = ({
  title,
  evidence,
  onRequestLogChange,
  onResponseLogChange,
  onImageChange,
  requestLogLabel,
  requestLogPlaceholder,
  responseLogLabel,
  responseLogPlaceholder,
  screenshotLabel,
  showScreenshot = true,
}) => {
  const safeEvidence = evidence ?? EMPTY_SCENARIO_EVIDENCE;
  return (
  <div className="manual-subsection manual-token-scenario">
    <div className="manual-subtitle">{title}</div>
    <label className="form-label">{requestLogLabel}</label>
    <textarea
      className="form-control mb-2 font-monospace"
      rows={4}
      value={safeEvidence.requestLog || ''}
      onChange={(e) => onRequestLogChange(e.target.value)}
      placeholder={requestLogPlaceholder}
    />
    <label className="form-label">{responseLogLabel}</label>
    <textarea
      className="form-control mb-2 font-monospace"
      rows={4}
      value={safeEvidence.responseLog || ''}
      onChange={(e) => onResponseLogChange(e.target.value)}
      placeholder={responseLogPlaceholder}
    />
    {showScreenshot && (
      <ImageUpload
        label={screenshotLabel}
        preview={safeEvidence.image || null}
        onChange={onImageChange}
      />
    )}
  </div>
  );
};

const SessionManual = () => {
  const { sessionId } = useParams();
  const dispatch = useDispatch();
  const { t } = useI18n();
  const [successPreview, setSuccessPreview] = useState(null);
  const [failedPreview, setFailedPreview] = useState(null);
  const [scenarioEvidence, setScenarioEvidence] = useState({});
  const { register, handleSubmit, reset, setValue } = useForm();
  const { data: session } = useSessionQuery(sessionId);
  const { data: workspace } = useSessionWorkspaceQuery(sessionId, { enabled: Boolean(sessionId) });
  const isTokenFlow = workspace?.partnerFlow === 'TOKEN';
  const isRecurringFlow = workspace?.partnerFlow === 'RECURRING';
  const isInstalmentFlow = workspace?.partnerFlow === 'INSTALMENT';
  const flowScenarios = isTokenFlow
    ? TOKEN_SCENARIOS
    : isRecurringFlow
      ? RECURRING_SCENARIOS
      : isInstalmentFlow
        ? INSTALMENT_SCENARIOS
        : null;
  const { data: acceptance, isFetched: acceptanceLoaded } = useManualAcceptanceQuery(sessionId, {
    enabled: Boolean(sessionId),
  });
  const saveAcceptance = useSaveManualAcceptanceMutation(sessionId);

  useEffect(() => {
    if (!flowScenarios) return;
    setScenarioEvidence((prev) => {
      const next = emptyScenarioEvidence(flowScenarios);
      Object.keys(next).forEach((key) => {
        if (prev[key]) {
          next[key] = prev[key];
        }
      });
      return next;
    });
  }, [flowScenarios]);

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
      if (flowScenarios) {
        setScenarioEvidence(emptyScenarioEvidence(flowScenarios));
      }
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
    if (flowScenarios) {
      const merged = emptyScenarioEvidence(flowScenarios);
      const source = isTokenFlow
        ? acceptance.tokenScenarioEvidence
        : isRecurringFlow
          ? acceptance.recurringScenarioEvidence
          : acceptance.instalmentScenarioEvidence;
      if (source) {
        Object.entries(source).forEach(([key, value]) => {
          if (merged[key] && value) {
            merged[key] = {
              requestLog: value.requestLog || '',
              responseLog: value.responseLog || '',
              image: value.image || '',
            };
          }
        });
      }
      setScenarioEvidence(merged);
    }
  }, [session, acceptance, acceptanceLoaded, reset, flowScenarios, isTokenFlow, isRecurringFlow]);

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

  const onTokenImageChange = async (scenarioKey, e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      dispatch(appActions.flash(t('sessions.manualImageMaxSize'), 'danger'));
      return;
    }
    const dataUrl = await readImageAsDataUrl(file);
    setScenarioEvidence((prev) => ({
      ...prev,
      [scenarioKey]: { ...(prev[scenarioKey] ?? EMPTY_SCENARIO_EVIDENCE), image: dataUrl },
    }));
  };

  const updateScenarioField = (scenarioKey, field, value) => {
    setScenarioEvidence((prev) => ({
      ...prev,
      [scenarioKey]: { ...(prev[scenarioKey] ?? EMPTY_SCENARIO_EVIDENCE), [field]: value },
    }));
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
      if (isTokenFlow) {
        payload.tokenScenarioEvidence = scenarioEvidence;
      }
      if (isRecurringFlow) {
        payload.recurringScenarioEvidence = scenarioEvidence;
      }
      if (isInstalmentFlow) {
        payload.instalmentScenarioEvidence = scenarioEvidence;
      }
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

          {flowScenarios ? (
            <div className="manual-section mb-4">
              <h4 className="manual-section-title">
                {isTokenFlow
                  ? t('sessions.manualTokenSection')
                  : isRecurringFlow
                    ? t('sessions.manualRecurringSection')
                    : t('sessions.manualInstalmentSection')}
              </h4>
              <p className="manual-check-desc mb-3">
                {isTokenFlow
                  ? t('sessions.manualTokenEvidenceHint')
                  : isRecurringFlow
                    ? t('sessions.manualRecurringEvidenceHint')
                    : t('sessions.manualInstalmentEvidenceHint')}
              </p>
              {flowScenarios.map((scenario) => {
                const { key, titleKey, showScreenshot } = scenario;
                return (
                <TokenScenarioBlock
                  key={key}
                  title={t(titleKey)}
                  evidence={scenarioEvidence[key]}
                  requestLogLabel={
                    isTokenFlow
                      ? t('sessions.manualTokenRequestLog')
                      : isRecurringFlow
                        ? t('sessions.manualRecurringRequestLog')
                        : t('sessions.manualInstalmentRequestLog')
                  }
                  requestLogPlaceholder={
                    isTokenFlow
                      ? t('sessions.manualTokenRequestLogPlaceholder')
                      : isRecurringFlow
                        ? t('sessions.manualRecurringRequestLogPlaceholder')
                        : t('sessions.manualInstalmentRequestLogPlaceholder')
                  }
                  responseLogLabel={
                    isTokenFlow
                      ? t('sessions.manualTokenResponseLog')
                      : isRecurringFlow
                        ? t('sessions.manualRecurringResponseLog')
                        : t('sessions.manualInstalmentResponseLog')
                  }
                  responseLogPlaceholder={
                    isTokenFlow
                      ? t('sessions.manualTokenResponseLogPlaceholder')
                      : isRecurringFlow
                        ? t('sessions.manualRecurringResponseLogPlaceholder')
                        : t('sessions.manualInstalmentResponseLogPlaceholder')
                  }
                  screenshotLabel={
                    isInstalmentFlow
                      ? t('sessions.manualInstalmentScreenshot')
                      : isTokenFlow
                        ? t('sessions.manualTokenScreenshot')
                        : t('sessions.manualRecurringScreenshot')
                  }
                  showScreenshot={isTokenFlow || showScreenshot === true}
                  onRequestLogChange={(value) => updateScenarioField(key, 'requestLog', value)}
                  onResponseLogChange={(value) => updateScenarioField(key, 'responseLog', value)}
                  onImageChange={(e) => onTokenImageChange(key, e)}
                />
                );
              })}
            </div>
          ) : (
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
            </div>
          )}

          <div className="row manual-columns">
            <div className={flowScenarios ? 'col-12' : 'col-lg-6'}>
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
