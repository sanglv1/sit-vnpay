/**
 * Typed SIT API client. Return types match `src/types/api.d.ts`.
 */
import iAxios from '../utils/IAxios';
import { SUCCESS_CODE } from '../constants';
import {
  apiDelete,
  apiDownload,
  apiGet,
  apiPatch,
  apiPost,
  apiPut,
} from '../utils/api';

/**
 * @template T
 * @param {import('axios').AxiosResponse<import('../types/api').ApiResponse<T>>} response
 * @returns {T}
 */
function unwrap(response) {
  const body = response.data;
  if (body.code !== SUCCESS_CODE) {
    throw new Error(body.rspMsg || 'Request failed');
  }
  return body.data;
}

export const sitApi = {
  auth: {
    /**
     * @param {import('../types/api').LoginRequest} credentials
     * @returns {Promise<import('../types/api').AuthResponse>}
     */
    login: async (credentials) => {
      const response = await iAxios.post('/api/auth/login', credentials);
      return unwrap(response);
    },
  },

  dashboard: {
    /** @returns {Promise<import('../types/api').DashboardResponse>} */
    get: () => apiGet('/api/dashboard'),
  },

  partners: {
    /** @returns {Promise<import('../types/api').PartnerResponse[]>} */
    list: () => apiGet('/api/partners'),

    /**
     * @param {number|string} id
     * @returns {Promise<import('../types/api').PartnerResponse>}
     */
    get: (id) => apiGet(`/api/partners/${id}`),

    /**
     * @param {import('../types/api').PartnerFormRequest} data
     * @returns {Promise<import('../types/api').PartnerResponse>}
     */
    create: (data) => apiPost('/api/partners', data),

    /**
     * @param {number|string} id
     * @param {import('../types/api').PartnerFormRequest} data
     * @returns {Promise<import('../types/api').PartnerResponse>}
     */
    update: (id, data) => apiPut(`/api/partners/${id}`, data),

    /** @param {number|string} id @returns {Promise<void>} */
    remove: (id) => apiDelete(`/api/partners/${id}`),
  },

  sessions: {
    /**
     * @param {{ page?: number, size?: number }} [params]
     * @returns {Promise<import('../types/api').PageResponse<import('../types/api').TestSessionResponse>>}
     */
    list: ({ page = 0, size = 50 } = {}) => apiGet(`/api/sessions?page=${page}&size=${size}`),

    /**
     * @param {number|string} id
     * @returns {Promise<import('../types/api').TestSessionResponse>}
     */
    get: (id) => apiGet(`/api/sessions/${id}`),

    /**
     * @param {import('../types/api').CreateSessionRequest} data
     * @returns {Promise<import('../types/api').TestSessionResponse>}
     */
    create: (data) => apiPost('/api/sessions', data),

    /**
     * @param {number|string} sessionId
     * @returns {Promise<import('../types/api').TestSuiteResponse>}
     */
    suiteResult: (sessionId) => apiGet(`/api/sessions/${sessionId}/suite-result`),

    /**
     * @param {number|string} sessionId
     * @param {string} filename
     * @param {Record<string, string>} [query]
     * @returns {Promise<void>}
     */
    exportMinutes: (sessionId, filename, query = {}) => {
      const params = new URLSearchParams(query).toString();
      const path = params
        ? `/api/sessions/${sessionId}/export-minutes?${params}`
        : `/api/sessions/${sessionId}/export-minutes`;
      return apiDownload(path, filename);
    },
  },

  tests: {
    /** @returns {Promise<import('../types/api').TestMetadataResponse>} */
    metadata: () => apiGet('/api/tests/metadata'),

    /**
     * @param {{ page?: number, size?: number, sessionId?: number|string }} [params]
     * @returns {Promise<import('../types/api').PageResponse<import('../types/api').TestRunResponse>>}
     */
    history: ({ page = 0, size = 20, sessionId } = {}) => {
      const query = sessionId != null
        ? `?page=${page}&size=${size}&sessionId=${sessionId}`
        : `?page=${page}&size=${size}`;
      return apiGet(`/api/tests${query}`);
    },

    /**
     * @param {number|string} id
     * @returns {Promise<import('../types/api').TestRunResponse>}
     */
    get: (id) => apiGet(`/api/tests/${id}`),

    /**
     * @param {import('../types/api').TestRunRequest} data
     * @returns {Promise<import('../types/api').TestRunResponse>}
     */
    run: (data) => apiPost('/api/tests/run', data),

    /**
     * @param {import('../types/api').TestSuiteRequest} data
     * @param {import('axios').AxiosRequestConfig} [config]
     * @returns {Promise<import('../types/api').TestSuiteResponse>}
     */
    runIpnSuite: (data, config) => apiPost('/api/tests/run-ipn-suite', data, config),

    /**
     * @param {import('../types/api').PrepareMerchantOrderRequest} data
     * @returns {Promise<import('../types/api').PrepareOrderResponse>}
     */
    prepareMerchantOrder: (data) => apiPost('/api/tests/prepare-merchant-order', data),
  },

  manualAcceptance: {
    /**
     * @param {number|string} sessionId
     * @returns {Promise<import('../types/api').ManualAcceptanceResponse | null>}
     */
    latestBySession: (sessionId) => apiGet(`/api/manual-acceptance/latest?sessionId=${sessionId}`),

    /**
     * @param {import('../types/api').ManualAcceptanceRequest} data
     * @returns {Promise<import('../types/api').ManualAcceptanceResponse>}
     */
    save: (data) => apiPost('/api/manual-acceptance', data),
  },

  users: {
    /**
     * @param {string} [q]
     * @returns {Promise<import('../types/api').UserResponse[]>}
     */
    list: (q) => apiGet(q ? `/api/users?q=${encodeURIComponent(q)}` : '/api/users'),

    /**
     * @param {Record<string, unknown>} data
     * @returns {Promise<import('../types/api').UserResponse>}
     */
    create: (data) => apiPost('/api/users', data),

    /**
     * @param {number|string} id
     * @param {Record<string, unknown>} data
     * @returns {Promise<import('../types/api').UserResponse>}
     */
    update: (id, data) => apiPut(`/api/users/${id}`, data),

    /**
     * @param {number|string} id
     * @param {{ password: string }} data
     * @returns {Promise<import('../types/api').UserResponse>}
     */
    resetPassword: (id, data) => apiPost(`/api/users/${id}/reset-password`, data),

    /**
     * @param {number|string} id
     * @param {boolean} active
     * @returns {Promise<import('../types/api').UserResponse>}
     */
    updateStatus: (id, active) => apiPatch(`/api/users/${id}/status`, { active }),

    /** @param {number|string} id @returns {Promise<void>} */
    remove: (id) => apiDelete(`/api/users/${id}`),
  },
};
