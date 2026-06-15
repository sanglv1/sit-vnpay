export const queryKeys = {
  dashboard: ['dashboard'],
  partners: {
    all: ['partners'],
    detail: (id) => ['partners', String(id)],
  },
  sessions: {
    list: (params) => ['sessions', 'list', params],
    detail: (id) => ['sessions', 'detail', String(id)],
    suiteResult: (id) => ['sessions', 'suite-result', String(id)],
  },
  tests: {
    metadata: ['tests', 'metadata'],
    history: (params) => ['tests', 'history', params],
    detail: (id) => ['tests', 'detail', String(id)],
  },
  manualAcceptance: (sessionId) => ['manual-acceptance', String(sessionId)],
  users: (search) => ['users', search ?? ''],
};

/**
 * @param {import('../types/api').TestRunResponse[] | undefined} content
 * @returns {Record<string, import('../types/api').TestRunResponse>}
 */
export function latestRunsByCase(content) {
  const latest = {};
  (content || []).forEach((run) => {
    if (!latest[run.testCase]) {
      latest[run.testCase] = run;
    }
  });
  return latest;
}
