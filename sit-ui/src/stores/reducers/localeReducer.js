import { LANG_KEY } from '../../i18n/messages';

const stored = localStorage.getItem(LANG_KEY);
const initialLocale = stored === 'en' || stored === 'vi' ? stored : 'vi';

const initialState = {
  locale: initialLocale,
};

export default function localeReducer(state = initialState, action) {
  switch (action.type) {
    case 'LOCALE_SET':
      return { ...state, locale: action.locale };
    default:
      return state;
  }
}
