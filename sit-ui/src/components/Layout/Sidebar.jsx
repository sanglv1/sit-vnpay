import { NavLink } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useI18n } from '../../i18n/useI18n';

const MENU_ITEMS = [
  { path: '/', icon: 'ri-home-4-line', labelKey: 'menu.home', exact: true },
  { path: '/partners', icon: 'ri-terminal-box-line', labelKey: 'menu.partners' },
  { path: '/users', icon: 'ri-user-settings-line', labelKey: 'menu.users', adminOnly: true },
  { path: '/sessions', icon: 'ri-file-list-3-line', labelKey: 'menu.sessions' },
  { path: '/guide', icon: 'ri-book-open-line', labelKey: 'menu.guide' },
];

const Sidebar = () => {
  const user = useSelector((state) => state.auth.user);
  const { t } = useI18n();
  const isAdmin = user?.role === 'ADMIN';

  const items = MENU_ITEMS.filter((item) => !item.adminOnly || isAdmin);

  return (
    <aside className="sit-aside">
      <nav>
        <ul className="menu-nav">
          {items.map((item) => (
            <li key={item.path} className="menu-item">
              <NavLink
                to={item.path}
                end={item.exact}
                className={({ isActive }) => `menu-link${isActive ? ' menu-item-active-link' : ''}`}
                style={({ isActive }) => (isActive ? { background: '#e8f1f6', color: 'var(--vnpay-brand)' } : {})}
              >
                <i className={item.icon} />
                <span className="menu-text">{t(item.labelKey)}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
};

export default Sidebar;
