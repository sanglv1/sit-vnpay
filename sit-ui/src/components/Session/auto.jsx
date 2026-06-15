import { useEffect, useMemo, useState } from 'react';
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
  useSessionQuery,
  useTestHistoryQuery,
  useTestMetadataQuery,
} from '../../api/hooks';
import { appActions } from '../../stores';

const IPN_LOGIC = [
  { step: 1, check: 'Kiểm tra chữ ký', param: 'vnp_SecureHash', caseNo: 'Case 1', rsp: '97' },
  { step: 2, check: 'Kiểm tra đơn hàng', param: 'vnp_TxnRef', caseNo: 'Case 2', rsp: '01' },
  { step: 3, check: 'Kiểm tra số tiền', param: 'vnp_Amount', caseNo: 'Case 3', rsp: '04' },
  { step: 4, check: 'Kiểm tra trạng thái đơn', param: 'status', caseNo: 'Case 4', rsp: '02' },
  { step: 5, check: 'Kiểm tra kết quả giao dịch', param: 'vnp_ResponseCode / vnp_TransactionStatus', caseNo: 'Case 5', rsp: '00' },
];

const SessionAuto = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [runningCase, setRunningCase] = useState(null);
  const { register, handleSubmit, watch, getValues, reset, setValue } = useForm();
  const testCase = watch('testCase');

  const { data: session } = useSessionQuery(sessionId);
  const { data: metadata } = useTestMetadataQuery();
  const { data: history } = useTestHistoryQuery({ sessionId, size: 50 }, { enabled: Boolean(sessionId) });
  const runTest = useRunTestMutation(sessionId);
  const runIpnSuite = useRunIpnSuiteMutation(sessionId);
  const prepareOrder = usePrepareMerchantOrderMutation();

  const runsByCase = useMemo(
    () => latestRunsByCase(history?.content),
    [history],
  );

  useEffect(() => {
    if (!session || !metadata) return;
    reset({
      partnerId: session.partnerId,
      sessionId: session.id,
      txnRef: metadata.defaultTxnRef,
      amountVnd: 100000,
      testCase: 'SUCCESS',
    });
  }, [session, metadata, reset]);

  const buildPayload = (values) => ({
    partnerId: Number(session.partnerId),
    sessionId: Number(sessionId),
    callbackType: 'IPN',
    testCase: values.testCase,
    txnRef: values.txnRef,
    amountVnd: Number(values.amountVnd),
    wrongAmountVnd: values.wrongAmountVnd ? Number(values.wrongAmountVnd) : null,
  });

  const runSingleCase = async (caseValue) => {
    const values = getValues();
    if (!values.txnRef) {
      dispatch(appActions.flash('Nhập mã giao dịch (txnRef)', 'danger'));
      return;
    }
    setRunningCase(caseValue);
    try {
      const result = await runTest.mutateAsync({
        ...buildPayload({ ...values, testCase: caseValue }),
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
    if (!values.txnRef) {
      dispatch(appActions.flash('Nhập mã giao dịch (txnRef)', 'danger'));
      return;
    }
    try {
      const result = await runIpnSuite.mutateAsync({
        data: {
          partnerId: Number(session.partnerId),
          sessionId: Number(sessionId),
          txnRef: values.txnRef,
          amountVnd: Number(values.amountVnd || 100000),
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
    const amountVnd = Number(getValues('amountVnd') || 100000);
    try {
      const result = await prepareOrder.mutateAsync({
        partnerId: Number(session.partnerId),
        amountVnd,
      });
      setValue('txnRef', result.txnRef);
      setValue('amountVnd', result.amountVnd);
      dispatch(appActions.flash(
        `Đã tạo đơn test: txnRef=${result.txnRef}, amount=${result.amountVnd} VND`,
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
          <table className="table table-striped">
            <thead>
              <tr>
                <th>Bước</th>
                <th>Điều kiện kiểm tra</th>
                <th>Tham số liên quan</th>
                <th>Case</th>
                <th>RspCode</th>
              </tr>
            </thead>
            <tbody>
              {IPN_LOGIC.map((r) => (
                <tr key={r.step}>
                  <td>{r.step}</td>
                  <td>{r.check}</td>
                  <td><code>{r.param}</code></td>
                  <td>{r.caseNo}</td>
                  <td><strong>{r.rsp}</strong></td>
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
          Case 97 và 01 không cần đơn hàng thật. Các case còn lại (04, 00, 02…) yêu cầu merchant
          {' '}
          <strong>đã có đơn</strong>
          {' '}
          với đúng
          {' '}
          <code>txnRef</code>
          {' '}
          và
          {' '}
          <strong>số tiền khớp</strong>
          . Nếu merchant trả
          {' '}
          <code>01</code>
          {' '}
          → đơn chưa tồn tại hoặc sai
          {' '}
          <code>txnRef</code>
          /amount.
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
            hoặc tạo đơn thủ công trên merchant rồi nhập lại
            {' '}
            <code>txnRef</code>
            {' '}
            + số tiền.
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <input type="hidden" {...register('partnerId')} />
          <div className="row">
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
              <label className="form-label">Mã giao dịch (txnRef) *</label>
              <div className="d-flex gap-2">
                <input className="form-control" {...register('txnRef', { required: 'Bắt buộc' })} />
                <button
                  type="button"
                  className="btn btn-light-primary"
                  style={{ whiteSpace: 'nowrap' }}
                  onClick={onPrepareMerchantOrder}
                  title="Gọi merchant tạo đơn test in-memory (demo hỗ trợ /api/sit/prepare-order)"
                >
                  Tạo đơn test
                </button>
              </div>
            </div>
            <div className="col-lg-4">
              <label className="form-label">Số tiền (VND) *</label>
              <input type="number" className="form-control" {...register('amountVnd', { required: true, min: 1 })} />
            </div>
            <div className="col-lg-4">
              <label className="form-label">Số tiền sai (VND){testCase === 'WRONG_AMOUNT' ? ' *' : ''}</label>
              <input
                type="number"
                className="form-control"
                placeholder="Mặc định: amount + 1000"
                {...register('wrongAmountVnd', { required: testCase === 'WRONG_AMOUNT' ? 'Nhập số tiền sai' : false })}
              />
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
