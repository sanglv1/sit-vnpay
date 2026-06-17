import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import SessionHeader from './SessionHeader';
import AcceptanceTabs from './AcceptanceTabs';
import TestCasePanel from './TestCasePanel';
import {
  latestRunsByCase,
  usePrepareMerchantOrderMutation,
  useRunIpnSuiteMutation,
  useRunTestMutation,
  useSaveSessionTestInputMutation,
  useSessionWorkspaceQuery,
} from '../../api/hooks';
import { appActions } from '../../stores';

const IPN_LOGIC = [
  { step: 1, check: 'Kiểm tra chữ ký', param: 'vnp_SecureHash', caseNo: 'Case 1', rsp: '97' },
  { step: 2, check: 'Kiểm tra đơn hàng', param: 'vnp_TxnRef', caseNo: 'Case 2', rsp: '01' },
  { step: 3, check: 'Kiểm tra số tiền', param: 'vnp_Amount', caseNo: 'Case 3', rsp: '04' },
  { step: 4, check: 'Kiểm tra trạng thái đơn', param: 'status', caseNo: 'Case 4', rsp: '02' },
  { step: 5, check: 'Kiểm tra kết quả giao dịch', param: 'vnp_ResponseCode / vnp_TransactionStatus', caseNo: 'Case 5', rsp: '00' },
];

const PENDING_ORDER_CASES = new Set(['WRONG_AMOUNT', 'SUCCESS', 'FAILED']);

/** Chọn txnRef/amount theo case — Case 4 ưu tiên đơn đã SUCCESS nếu có. */
const resolveOrderForCase = (caseValue, values) => {
  const pendingTxnRef = values.pendingTxnRef?.trim() || '';
  const pendingAmountVnd = Number(values.pendingAmountVnd) || 100000;
  const confirmedTxnRef = values.confirmedTxnRef?.trim() || '';
  const confirmedAmountVnd = Number(values.confirmedAmountVnd) || pendingAmountVnd;

  if (caseValue === 'ORDER_ALREADY_CONFIRMED' && confirmedTxnRef) {
    return { txnRef: confirmedTxnRef, amountVnd: confirmedAmountVnd };
  }
  if (pendingTxnRef) {
    return { txnRef: pendingTxnRef, amountVnd: pendingAmountVnd };
  }
  if (caseValue === 'INVALID_HASH' || caseValue === 'ORDER_NOT_FOUND') {
    return { txnRef: 'SIT_DUMMY', amountVnd: pendingAmountVnd };
  }
  return { txnRef: '', amountVnd: pendingAmountVnd };
};

const validateOrderForCase = (caseValue, values) => {
  const { txnRef } = resolveOrderForCase(caseValue, values);
  if (PENDING_ORDER_CASES.has(caseValue) && !values.pendingTxnRef?.trim()) {
    return 'Nhập mã giao dịch đơn chờ thanh toán (PENDING)';
  }
  if (caseValue === 'ORDER_ALREADY_CONFIRMED') {
    const hasConfirmed = Boolean(values.confirmedTxnRef?.trim());
    const hasPending = Boolean(values.pendingTxnRef?.trim());
    if (!hasConfirmed && !hasPending) {
      return 'Nhập txnRef đơn đã thanh toán (SUCCESS) hoặc đơn PENDING đã chạy Case 5 trước đó';
    }
  }
  if (!txnRef) {
    return 'Nhập mã giao dịch (txnRef)';
  }
  return null;
};

const toTestInputPayload = (values) => ({
  pendingTxnRef: values.pendingTxnRef?.trim() ?? '',
  pendingAmountVnd: values.pendingAmountVnd ? Number(values.pendingAmountVnd) : null,
  confirmedTxnRef: values.confirmedTxnRef?.trim() ?? '',
  confirmedAmountVnd: values.confirmedAmountVnd ? Number(values.confirmedAmountVnd) : null,
  wrongAmountVnd: values.wrongAmountVnd ? Number(values.wrongAmountVnd) : null,
});

const SessionAuto = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [runningCase, setRunningCase] = useState(null);
  const { register, handleSubmit, watch, getValues, reset, setValue } = useForm();
  const testCase = watch('testCase');
  const initializedSessionId = useRef(null);
  const formReadyRef = useRef(false);

  const { data: workspace } = useSessionWorkspaceQuery(sessionId);
  const session = workspace?.session;
  const metadata = workspace ? { testCases: workspace.testCases } : null;
  const runTest = useRunTestMutation(sessionId);
  const runIpnSuite = useRunIpnSuiteMutation(sessionId);
  const prepareOrder = usePrepareMerchantOrderMutation();
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

  const buildPayload = (values, caseValue) => {
    const order = resolveOrderForCase(caseValue ?? values.testCase, values);
    return {
      partnerId: Number(session.partnerId),
      sessionId: Number(sessionId),
      callbackType: 'IPN',
      testCase: caseValue ?? values.testCase,
      txnRef: order.txnRef,
      amountVnd: order.amountVnd,
      wrongAmountVnd: values.wrongAmountVnd ? Number(values.wrongAmountVnd) : null,
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
      dispatch(appActions.flash(
        result.passed ? 'Kiểm thử PASS' : 'Kiểm thử FAIL',
        result.passed ? 'success' : 'danger',
      ));
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
      dispatch(appActions.flash('Nhập mã giao dịch đơn chờ thanh toán (PENDING) để chạy suite', 'danger'));
      return;
    }
    if (!values.pendingAmountVnd || Number(values.pendingAmountVnd) < 1) {
      dispatch(appActions.flash('Nhập số tiền đơn chờ thanh toán (PENDING) để chạy suite', 'danger'));
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
          wrongAmountVnd: values.wrongAmountVnd ? Number(values.wrongAmountVnd) : null,
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

  const onPrepareMerchantOrder = async () => {
    const amountVnd = Number(getValues('pendingAmountVnd'));
    if (!amountVnd || amountVnd < 1) {
      dispatch(appActions.flash('Nhập số tiền đơn chờ thanh toán (PENDING) trước khi tạo đơn', 'danger'));
      return;
    }
    try {
      const result = await prepareOrder.mutateAsync({
        partnerId: Number(session.partnerId),
        amountVnd,
      });
      setValue('pendingTxnRef', result.txnRef);
      setValue('pendingAmountVnd', result.amountVnd);
      await saveTestInput.mutateAsync({
        pendingTxnRef: result.txnRef,
        pendingAmountVnd: result.amountVnd,
        confirmedTxnRef: getValues('confirmedTxnRef')?.trim() ?? '',
        confirmedAmountVnd: getValues('confirmedAmountVnd')
          ? Number(getValues('confirmedAmountVnd'))
          : null,
        wrongAmountVnd: getValues('wrongAmountVnd') ? Number(getValues('wrongAmountVnd')) : null,
      });
      dispatch(appActions.flash(
        `Đã tạo đơn PENDING: txnRef=${result.txnRef}, amount=${result.amountVnd} VND`,
        'success',
      ));
    } catch {
      // mutation cache handles API errors
    }
  };

  const hasUnexpectedOrderNotFound = Object.entries(runsByCase).some(
    ([caseValue, run]) => run.actualRspCode === '01'
      && caseValue !== 'ORDER_NOT_FOUND'
      && run.expectedRspCode !== '01',
  );

  if (!session || !metadata) return null;

  return (
    <>
      <div className="card-body pb-0">
        <SessionHeader session={session} onRerunAll={onRunIpnSuite} />
      </div>
      <AcceptanceTabs />

      <div className="card-body pt-0">
        <h4 className="card-title" style={{ fontSize: 15 }}>Logic điều kiện kiểm tra ở đầu IPN</h4>
        <div className="table-wrap mb-3">
          <table className="table table-striped data-table">
            <colgroup>
              <col className="col-step" />
              <col />
              <col />
              <col className="col-case" />
              <col className="col-rsp" />
            </colgroup>
            <thead>
              <tr>
                <th className="text-center">Bước</th>
                <th>Điều kiện kiểm tra</th>
                <th>Tham số liên quan</th>
                <th className="text-center">Case</th>
                <th className="text-center">RspCode</th>
              </tr>
            </thead>
            <tbody>
              {IPN_LOGIC.map((r) => (
                <tr key={r.step}>
                  <td className="text-center">{r.step}</td>
                  <td>{r.check}</td>
                  <td><code>{r.param}</code></td>
                  <td className="text-center">{r.caseNo}</td>
                  <td className="text-center"><strong>{r.rsp}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="row mb-3">
          <div className="col-lg-6">
            <div className="manual-check-card">
              <div className="manual-check-title">Chi tiết bước 5 — Kiểm tra kết quả giao dịch</div>
              <pre className="code-block mb-0">{`if (vnp_ResponseCode == "00" && vnp_TransactionStatus == "00")
  → cập nhật SUCCESS
else
  → cập nhật FAIL
// VNPay yêu cầu trả RspCode "00" khi đã nhận IPN`}</pre>
            </div>
          </div>
          <div className="col-lg-6">
            <div className="manual-check-card">
              <div className="manual-check-title">Quy tắc trả RspCode</div>
              <ul className="rsp-rules">
                <li><strong>00</strong> — Thành công / Đã xác nhận</li>
                <li><strong>01</strong> — Không tìm thấy đơn</li>
                <li><strong>02</strong> — GD đã cập nhật</li>
                <li><strong>04</strong> — Số tiền không khớp</li>
                <li><strong>97</strong> — Sai chữ ký</li>
                <li><strong>99</strong> — Lỗi khác (nghiệm thu thủ công)</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="alert alert-danger mb-3" style={{ fontSize: 13 }}>
          <strong>Lưu ý:</strong>
          {' '}
          Case 1 (97) và Case 2 (01) không cần đơn thật. Case 3, 5, 6 và
          {' '}
          <strong>suite tự động</strong>
          {' '}
          dùng
          {' '}
          <strong>đơn PENDING</strong>
          {' '}
          (chưa thanh toán xong). Case 4 chạy lẻ nên dùng
          {' '}
          <strong>đơn đã SUCCESS</strong>
          ; khi chạy suite, Case 4 tự dùng cùng đơn PENDING sau Case 5.
          Merchant trả
          {' '}
          <code>01</code>
          {' '}
          → sai
          {' '}
          <code>txnRef</code>
          /amount hoặc đơn chưa tồn tại.
        </div>

        {hasUnexpectedOrderNotFound && (
          <div className="alert alert-danger mb-3" style={{ fontSize: 13 }}>
            Nhiều case đang nhận
            {' '}
            <strong>RspCode 01</strong>
            {' '}
            (không tìm thấy đơn). Bấm
            {' '}
            <strong>Tạo đơn test trên merchant</strong>
            {' '}
            hoặc tạo đơn PENDING trên merchant rồi nhập lại ô
            {' '}
            <strong>Đơn chờ thanh toán</strong>
            .
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} onBlur={saveCurrentInput}>
          <input type="hidden" {...register('partnerId')} />
          <div className="row mb-3">
            <div className="col-lg-4">
              <label className="form-label">Test case (chạy đơn lẻ)</label>
              <select className="form-select" {...register('testCase', { required: true })}>
                {metadata.testCases.map((o) => (
                  <option key={o.value} value={o.value}>
                    [Case {o.caseCode}] {o.label} → {o.expectedRspCode}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-lg-4">
              <label className="form-label">Số tiền sai (VND){testCase === 'WRONG_AMOUNT' ? ' *' : ''}</label>
              <input
                type="number"
                className="form-control"
                placeholder="Mặc định: amount PENDING + 1000"
                {...register('wrongAmountVnd', { required: testCase === 'WRONG_AMOUNT' ? 'Nhập số tiền sai' : false })}
              />
            </div>
          </div>

          <div className="order-input-group mb-3">
            <h4 className="order-input-title">Đơn chờ thanh toán (PENDING)</h4>
            <p className="order-input-desc">
              Dùng cho Case 3, 5, 6 và chạy suite tự động. Tạo đơn trên merchant (có thể dừng ở OTP).
            </p>
            <div className="row">
              <div className="col-lg-5">
                <label className="form-label">Mã giao dịch (txnRef) *</label>
                <div className="d-flex gap-2">
                  <input
                    className="form-control"
                    placeholder="VD: ORDER20250616001"
                    {...register('pendingTxnRef')}
                  />
                  <button
                    type="button"
                    className="btn btn-light-primary"
                    style={{ whiteSpace: 'nowrap' }}
                    onClick={onPrepareMerchantOrder}
                    title="Gọi merchant tạo đơn test (endpoint /api/sit/prepare-order)"
                  >
                    Tạo đơn test
                  </button>
                </div>
              </div>
              <div className="col-lg-3">
                <label className="form-label">Số tiền (VND) *</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="VD: 100000"
                  {...register('pendingAmountVnd', { required: true, min: 1 })}
                />
              </div>
            </div>
          </div>

          <div className="order-input-group order-input-group-success mb-3">
            <h4 className="order-input-title">Đơn đã thanh toán thành công (SUCCESS)</h4>
            <p className="order-input-desc">
              Dùng khi chạy <strong>Case 4</strong> đơn lẻ. Để trống nếu chạy suite (Case 4 dùng đơn PENDING sau Case 5).
            </p>
            <div className="row">
              <div className="col-lg-5">
                <label className="form-label">Mã giao dịch (txnRef)</label>
                <input
                  className="form-control"
                  placeholder="TxnRef đơn đã SUCCESS trên merchant"
                  {...register('confirmedTxnRef')}
                />
              </div>
              <div className="col-lg-3">
                <label className="form-label">Số tiền (VND)</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="VD: 100000"
                  {...register('confirmedAmountVnd', { min: 1 })}
                />
              </div>
            </div>
          </div>
          <div className="form-footer">
            <button type="submit" className="btn btn-light-primary">
              <i className="ri-send-plane-line" /> Gửi 1 callback IPN
            </button>
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
