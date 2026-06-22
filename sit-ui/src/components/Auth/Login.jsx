import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import { sitApi } from '../../api/client';
import { useI18n } from '../../i18n/useI18n';
import LanguageSwitcher from '../Shared/LanguageSwitcher';
import { appActions, authActions } from '../../stores';

/** Map backend auth errors to locale — API rspMsg is always Vietnamese. */
const resolveLoginError = (error, t) => {
  const msg = (error?.message ?? '').toLowerCase();
  if (msg.includes('vô hiệu') || msg.includes('disabled')) {
    return t('login.accountDisabled');
  }
  if (msg.includes('mật khẩu') || msg.includes('password') || msg.includes('email')) {
    return t('login.invalidCredentials');
  }
  return t('login.failed');
};

const Login = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useSelector((state) => state.auth);
  const { t } = useI18n();
  const basename = process.env.REACT_APP_BASENAME || '';
  const [loginError, setLoginError] = useState(null);
  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: { email: '', password: '' },
  });

  useEffect(() => {
    if (auth.bootstrapped && auth.token) {
      navigate(location.state?.from || '/', { replace: true });
    }
  }, [auth.bootstrapped, auth.token, navigate, location.state]);

  const onSubmit = async (values) => {
    setLoginError(null);
    dispatch(appActions.loadingOn());
    try {
      const { token, user } = await sitApi.auth.login(values);
      dispatch(authActions.login(token, user));
      navigate(location.state?.from || '/', { replace: true });
    } catch (e) {
      setLoginError(resolveLoginError(e, t));
    } finally {
      dispatch(appActions.loadingOff());
    }
  };

  return (
    <div className="login-page">
      <div className="login-lang">
        <LanguageSwitcher />
      </div>
      <div className="login-card">
        <div className="login-brand">
          <img src={`${basename}/media/logo-color.svg`} alt="VNPAY" className="login-logo" />
        </div>
        <h1 className="login-title">{t('login.title')}</h1>
        <p className="login-subtitle">{t('login.subtitle')}</p>

        <form onSubmit={handleSubmit(onSubmit)} className="login-form">
          {loginError && (
            <div className="alert alert-danger login-error" role="alert">
              {loginError}
            </div>
          )}
          <div className="mb-3">
            <label className="form-label">{t('login.email')}</label>
            <input
              className="form-control"
              type="email"
              autoComplete="username"
              placeholder={t('login.emailPlaceholder')}
              {...register('email', { required: t('login.emailRequired') })}
            />
            {errors.email && <div className="fv-help-block">{errors.email.message}</div>}
          </div>
          <div className="mb-3">
            <label className="form-label">{t('login.password')}</label>
            <input
              className="form-control"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              {...register('password', { required: t('login.passwordRequired') })}
            />
            {errors.password && <div className="fv-help-block">{errors.password.message}</div>}
          </div>
          <div className="login-actions">
            <button type="submit" className="btn btn-primary login-submit">
              {t('login.submit')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;
