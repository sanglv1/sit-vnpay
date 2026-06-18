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

const IPN_LOGIC = [
  { step: 1, check: 'Kiểm tra chữ ký', param: 'vnp_SecureHash', caseNo: 'Case 1', rsp: '97' },
  { step: 2, check: 'Kiểm tra đơn hàng', param: 'vnp_TxnRef', caseNo: 'Case 2', rsp: '01' },
  { step: 3, check: 'Kiểm tra số tiền', param: 'vnp_Amount', caseNo: 'Case 3', rsp: '04' },
  { step: 4, check: 'Kiểm tra trạng thái đơn', param: 'status', caseNo: 'Case 4', rsp: '02' },
  { step: 5, check: 'Kiểm tra kết quả giao dịch', param: 'vnp_ResponseCode / vnp_TransactionStatus', caseNo: 'Case 5', rsp: '00' },
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
    return 'txnRef Case 5 (thành công) và Case 6 (thất bại) phải khác nhau';
  }
  return null;
};

const validateOrderForCase = (caseValue, values) => {
  const { txnRef } = resolveOrderForCase(caseValue, values);
  if (SUCCESS_ORDER_CASES.has(caseValue) && !values.pendingTxnRef?.trim()) {
    return 'Nhập mã giao dịch đơn thành công (Case 5)';
  }
  if (caseValue === 'FAILED' && !values.failedTxnRef?.trim()) {
    return 'Nhập mã giao dịch đơn thất bại (Case 6)';
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
      dispatch(appActions.flash('Nhập mã giao dịch đơn thành công (Case 5) để chạy suite', 'danger'));
      return;
    }
    if (!values.failedTxnRef?.trim()) {
      dispatch(appActions.flash('Nhập mã giao dịch đơn thất bại (Case 6) để chạy suite', 'danger'));
      return;
    }
    const differError = txnRefsMustDiffer(values.pendingTxnRef, values.failedTxnRef);
    if (differError) {
      dispatch(appActions.flash(differError, 'danger'));
      return;
    }
    if (!values.pendingAmountVnd || Number(values.pendingAmountVnd) < 1) {
      dispatch(appActions.flash('Nhập số tiền đơn thành công (Case 5) để chạy suite', 'danger'));
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

        <div className="alert alert-info mb-3" style={{ fontSize: 13 }}>
          <strong>Quy trình chuẩn bị (trên merchant):</strong>
          <ol className="mb-0 mt-2 ps-3">
            <li>Tạo <strong>giao dịch 1</strong> trên merchant (Pay) → đến màn <strong>OTP</strong> → dừng, copy <code>txnRef</code> + số tiền → nhập ô <strong>Case 5</strong>.</li>
            <li>Tạo <strong>giao dịch 2</strong> (txnRef khác) → đến <strong>OTP</strong> → dừng → nhập ô <strong>Case 6</strong>.</li>
            <li>Quay lại SIT → bấm <strong>Tiến hành kiểm tra tự động</strong>. Tool giả lập IPN (không cần xác nhận OTP trên cổng VNPay).</li>
          </ol>
        </div>

        <div className="alert alert-danger mb-3" style={{ fontSize: 13 }}>
          <strong>Lưu ý case IPN:</strong>
          {' '}
          Case 1 (97) và Case 2 (01) không cần đơn thật. Case 3, 5 dùng
          {' '}
          <strong>đơn Case 5</strong>
          ; Case 6 dùng
          {' '}
          <strong>đơn Case 6 riêng</strong>
          . Mỗi đơn chỉ cập nhật DB một lần (RspCode
          {' '}
          <code>00</code>
          ); gọi lại IPN trả
          {' '}
          <code>02</code>
          . Case 4 (suite) gửi lại IPN đơn Case 5 sau khi đã xác nhận.
          Merchant trả
          {' '}
          <code>01</code>
          {' '}
          → sai
          {' '}
          <code>txnRef</code>
          /amount hoặc đơn chưa tồn tại trên merchant.
        </div>

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
            <h4 className="order-input-title">Đơn giao dịch thành công (Case 5)</h4>
            <p className="order-input-desc">
              Tạo trên merchant, dừng ở OTP. Dùng cho Case 3, Case 5; suite Case 4 gửi lại IPN trên đơn này.
            </p>
            <div className="row">
              <div className="col-lg-5">
                <label className="form-label">Mã giao dịch (txnRef) *</label>
                <input
                  className="form-control"
                  placeholder="TxnRef từ merchant (GD 1, dừng OTP)"
                  {...register('pendingTxnRef')}
                />
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

          <div className="order-input-group order-input-group-failed mb-3">
            <h4 className="order-input-title">Đơn giao dịch thất bại (Case 6)</h4>
            <p className="order-input-desc">
              Tạo <strong>giao dịch thứ hai</strong> trên merchant, dừng ở OTP — txnRef phải khác Case 5.
            </p>
            <div className="row">
              <div className="col-lg-5">
                <label className="form-label">Mã giao dịch (txnRef) *</label>
                <input
                  className="form-control"
                  placeholder="TxnRef từ merchant (GD 2, dừng OTP)"
                  {...register('failedTxnRef')}
                />
              </div>
              <div className="col-lg-3">
                <label className="form-label">Số tiền (VND)</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="Mặc định: giống Case 5"
                  {...register('failedAmountVnd', { min: 1 })}
                />
              </div>
            </div>
          </div>

          <div className="order-input-group order-input-group-success mb-3">
            <h4 className="order-input-title">Đơn đã thanh toán thành công (Case 4)</h4>
            <p className="order-input-desc">
              Dùng khi chạy <strong>Case 4</strong> đơn lẻ. Tự điền sau khi Case 5 PASS; để trống nếu chạy suite.
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
