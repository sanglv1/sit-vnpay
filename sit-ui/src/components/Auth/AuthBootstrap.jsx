import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useI18n } from '../../i18n/useI18n';
import { bootstrapAuth } from '../../stores';

const AuthBootstrap = ({ children }) => {
  const dispatch = useDispatch();
  const bootstrapped = useSelector((state) => state.auth.bootstrapped);
  const { t } = useI18n();

  useEffect(() => {
    if (!bootstrapped) {
      dispatch(bootstrapAuth());
    }
  }, [bootstrapped, dispatch]);

  if (!bootstrapped) {
    return <div className="login-loading">{t('common.loading')}</div>;
  }

  return children;
};

export default AuthBootstrap;
