import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { LANG_KEY, translate } from '../i18n/messages';

export function useI18n() {
  const locale = useSelector((state) => state.locale.locale);
  const dispatch = useDispatch();

  const t = useCallback((key, params) => translate(locale, key, params), [locale]);

  const setLocale = useCallback((next) => {
    if (next !== 'vi' && next !== 'en') return;
    localStorage.setItem(LANG_KEY, next);
    dispatch({ type: 'LOCALE_SET', locale: next });
  }, [dispatch]);

  return { locale, t, setLocale };
}
