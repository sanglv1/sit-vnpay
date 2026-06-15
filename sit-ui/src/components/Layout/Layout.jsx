import { Outlet } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { useEffect } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import CreditSection from './CreditSection';
import QueryLoadingBridge from '../Shared/QueryLoadingBridge';
import { appActions } from '../../stores';

const Layout = () => {
  const app = useSelector((state) => state.app);
  const dispatch = useDispatch();

  useEffect(() => {
    if (app.flashMessage) {
      const timer = setTimeout(() => dispatch(appActions.clearFlash()), 5000);
      return () => clearTimeout(timer);
    }
  }, [app.flashMessage, dispatch]);

  return (
    <>
      <QueryLoadingBridge />
      <Header />
      <div className="sit-page">
        <Sidebar />
        <div className="sit-wrapper">
          <div className="sit-card">
            {app.flashMessage && (
              <div className={`alert alert-${app.flashType}`}>{app.flashMessage}</div>
            )}
            <Outlet />
          </div>
          <CreditSection />
        </div>
      </div>
      {app.loading && <div className="loading-overlay">Đang xử lý...</div>}
    </>
  );
};

export default Layout;
