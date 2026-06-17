import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sitApi } from './client';
import { queryKeys, latestRunsByCase } from './queryKeys';

export { latestRunsByCase };

export function useDashboardQuery() {
  return useQuery({
    queryKey: queryKeys.dashboard,
    queryFn: () => sitApi.dashboard.get(),
    meta: { silent: true },
  });
}

export function usePartnersQuery() {
  return useQuery({
    queryKey: queryKeys.partners.all,
    queryFn: () => sitApi.partners.list(),
  });
}

export function usePartnerQuery(id, { enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.partners.detail(id),
    queryFn: () => sitApi.partners.get(id),
    enabled: enabled && Boolean(id),
  });
}

export function useDeletePartnerMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => sitApi.partners.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.partners.all });
    },
  });
}

export function useSavePartnerMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => (
      id ? sitApi.partners.update(id, data) : sitApi.partners.create(data)
    ),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: queryKeys.partners.all });
      if (id) {
        qc.invalidateQueries({ queryKey: queryKeys.partners.detail(id) });
      }
    },
  });
}

export function useSessionsQuery(params = { page: 0, size: 50 }) {
  return useQuery({
    queryKey: queryKeys.sessions.list(params),
    queryFn: () => sitApi.sessions.list(params),
  });
}

export function useSessionWorkspaceQuery(sessionId, { enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.sessions.workspace(sessionId),
    queryFn: () => sitApi.sessions.workspace(sessionId),
    enabled: enabled && Boolean(sessionId),
  });
}

export function useSessionQuery(sessionId, { enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.sessions.detail(sessionId),
    queryFn: () => sitApi.sessions.get(sessionId),
    enabled: enabled && Boolean(sessionId),
  });
}

export function useCreateSessionMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data) => sitApi.sessions.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sessions', 'list'] });
      qc.invalidateQueries({ queryKey: queryKeys.dashboard });
    },
  });
}

export function useSaveSessionTestInputMutation(sessionId) {
  return useMutation({
    mutationFn: (data) => sitApi.sessions.saveTestInput(sessionId, data),
    meta: { silent: true },
  });
}

export function useSuiteResultQuery(sessionId) {
  return useQuery({
    queryKey: queryKeys.sessions.suiteResult(sessionId),
    queryFn: () => sitApi.sessions.suiteResult(sessionId),
    enabled: Boolean(sessionId),
    retry: false,
    meta: { silent: true },
  });
}

export function useTestMetadataQuery({ enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.tests.metadata,
    queryFn: () => sitApi.tests.metadata(),
    enabled,
    staleTime: 5 * 60_000,
  });
}

export function useTestHistoryQuery(params = {}, { enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.tests.history(params),
    queryFn: () => sitApi.tests.history(params),
    enabled,
  });
}

export function useTestRunQuery(id) {
  return useQuery({
    queryKey: queryKeys.tests.detail(id),
    queryFn: () => sitApi.tests.get(id),
    enabled: Boolean(id),
  });
}

export function useManualAcceptanceQuery(sessionId, { enabled = true } = {}) {
  return useQuery({
    queryKey: queryKeys.manualAcceptance(sessionId),
    queryFn: () => sitApi.manualAcceptance.latestBySession(sessionId),
    enabled: enabled && Boolean(sessionId),
    meta: { silent: true },
  });
}

function invalidateAfterTestRun(qc, sessionId) {
  qc.invalidateQueries({ queryKey: queryKeys.sessions.workspace(sessionId) });
  qc.invalidateQueries({ queryKey: queryKeys.sessions.detail(sessionId) });
  qc.invalidateQueries({ queryKey: queryKeys.sessions.suiteResult(sessionId) });
  qc.invalidateQueries({ queryKey: ['sessions', 'list'] });
  qc.invalidateQueries({ queryKey: queryKeys.dashboard });
}

export function useRunTestMutation(sessionId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data) => sitApi.tests.run(data),
    onSuccess: () => invalidateAfterTestRun(qc, sessionId),
  });
}

export function useRunIpnSuiteMutation(sessionId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ data, config }) => sitApi.tests.runIpnSuite(data, config),
    onSuccess: () => invalidateAfterTestRun(qc, sessionId),
  });
}

export function usePrepareMerchantOrderMutation() {
  return useMutation({
    mutationFn: (data) => sitApi.tests.prepareMerchantOrder(data),
  });
}

export function useSaveManualAcceptanceMutation(sessionId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data) => sitApi.manualAcceptance.save(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.manualAcceptance(sessionId) });
    },
  });
}

export function useUsersQuery(search = '', { enabled = true } = {}) {
  const term = search.trim();
  return useQuery({
    queryKey: queryKeys.users(term),
    queryFn: () => sitApi.users.list(term || undefined),
    enabled,
  });
}

export function useSaveUserMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => (
      id ? sitApi.users.update(id, data) : sitApi.users.create(data)
    ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
    },
  });
}

export function useResetPasswordMutation() {
  return useMutation({
    mutationFn: ({ id, password }) => sitApi.users.resetPassword(id, { password }),
  });
}

export function useUpdateUserStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }) => sitApi.users.updateStatus(id, active),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
    },
  });
}

export function useDeleteUserMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => sitApi.users.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
    },
  });
}

export function useExportMinutesMutation() {
  return useMutation({
    mutationFn: ({ sessionId, filename, query }) => sitApi.sessions.exportMinutes(sessionId, filename, query),
  });
}
