import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useI18n } from '../../i18n/useI18n';
import LanguageSwitcher from '../Shared/LanguageSwitcher';
import { appActions, authActions } from '../../stores';

const Header = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const user = useSelector((state) => state.auth.user);
  const { t } = useI18n();
  const basename = process.env.REACT_APP_BASENAME || '';

  const handleLogout = () => {
    dispatch(authActions.logout());
    navigate('/login');
  };

  return (
    <header className="sit-header">
      <div className="sit-brand">
        <Link to="/" className="sit-brand-link">
          <img src={`${basename}/media/logo-icon.svg`} alt="" className="sit-brand-icon" />
          <span className="sit-brand-text">VNPAY</span>
        </Link>
      </div>
      <div className="sit-header-inner">
        <div className="sit-header-left">
          <button
            type="button"
            className="sit-toggle"
            onClick={() => {
              document.body.classList.toggle('aside-minimize');
              dispatch(appActions.toggleAside());
            }}
          >
            <i className="ri-menu-line" />
          </button>
          <div className="sit-header-title">
            <span className="sit-header-title-main">{t('header.title')}</span>
            <span className="sit-header-title-sub">{t('header.subtitle')}</span>
          </div>
        </div>
        <div className="sit-header-user">
          <LanguageSwitcher className="lang-switch-header" />
          {user && (
            <>
              <div className="sit-header-user-info">
                <div className="sit-header-user-name">{user.fullName}</div>
                <div className="sit-header-user-role">{user.roleLabel}</div>
              </div>
              <button type="button" className="sit-header-logout" onClick={handleLogout} title={t('header.logout')}>
                <i className="ri-logout-box-r-line" />
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
