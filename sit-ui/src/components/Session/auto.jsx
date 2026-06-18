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

const IPN_LOGIC = [
  { step: 1, check: 'Kiểm tra chữ ký', param: 'vnp_SecureHash', rsp: '97' },
  { step: 2, check: 'Kiểm tra đơn hàng', param: 'vnp_TxnRef', rsp: '01' },
  { step: 3, check: 'Kiểm tra số tiền', param: 'vnp_Amount', rsp: '04' },
  { step: 4, check: 'Kiểm tra trạng thái đơn', param: 'status', rsp: '02' },
  { step: 5, check: 'Kiểm tra kết quả giao dịch', param: 'vnp_ResponseCode / vnp_TransactionStatus', rsp: '00' },
];

const RSP_CODES = [
  ['00', 'Thành công / Đã xác nhận'],
  ['01', 'Không tìm thấy đơn'],
  ['02', 'GD đã cập nhật'],
  ['04', 'Số tiền không khớp'],
  ['97', 'Sai chữ ký'],
  ['99', 'Lỗi khác (nghiệm thu thủ công)'],
];

const SUCCESS_ORDER_CASES = new Set(['WRONG_AMOUNT', 'SUCCESS']);

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

const txnRefsMustDiffer = (successTxnRef, failedTxnRef) => {
  if (!successTxnRef?.trim() || !failedTxnRef?.trim()) return null;
  if (successTxnRef.trim().toLowerCase() === failedTxnRef.trim().toLowerCase()) {
    return 'txnRef giao dịch 1 và giao dịch 2 phải khác nhau';
  }
  return null;
};

const validateOrderForCase = (caseValue, values) => {
  const { txnRef } = resolveOrderForCase(caseValue, values);
  if (SUCCESS_ORDER_CASES.has(caseValue) && !values.pendingTxnRef?.trim()) {
    return 'Nhập mã giao dịch (giao dịch 1)';
  }
  if (caseValue === 'FAILED' && !values.failedTxnRef?.trim()) {
    return 'Nhập mã giao dịch (giao dịch 2)';
  }
  if (caseValue === 'ORDER_ALREADY_CONFIRMED') {
    const hasConfirmed = Boolean(values.confirmedTxnRef?.trim());
    const hasSuccessOrder = Boolean(values.pendingTxnRef?.trim());
    if (!hasConfirmed && !hasSuccessOrder) {
      return 'Chạy Case 5 trước hoặc nhập txnRef đơn đã SUCCESS cho Case 4';
    }
  }
  const differError = txnRefsMustDiffer(values.pendingTxnRef, values.failedTxnRef);
  if (differError && (caseValue === 'FAILED' || caseValue === 'SUCCESS')) {
    return differError;
  }
  if (!txnRef) {
    return 'Nhập mã giao dịch (txnRef)';
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

  const { data: workspace, isLoading: workspaceLoading } = useSessionWorkspaceQuery(sessionId);
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
    const validationError = validateOrderForCase(caseValue, values);
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
        dispatch(appActions.flash(
          'Merchant trả RspCode 02 — đơn đã xử lý trước đó (đúng khi gọi lại IPN). Kết quả nghiệm thu giữ bản PASS trước đó.',
          'warning',
        ));
      } else {
        dispatch(appActions.flash(
          result.passed ? 'Kiểm thử PASS' : 'Kiểm thử FAIL',
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
      dispatch(appActions.flash('Nhập mã giao dịch (giao dịch 1) để chạy suite', 'danger'));
      return;
    }
    if (!values.failedTxnRef?.trim()) {
      dispatch(appActions.flash('Nhập mã giao dịch (giao dịch 2) để chạy suite', 'danger'));
      return;
    }
    const differError = txnRefsMustDiffer(values.pendingTxnRef, values.failedTxnRef);
    if (differError) {
      dispatch(appActions.flash(differError, 'danger'));
      return;
    }
    if (!values.pendingAmountVnd || Number(values.pendingAmountVnd) < 1) {
      dispatch(appActions.flash('Nhập số tiền (giao dịch 1) để chạy suite', 'danger'));
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
        result.allPassed ? 'Tất cả case IPN đã PASS' : `${result.passedSteps}/${result.totalSteps} case PASS`,
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

  if (workspaceLoading) {
    return (
      <div className="card-body">
        <div className="sit-list-loading">
          <i className="ri-loader-4-line" aria-hidden="true" />
          <span>{t('common.loading')}</span>
        </div>
      </div>
    );
  }

  if (!session || !metadata) return null;

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
                  {IPN_LOGIC.map((r) => (
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
            <p className="ipn-ref-note">
              <strong>Bước 5:</strong>
              {' '}
              nếu
              {' '}
              <code>vnp_ResponseCode</code>
              {' '}
              và
              {' '}
              <code>vnp_TransactionStatus</code>
              {' '}
              đều
              {' '}
              <code>00</code>
              {' '}
              → cập nhật SUCCESS, ngược lại FAIL {'->'} phản hồi RspCode 00 lại VNPAY
              {' '}
              {/* <code>00</code> */}
              {' '}
              khi đã nhận IPN.
            </p>
            <div className="rsp-code-legend">
              {RSP_CODES.map(([code, label]) => (
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
            Nhiều case đang nhận
            {' '}
            <strong>RspCode 01</strong>
            {' '}
            (không tìm thấy đơn). Kiểm tra lại
            {' '}
            <code>txnRef</code>
            /số tiền đã nhập — đơn phải được tạo trên merchant và dừng ở OTP (chưa thanh toán xong).
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} onBlur={saveCurrentInput}>
          <input type="hidden" {...register('partnerId')} />
          <input type="hidden" {...register('testCase')} />
          <input type="hidden" {...register('wrongAmountVnd')} />
          <input type="hidden" {...register('confirmedTxnRef')} />
          <input type="hidden" {...register('confirmedAmountVnd')} />

          <div className="order-input-group mb-3">
            <h4 className="order-input-title">Thông tin giao dịch</h4>
            <p className="order-input-desc">
              Tạo 2 giao dịch đến màn hình OTP dừng lại và điền txnRef, amount vào bảng dưới {'->'} bấm <strong>Tiến hành kiểm tra tự động</strong>.
            </p>
            <div className="order-prep-table">
              <div className="order-prep-head">
                <span />
                <span>Mã giao dịch (vnp_TxnRef)</span>
                <span>Số tiền (vnp_Amount)</span>
              </div>
              <div className="order-prep-row">
                <span className="order-prep-label">Giao dịch 1</span>
                <input
                  className="form-control form-control-sm"
                  placeholder="TxnRef giao dịch 1"
                  {...register('pendingTxnRef')}
                />
                <input
                  type="number"
                  className="form-control form-control-sm"
                  placeholder="amount giao dịch 1"
                  {...register('pendingAmountVnd', { required: true, min: 1 })}
                />
              </div>
              <div className="order-prep-row">
                <span className="order-prep-label">Giao dịch 2</span>
                <input
                  className="form-control form-control-sm"
                  placeholder="TxnRef giao dịch 2"
                  {...register('failedTxnRef')}
                />
                <input
                  type="number"
                  className="form-control form-control-sm"
                  placeholder="amount giao dịch 2"
                  {...register('failedAmountVnd', { min: 1 })}
                />
              </div>
            </div>
          </div>

          <div className="form-footer">
            <button type="button" className="btn btn-primary" onClick={onRunIpnSuite}>
              <i className="ri-list-check-2" /> Tiến hành kiểm tra tự động
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
