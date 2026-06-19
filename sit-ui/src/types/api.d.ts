/**
 * API response types — mirror `sit-api` DTOs (`com.vnpay.sit.api.dto`).
 * JSON dates are ISO-8601 strings.
 */

export type PaymentFlow = 'PAY' | 'TOKEN' | 'RECURRING' | 'INSTALMENT';
export type CallbackType = 'RETURN' | 'IPN';
export type UserRole = 'ADMIN' | 'MERCHANT_QC';
export type TestCaseType =
  | 'UNKNOWN_ERROR'
  | 'INVALID_HASH'
  | 'ORDER_NOT_FOUND'
  | 'WRONG_AMOUNT'
  | 'ORDER_ALREADY_CONFIRMED'
  | 'SUCCESS'
  | 'FAILED';

/** Standard API envelope: `{ code, data, rspMsg }` */
export interface ApiResponse<T> {
  code: string;
  data: T;
  rspMsg: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PartnerResponse {
  id: number;
  name: string;
  flow: PaymentFlow;
  flowLabel: string;
  tmnCode: string;
  /** Full secret for ADMIN or creator; masked for other MERCHANT_QC */
  secretKey: string;
  returnUrl: string | null;
  ipnUrl: string;
  note: string | null;
  active: boolean;
  createdByEmail: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TestSessionResponse {
  id: number;
  partnerId: number;
  partnerName: string;
  tmnCode: string;
  note: string | null;
  status: string;
  autoPassed: number;
  autoTotal: number;
  pendingTxnRef: string | null;
  pendingAmountVnd: number | null;
  confirmedTxnRef: string | null;
  confirmedAmountVnd: number | null;
  failedTxnRef: string | null;
  failedAmountVnd: number | null;
  wrongAmountVnd: number | null;
  createdAt: string;
  updatedAt: string;
  createdByEmail: string | null;
}

export interface TestRunResponse {
  id: number;
  partnerId: number;
  partnerName: string;
  flow: PaymentFlow;
  callbackType: CallbackType;
  testCase: TestCaseType;
  testCaseLabel: string;
  txnRef: string;
  targetUrl: string;
  requestUrl: string | null;
  requestParams: string;
  httpStatus: number | null;
  responseBody: string | null;
  expectedRspCode: string | null;
  actualRspCode: string | null;
  passed: boolean;
  durationMs: number | null;
  errorMessage: string | null;
  createdAt: string;
  sessionCreatedByEmail: string | null;
}

export type SessionCompletionFilter = 'ALL' | 'COMPLETED' | 'IN_PROGRESS' | 'NOT_STARTED';

export interface SessionWorkspaceResponse {
  session: TestSessionResponse;
  latestRuns: TestRunResponse[];
  testCases: EnumOption[];
}

export interface EnumOption {
  value: string;
  label: string;
  expectedRspCode: string | null;
  caseCode: string | null;
  checkOrder: number;
}

export interface TestMetadataResponse {
  partners: PartnerResponse[];
  callbackTypes: EnumOption[];
  testCases: EnumOption[];
  paymentFlows: EnumOption[];
  defaultTxnRef: string;
}

export interface TestSuiteStepResponse {
  step: number;
  caseCode: string;
  checkOrder: number;
  testCase: TestCaseType;
  testCaseLabel: string;
  expectedRspCode: string | null;
  actualRspCode: string | null;
  passed: boolean;
  testRunId: number | null;
  httpStatus: number | null;
  message: string | null;
}

export interface TestSuiteResponse {
  sessionId: number | null;
  txnRef: string;
  failedTxnRef: string;
  partnerName: string;
  totalSteps: number;
  passedSteps: number;
  allPassed: boolean;
  steps: TestSuiteStepResponse[];
}

export interface PrepareOrderResponse {
  txnRef: string;
  amountVnd: number;
  prepareUrl: string;
}

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  roleLabel: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface ManualAcceptanceResponse {
  id: number;
  partnerId: number;
  partnerName: string;
  returnSuccessTxnRef: string | null;
  returnSuccessImage: string | null;
  returnFailedTxnRef: string | null;
  returnFailedImage: string | null;
  exceptionHandled: boolean | null;
  whitelistIpPassed: boolean | null;
  logStoragePassed: boolean | null;
  note: string | null;
  updatedAt: string;
}

export interface DashboardChartDay {
  label: string;
  passed: number;
  failed: number;
}

export interface DashboardRecentSession {
  id: number;
  tmnCode: string;
  statusLabel: string;
  passed: boolean;
  createdByEmail: string;
  createdAt: string;
}

export interface DashboardResponse {
  terminalCount: number;
  terminalDeltaMonth: number;
  sessionsThisWeek: number;
  sessionsWeekChangePercent: number;
  testCasePassed: number;
  testCaseFailed: number;
  passRatePercent: number;
  topFailHint: string;
  chartLast7Days: DashboardChartDay[];
  recentSessions: DashboardRecentSession[];
}

/** Request bodies (subset used by UI) */
export interface LoginRequest {
  email: string;
  password: string;
}

export interface CreateSessionRequest {
  partnerId: number;
  note?: string;
}

export interface SaveSessionTestInputRequest {
  pendingTxnRef?: string;
  pendingAmountVnd?: number | null;
  failedTxnRef?: string;
  failedAmountVnd?: number | null;
  confirmedTxnRef?: string;
  confirmedAmountVnd?: number | null;
  wrongAmountVnd?: number | null;
}

export interface TestRunRequest {
  partnerId: number;
  sessionId?: number;
  callbackType: CallbackType;
  testCase: TestCaseType;
  txnRef: string;
  amountVnd: number;
  wrongAmountVnd?: number;
}

export interface TestSuiteRequest {
  partnerId: number;
  sessionId: number;
  txnRef: string;
  amountVnd: number;
  failedTxnRef: string;
  failedAmountVnd?: number | null;
  wrongAmountVnd?: number;
}

export interface PrepareMerchantOrderRequest {
  partnerId: number;
  txnRef: string;
  amountVnd: number;
}

export interface PartnerFormRequest {
  id?: number | null;
  name: string;
  flow: PaymentFlow;
  tmnCode: string;
  secretKey: string;
  returnUrl?: string;
  ipnUrl: string;
  note?: string;
  active?: boolean;
}

export interface ManualAcceptanceRequest {
  id?: number | null;
  partnerId: number;
  sessionId?: number | null;
  returnSuccessTxnRef?: string;
  returnSuccessImage?: string;
  returnFailedTxnRef?: string;
  returnFailedImage?: string;
  exceptionHandled?: boolean;
  whitelistIpPassed?: boolean;
  logStoragePassed?: boolean;
  note?: string;
}
