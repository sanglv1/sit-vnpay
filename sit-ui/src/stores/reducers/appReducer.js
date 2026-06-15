const initialState = {
  loading: false,
  asideMinimized: false,
  flashMessage: null,
  flashType: 'success',
};

export default function appReducer(state = initialState, action) {
  switch (action.type) {
    case 'APP_LOADING_ON':
      return { ...state, loading: true };
    case 'APP_LOADING_OFF':
      return { ...state, loading: false };
    case 'APP_TOGGLE_ASIDE':
      return { ...state, asideMinimized: !state.asideMinimized };
    case 'APP_FLASH':
      return { ...state, flashMessage: action.message, flashType: action.flashType || 'success' };
    case 'APP_CLEAR_FLASH':
      return { ...state, flashMessage: null };
    default:
      return state;
  }
}
