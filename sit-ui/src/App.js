import { Provider } from 'react-redux';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import AuthBootstrap from './components/Auth/AuthBootstrap';
import Login from './components/Auth/Login';
import PrivateRoute from './components/Auth/PrivateRoute';
import Layout from './components/Layout/Layout';
import routes from './configs/routes';
import QueryProvider from './providers/QueryProvider';
import { store } from './stores';

function App() {
  return (
    <Provider store={store}>
      <QueryProvider>
        <BrowserRouter basename={process.env.REACT_APP_BASENAME}>
        <AuthBootstrap>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/"
              element={(
                <PrivateRoute>
                  <Layout />
                </PrivateRoute>
              )}
            >
              {routes.map((route) => {
                const element = route.adminOnly ? (
                  <PrivateRoute adminOnly>
                    <route.component />
                  </PrivateRoute>
                ) : (
                  <route.component />
                );
                return (
                  <Route key={route.path} path={route.path} element={element} />
                );
              })}
            </Route>
          </Routes>
        </AuthBootstrap>
        </BrowserRouter>
      </QueryProvider>
    </Provider>
  );
}

export default App;
