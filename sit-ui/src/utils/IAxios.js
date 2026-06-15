import axios from 'axios';
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants';

const iAxios = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  timeout: 60000,
});

iAxios.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

iAxios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
      const basename = process.env.REACT_APP_BASENAME || '';
      const loginPath = `${basename}/login`.replace('//', '/');
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = loginPath;
      }
    }
    const rspMsg = error.response?.data?.rspMsg;
    if (rspMsg) {
      return Promise.reject(new Error(rspMsg));
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error(
        'Yêu cầu quá thời gian chờ. IPN suite chạy 6 case liên tiếp — kiểm tra IPN URL merchant hoặc thử lại.',
      ));
    }
    if (error.message) {
      return Promise.reject(new Error(error.message));
    }
    return Promise.reject(error);
  },
);

export default iAxios;
