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

/** @param {{ requestUrl?: string | null, targetUrl?: string | null, requestParams?: string | null, flow?: string }} run */
export const formatCallbackRequestLog = (run, recurringAppUserId) => {
  if (!run) return '';
  const url = run.requestUrl || buildUrlFromParams(run.targetUrl, run.requestParams);
  if (!url) {
    return run.requestParams || '';
  }
  const formatted = url.startsWith('GET ') ? url : `GET ${url}`;
  if (run.flow === 'RECURRING' && recurringAppUserId?.trim()) {
    return overlayQueryParam(formatted, 'vnp_app_user_id', recurringAppUserId.trim());
  }
  return formatted;
};

const overlayQueryParam = (url, key, value) => {
  const encoded = `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
  const pattern = new RegExp(`${key}=[^&]*`);
  if (pattern.test(url)) {
    return url.replace(pattern, encoded);
  }
  return url;
};

export default formatCallbackRequestLog;
