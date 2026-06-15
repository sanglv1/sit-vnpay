import { createStore, applyMiddleware, combineReducers } from 'redux';
import thunk from 'redux-thunk';
import appReducer from './reducers/appReducer';
import authReducer from './reducers/authReducer';
import localeReducer from './reducers/localeReducer';
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants';
import iAxios from '../utils/IAxios';

const rootReducer = combineReducers({ app: appReducer, auth: authReducer, locale: localeReducer });

export const store = createStore(rootReducer, applyMiddleware(thunk));

export const appActions = {
  loadingOn: () => ({ type: 'APP_LOADING_ON' }),
  loadingOff: () => ({ type: 'APP_LOADING_OFF' }),
  toggleAside: () => ({ type: 'APP_TOGGLE_ASIDE' }),
  flash: (message, flashType = 'success') => ({ type: 'APP_FLASH', message, flashType }),
  clearFlash: () => ({ type: 'APP_CLEAR_FLASH' }),
};

export const authActions = {
  login: (token, user) => {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    return { type: 'AUTH_LOGIN', token, user };
  },
  logout: () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    delete iAxios.defaults.headers.common.Authorization;
    return { type: 'AUTH_LOGOUT' };
  },
  bootstrapDone: () => ({ type: 'AUTH_BOOTSTRAP_DONE' }),
};

export const bootstrapAuth = () => async (dispatch) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) {
    dispatch(authActions.bootstrapDone());
    return;
  }
  iAxios.defaults.headers.common.Authorization = `Bearer ${token}`;
  try {
    const response = await iAxios.get('/api/auth/me');
    const body = response.data;
    if (body.code === '00' && body.data) {
      dispatch(authActions.login(token, body.data));
    } else {
      dispatch(authActions.logout());
    }
  } catch {
    dispatch(authActions.logout());
  }
};
