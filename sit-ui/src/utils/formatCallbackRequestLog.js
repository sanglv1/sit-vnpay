/**
 * Format test run callback as full HTTP GET log (IPN / Return URL).
 */

const parseParams = (json) => {
  if (!json) return {};
  try {
    return JSON.parse(json);
  } catch {
    return {};
  }
};

const buildUrlFromParams = (targetUrl, requestParams) => {
  if (!targetUrl) return '';
  const params = parseParams(requestParams);
  const entries = Object.entries(params);
  if (entries.length === 0) {
    return targetUrl;
  }
  const qs = entries
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value ?? '')}`)
    .join('&');
  const separator = targetUrl.includes('?') ? '&' : '?';
  return `${targetUrl}${separator}${qs}`;
};

/** @param {{ requestUrl?: string | null, targetUrl?: string | null, requestParams?: string | null }} run */
export const formatCallbackRequestLog = (run) => {
  if (!run) return '';
  const url = run.requestUrl || buildUrlFromParams(run.targetUrl, run.requestParams);
  if (!url) {
    return run.requestParams || '';
  }
  return url.startsWith('GET ') ? url : `GET ${url}`;
};

export default formatCallbackRequestLog;
