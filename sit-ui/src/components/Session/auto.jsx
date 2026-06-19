import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import SessionHeader from './SessionHeader';
import AcceptanceTabs from './AcceptanceTabs';
import TestCasePanel from './TestCasePanel';
import {
  latestRunsByCase,
  useRunIpnSuiteMutation,
  useRunTestMutation,
  useSaveSessionTestInputMutation,
  useSessionWorkspaceQuery,
} from '../../api/hooks';
import { appActions } from '../../stores';
import { useI18n } from '../../i18n/useI18n';

const SUCCESS_ORDER_CASES = new Set(['WRONG_AMOUNT', 'SUCCESS']);

const buildIpnLogic = (t) => [
  { step: 1, check: t('sessions.ipnLogicStep1'), param: 'vnp_SecureHash', rsp: '97' },
  { step: 2, check: t('sessions.ipnLogicStep2'), param: 'vnp_TxnRef', rsp: '01' },
  { step: 3, check: t('sessions.ipnLogicStep3'), param: 'vnp_Amount', rsp: '04' },
  { step: 4, check: t('sessions.ipnLogicStep4'), param: 'status', rsp: '02' },
  { step: 5, check: t('sessions.ipnLogicStep5'), param: 'vnp_ResponseCode / vnp_TransactionStatus', rsp: '00' },
];

const buildRspCodes = (t) => [
  ['00', t('sessions.rspCode00')],
  ['01', t('sessions.rspCode01')],
  ['02', t('sessions.rspCode02')],
  ['04', t('sessions.rspCode04')],
  ['97', t('sessions.rspCode97')],
  ['99', t('sessions.rspCode99')],
];

/** Chọn txnRef/amount theo case — mỗi kịch bản IPN dùng đơn riêng khi cần. */
const resolveOrderForCase = (caseValue, values) => {
  const pendingTxnRef = values.pendingTxnRef?.trim() || '';
  const pendingAmountVnd = Number(values.pendingAmountVnd) || 100000;
  const failedTxnRef = values.failedTxnRef?.trim() || '';
  const failedAmountVnd = Number(values.failedAmountVnd) || pendingAmountVnd;
  const confirmedTxnRef = values.confirmedTxnRef?.trim() || '';
  const confirmedAmountVnd = Number(values.confirmedAmountVnd) || pendingAmountVnd;

  if (caseValue === 'ORDER_ALREADY_CONFIRMED') {
    if (confirmedTxnRef) {
      return { txnRef: confirmedTxnRef, amountVnd: confirmedAmountVnd };
    }
    if (pendingTxnRef) {
      return { txnRef: pendingTxnRef, amountVnd: pendingAmountVnd };
    }
  }
  if (caseValue === 'FAILED' && failedTxnRef) {
    return { txnRef: failedTxnRef, amountVnd: failedAmountVnd };
  }
  if (SUCCESS_ORDER_CASES.has(caseValue) && pendingTxnRef) {
    return { txnRef: pendingTxnRef, amountVnd: pendingAmountVnd };
  }
  if (pendingTxnRef) {
    return { txnRef: pendingTxnRef, amountVnd: pendingAmountVnd };
  }
  if (caseValue === 'INVALID_HASH' || caseValue === 'ORDER_NOT_FOUND') {
    return { txnRef: 'SIT_DUMMY', amountVnd: pendingAmountVnd };
  }
  return { txnRef: '', amountVnd: pendingAmountVnd };
};

const txnRefsMustDiffer = (successTxnRef, failedTxnRef, t) => {
  if (!successTxnRef?.trim() || !failedTxnRef?.trim()) return null;
  if (successTxnRef.trim().toLowerCase() === failedTxnRef.trim().toLowerCase()) {
    return t('sessions.errTxnRefsDiffer');
  }
  return null;
};

const validateOrderForCase = (caseValue, values, t) => {
  const { txnRef } = resolveOrderForCase(caseValue, values);
  if (SUCCESS_ORDER_CASES.has(caseValue) && !values.pendingTxnRef?.trim()) {
    return t('sessions.errTxn1Required');
  }
  if (caseValue === 'FAILED' && !values.failedTxnRef?.trim()) {
    return t('sessions.errTxn2Required');
  }
  if (caseValue === 'ORDER_ALREADY_CONFIRMED') {
    const hasConfirmed = Boolean(values.confirmedTxnRef?.trim());
    const hasSuccessOrder = Boolean(values.pendingTxnRef?.trim());
    if (!hasConfirmed && !hasSuccessOrder) {
      return t('sessions.errCase4Prereq');
    }
  }
  const differError = txnRefsMustDiffer(values.pendingTxnRef, values.failedTxnRef, t);
  if (differError && (caseValue === 'FAILED' || caseValue === 'SUCCESS')) {
    return differError;
  }
  if (!txnRef) {
    return t('sessions.errTxnRefRequired');
  }
  return null;
};

const toTestInputPayload = (values) => ({
  pendingTxnRef: values.pendingTxnRef?.trim() ?? '',
  pendingAmountVnd: values.pendingAmountVnd ? Number(values.pendingAmountVnd) : null,
  failedTxnRef: values.failedTxnRef?.trim() ?? '',
  failedAmountVnd: values.failedAmountVnd ? Number(values.failedAmountVnd) : null,
  confirmedTxnRef: values.confirmedTxnRef?.trim() ?? '',
  confirmedAmountVnd: values.confirmedAmountVnd ? Number(values.confirmedAmountVnd) : null,
  wrongAmountVnd: values.wrongAmountVnd ? Number(values.wrongAmountVnd) : null,
});

const SessionAuto = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { t } = useI18n();
  const [runningCase, setRunningCase] = useState(null);
  const { register, handleSubmit, getValues, reset, setValue } = useForm();
  const initializedSessionId = useRef(null);
  const formReadyRef = useRef(false);

  const ipnLogic = useMemo(() => buildIpnLogic(t), [t]);
  const rspCodes = useMemo(() => buildRspCodes(t), [t]);

  const { data: workspace } = useSessionWorkspaceQuery(sessionId);
  const session = workspace?.session;
  const metadata = workspace ? { testCases: workspace.testCases } : null;
  const runTest = useRunTestMutation(sessionId);
  const runIpnSuite = useRunIpnSuiteMutation(sessionId);
  const saveTestInput = useSaveSessionTestInputMutation(sessionId);

  useEffect(() => {
    initializedSessionId.current = null;
    formReadyRef.current = false;
  }, [sessionId]);

  const runsByCase = useMemo(
    () => latestRunsByCase(workspace?.latestRuns),
    [workspace?.latestRuns],
  );

  useEffect(() => {
    if (!session || !metadata) return;
    if (initializedSessionId.current === session.id) return;

    initializedSessionId.current = session.id;
    formReadyRef.current = false;

    reset({
      partnerId: session.partnerId,
      sessionId: session.id,
      pendingTxnRef: session.pendingTxnRef || '',
      pendingAmountVnd: session.pendingAmountVnd ?? '',
      failedTxnRef: session.failedTxnRef || '',
      failedAmountVnd: session.failedAmountVnd ?? '',
      confirmedTxnRef: session.confirmedTxnRef || '',
      confirmedAmountVnd: session.confirmedAmountVnd ?? '',
      wrongAmountVnd: session.wrongAmountVnd ?? '',
      testCase: 'SUCCESS',
    });

    requestAnimationFrame(() => {
      formReadyRef.current = true;
    });
  }, [session, metadata, reset]);

  const saveCurrentInput = () => {
    if (!formReadyRef.current) return;
    saveTestInput.mutate(toTestInputPayload(getValues()));
  };

  const persistTestInput = async () => {
    if (!sessionId) return;
    await saveTestInput.mutateAsync(toTestInputPayload(getValues()));
  };

  const resolveWrongAmountVnd = (values, caseValue) => {
    if (values.wrongAmountVnd) return Number(values.wrongAmountVnd);
    if (caseValue === 'WRONG_AMOUNT') {
      const pendingAmount = Number(values.pendingAmountVnd) || 100000;
      return pendingAmount + 1000;
    }
    return null;
  };

  const buildPayload = (values, caseValue) => {
    const resolvedCase = caseValue ?? values.testCase;
    const order = resolveOrderForCase(resolvedCase, values);
    return {
      partnerId: Number(session.partnerId),
      sessionId: Number(sessionId),
      callbackType: 'IPN',
      testCase: resolvedCase,
      txnRef: order.txnRef,
      amountVnd: order.amountVnd,
      wrongAmountVnd: resolveWrongAmountVnd(values, resolvedCase),
    };
  };

  const runSingleCase = async (caseValue) => {
    const values = getValues();
    const validationError = validateOrderForCase(caseValue, values, t);
    if (validationError) {
      dispatch(appActions.flash(validationError, 'danger'));
      return;
    }
    setRunningCase(caseValue);
    try {
      await persistTestInput();
      const result = await runTest.mutateAsync({
        ...buildPayload(values, caseValue),
      });
      if (caseValue === 'SUCCESS' && result.passed) {
        const order = resolveOrderForCase('SUCCESS', getValues());
        setValue('confirmedTxnRef', order.txnRef);
        setValue('confirmedAmountVnd', order.amountVnd);
        await saveTestInput.mutateAsync(toTestInputPayload(getValues()));
      }
      const orderAlreadyProcessed = !result.passed
        && result.actualRspCode === '02'
        && (caseValue === 'SUCCESS' || caseValue === 'FAILED');
      if (orderAlreadyProcessed) {
        dispatch(appActions.flash(t('sessions.rsp02Warning'), 'warning'));
      } else {
        dispatch(appActions.flash(
          result.passed ? t('sessions.testPass') : t('sessions.testFail'),
          result.passed ? 'success' : 'danger',
        ));
      }
    } catch {
      // mutation cache handles API errors
    } finally {
      setRunningCase(null);
    }
  };

  const onSubmit = async (values) => {
    await runSingleCase(values.testCase);
  };

  const onRunIpnSuite = async () => {
    const values = getValues();
    if (!values.pendingTxnRef?.trim()) {
      dispatch(appActions.flash(t('sessions.errSuiteTxn1'), 'danger'));
      return;
    }
    if (!values.failedTxnRef?.trim()) {
      dispatch(appActions.flash(t('sessions.errSuiteTxn2'), 'danger'));
      return;
    }
    const differError = txnRefsMustDiffer(values.pendingTxnRef, values.failedTxnRef, t);
    if (differError) {
      dispatch(appActions.flash(differError, 'danger'));
      return;
    }
    if (!values.pendingAmountVnd || Number(values.pendingAmountVnd) < 1) {
      dispatch(appActions.flash(t('sessions.errSuiteAmount'), 'danger'));
      return;
    }
    try {
      await persistTestInput();
      const result = await runIpnSuite.mutateAsync({
        data: {
          partnerId: Number(session.partnerId),
          sessionId: Number(sessionId),
          txnRef: values.pendingTxnRef.trim(),
          amountVnd: Number(values.pendingAmountVnd),
          failedTxnRef: values.failedTxnRef.trim(),
          failedAmountVnd: values.failedAmountVnd ? Number(values.failedAmountVnd) : null,
          wrongAmountVnd: resolveWrongAmountVnd(values, 'WRONG_AMOUNT'),
        },
        config: { timeout: 180000 },
      });
      dispatch(appActions.flash(
        result.allPassed
          ? t('sessions.suiteAllPass')
          : t('sessions.suitePartialPass', { passed: result.passedSteps, total: result.totalSteps }),
        result.allPassed ? 'success' : 'danger',
      ));
      navigate(`/sessions/${sessionId}/suite-result`);
    } catch {
      // mutation cache handles API errors
    }
  };

  const hasUnexpectedOrderNotFound = Object.entries(runsByCase).some(
    ([caseValue, run]) => run.actualRspCode === '01'
      && caseValue !== 'ORDER_NOT_FOUND'
      && run.expectedRspCode !== '01',
  );

  if (!session || !metadata) {
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
        <SessionHeader session={session} onRerunAll={onRunIpnSuite} />
      </div>
      <AcceptanceTabs />

      <div className="card-body pt-0">
        <details className="ipn-ref-panel mb-3">
          <summary className="ipn-ref-summary">
            <i className="ri-book-open-line" />
            {t('sessions.ipnRefTitle')}
          </summary>
          <div className="ipn-ref-body">
            <div className="sit-list-table-wrap">
              <table className="table data-table sit-data-table ipn-logic-table mb-0">
                <colgroup>
                  <col className="col-ipn-step" />
                  <col className="col-ipn-check" />
                  <col className="col-ipn-param" />
                  <col className="col-ipn-rsp" />
                </colgroup>
                <thead>
                  <tr>
                    <th>{t('sessions.ipnStep')}</th>
                    <th>{t('sessions.ipnCheck')}</th>
                    <th>{t('sessions.ipnParam')}</th>
                    <th>{t('sessions.ipnRsp')}</th>
                  </tr>
                </thead>
                <tbody>
                  {ipnLogic.map((r) => (
                    <tr key={r.step}>
                      <td className="ipn-col-step">{r.step}</td>
                      <td>{r.check}</td>
                      <td className="ipn-col-param"><code>{r.param}</code></td>
                      <td className="ipn-col-rsp"><strong>{r.rsp}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="ipn-ref-note sit-page-subtitle mb-2">
              {t('sessions.ipnStep5Note')}
            </p>
            <div className="rsp-code-legend">
              {rspCodes.map(([code, label]) => (
                <span key={code} className="rsp-code-item">
                  <strong>{code}</strong>
                  {' '}
                  —
                  {' '}
                  {label}
                </span>
              ))}
            </div>
          </div>
        </details>

        {hasUnexpectedOrderNotFound && (
          <div className="alert alert-danger mb-3" style={{ fontSize: 13 }}>
            {t('sessions.unexpectedOrderNotFound')}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} onBlur={saveCurrentInput}>
          <input type="hidden" {...register('partnerId')} />
          <input type="hidden" {...register('testCase')} />
          <input type="hidden" {...register('wrongAmountVnd')} />
          <input type="hidden" {...register('confirmedTxnRef')} />
          <input type="hidden" {...register('confirmedAmountVnd')} />

          <div className="order-input-group mb-3">
            <h4 className="order-input-title">{t('sessions.orderTitle')}</h4>
            <p className="order-input-desc sit-page-subtitle">
              {t('sessions.orderDesc')}
            </p>
            <div className="order-prep-table">
              <div className="order-prep-head">
                <span />
                <span>{t('sessions.orderTxnRefCol')}</span>
                <span>{t('sessions.orderAmountCol')}</span>
              </div>
              <div className="order-prep-row">
                <span className="order-prep-label">{t('sessions.order1')}</span>
                <input
                  className="form-control form-control-sm"
                  placeholder={t('sessions.orderTxnRef1Placeholder')}
                  {...register('pendingTxnRef')}
                />
                <input
                  type="number"
                  className="form-control form-control-sm"
                  placeholder={t('sessions.orderAmount1Placeholder')}
                  {...register('pendingAmountVnd', { required: true, min: 1 })}
                />
              </div>
              <div className="order-prep-row">
                <span className="order-prep-label">{t('sessions.order2')}</span>
                <input
                  className="form-control form-control-sm"
                  placeholder={t('sessions.orderTxnRef2Placeholder')}
                  {...register('failedTxnRef')}
                />
                <input
                  type="number"
                  className="form-control form-control-sm"
                  placeholder={t('sessions.orderAmount2Placeholder')}
                  {...register('failedAmountVnd', { min: 1 })}
                />
              </div>
            </div>
          </div>

          <div className="form-footer">
            <button type="button" className="btn btn-primary" onClick={onRunIpnSuite}>
              <i className="ri-list-check-2" />
              {' '}
              {t('sessions.runSuite')}
            </button>
          </div>
        </form>

        <TestCasePanel
          testCases={metadata.testCases}
          runsByCase={runsByCase}
          autoPassed={session.autoPassed}
          autoTotal={session.autoTotal}
          onRunCase={(caseValue) => {
            setValue('testCase', caseValue);
            runSingleCase(caseValue);
          }}
          runningCase={runningCase}
        />
      </div>
    </>
  );
};

export default SessionAuto;
