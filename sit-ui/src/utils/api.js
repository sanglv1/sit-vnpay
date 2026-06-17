import iAxios from './IAxios';
import { SUCCESS_CODE } from '../constants';

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

/**
 * GET JSON API; unwraps `{ code, data, rspMsg }` envelope.
 * @template T
 * @param {string} path
 * @returns {Promise<T>}
 */
export async function apiGet(path) {
  return unwrap(await iAxios.get(path));
}

/**
 * POST JSON API; unwraps envelope.
 * @template T
 * @param {string} path
 * @param {unknown} [data]
 * @param {import('axios').AxiosRequestConfig} [config]
 * @returns {Promise<T>}
 */
export async function apiPost(path, data, config) {
  return unwrap(await iAxios.post(path, data, config));
}

/**
 * PUT JSON API; unwraps envelope.
 * @template T
 * @param {string} path
 * @param {unknown} data
 * @returns {Promise<T>}
 */
export async function apiPut(path, data) {
  return unwrap(await iAxios.put(path, data));
}

/**
 * PATCH JSON API; unwraps envelope.
 * @template T
 * @param {string} path
 * @param {unknown} data
 * @returns {Promise<T>}
 */
export async function apiPatch(path, data) {
  return unwrap(await iAxios.patch(path, data));
}

/**
 * PATCH API that returns no body (HTTP 204).
 * @param {string} path
 * @param {unknown} data
 * @returns {Promise<void>}
 */
export async function apiPatchNoContent(path, data) {
  await iAxios.patch(path, data);
}

/**
 * DELETE JSON API; unwraps envelope.
 * @template T
 * @param {string} path
 * @returns {Promise<T>}
 */
export async function apiDelete(path) {
  return unwrap(await iAxios.delete(path));
}

/**
 * Download binary response (e.g. DOCX export). Throws on JSON error body.
 * @param {string} path
 * @param {string} filename
 * @returns {Promise<void>}
 */
export async function apiDownload(path, filename) {
  const response = await iAxios.get(path, { responseType: 'blob' });
  if (response.data?.type?.includes('json')) {
    const text = await response.data.text();
    try {
      const body = JSON.parse(text);
      throw new Error(body.rspMsg || 'Tải file thất bại');
    } catch (e) {
      if (e instanceof Error && e.message !== 'Tải file thất bại') {
        throw new Error('Tải file thất bại');
      }
      throw e;
    }
  }
  const blob = new Blob(
    [response.data],
    { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
  );
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
