import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { toast } from 'react-toastify';
import keycloak from '../auth/keycloak';
import { mockAdapter } from '../mocks/mockAdapter';

const useMocks = process.env.REACT_APP_USE_MOCKS === 'true';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  ...(useMocks ? { adapter: mockAdapter } : {}),
});

api.interceptors.request.use((config) => {
  if (!useMocks && keycloak.token) {
    config.headers['Authorization'] = `Bearer ${keycloak.token}`;
  }
  config.headers['X-Request-ID'] = uuidv4();
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (useMocks) return Promise.reject(error);
    const status = error.response?.status;
    if (status === 401) {
      keycloak.login();
    } else if (status === 429) {
      toast.warning('Слишком много запросов, подождите');
    } else if (status >= 500) {
      toast.error('Ошибка сервера: ' + (error.message || 'неизвестная ошибка'));
    }
    return Promise.reject(error);
  }
);

export default api;
