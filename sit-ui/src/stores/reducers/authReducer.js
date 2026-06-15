import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../../constants';

const loadUser = () => {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const initialState = {
  token: localStorage.getItem(AUTH_TOKEN_KEY) || null,
  user: loadUser(),
  bootstrapped: false,
};

export default function authReducer(state = initialState, action) {
  switch (action.type) {
    case 'AUTH_LOGIN':
      return {
        ...state,
        token: action.token,
        user: action.user,
        bootstrapped: true,
      };
    case 'AUTH_LOGOUT':
      return {
        ...state,
        token: null,
        user: null,
        bootstrapped: true,
      };
    case 'AUTH_BOOTSTRAP_DONE':
      return { ...state, bootstrapped: true };
    default:
      return state;
  }
}
