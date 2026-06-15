import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Navigate, useLocation } from 'react-router-dom';
import { useI18n } from '../../i18n/useI18n';
import { bootstrapAuth } from '../../stores';

const PrivateRoute = ({ children, adminOnly = false }) => {
  const dispatch = useDispatch();
  const auth = useSelector((state) => state.auth);
  const location = useLocation();
  const { t } = useI18n();

  useEffect(() => {
    if (!auth.bootstrapped) {
      dispatch(bootstrapAuth());
    }
  }, [auth.bootstrapped, dispatch]);

  if (!auth.bootstrapped) {
    return <div className="login-loading">{t('common.loading')}</div>;
  }

  if (!auth.token) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (adminOnly && auth.user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default PrivateRoute;
