import React, { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { store, useAppDispatch, useAppSelector } from './store/store';
import { setUser, setLoading } from './store/userSlice';
import keycloak from './auth/keycloak';
import { register as registerSW } from './sw/serviceWorker';
import ProtectedRoute from './auth/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import DriverPage from './pages/DriverPage';
import DispatcherDashboard from './pages/DispatcherDashboard';
import { Box, CircularProgress, Typography } from '@mui/material';
import type { UserRole } from './store/userSlice';

registerSW();

const RoleDashboard: React.FC = () => {
  const { role } = useAppSelector((s) => s.user);
  if (role === 'DRIVER') return <DriverPage />;
  if (role === 'DISPATCHER' || role === 'FINANCE_MANAGER' || role === 'FINANCE_DIRECTOR') return <DispatcherDashboard />;
  if (role === 'ADMIN') return <DispatcherDashboard />;
  return (
    <Box sx={{ p: 4 }}>
      <Typography>Доступ запрещён или роль не определена</Typography>
    </Box>
  );
};

const AppInner: React.FC = () => {
  const dispatch = useAppDispatch();
  const { loading } = useAppSelector((s) => s.user);

  useEffect(() => {
    keycloak
      .init({ onLoad: 'check-sso', silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html' })
      .then((authenticated) => {
        if (authenticated && keycloak.tokenParsed) {
          const tp = keycloak.tokenParsed as any;
          const roles: string[] = tp.roles ?? tp.realm_access?.roles ?? [];
          const role = (['ADMIN', 'DISPATCHER', 'FINANCE_DIRECTOR', 'FINANCE_MANAGER', 'DRIVER']
            .find((r) => roles.includes(r)) ?? null) as UserRole | null;
          dispatch(setUser({
            authenticated: true,
            id: tp.sub ?? null,
            username: tp.preferred_username ?? null,
            role,
            driverId: tp.driver_id ?? tp.sub ?? null,
          }));
        } else {
          dispatch(setLoading(false));
        }
      })
      .catch(() => dispatch(setLoading(false)));
  }, [dispatch]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<RoleDashboard />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <ToastContainer position="bottom-right" autoClose={4000} />
    </BrowserRouter>
  );
};

const App: React.FC = () => (
  <Provider store={store}>
    <AppInner />
  </Provider>
);

export default App;
