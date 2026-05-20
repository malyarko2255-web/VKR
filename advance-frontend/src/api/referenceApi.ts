import api from './axiosClient';
import { ActiveTrip } from '../types/advance.types';

export interface FuelNorm {
  advanceType: string;
  recommendedAmount: number;
}

export const referenceApi = {
  getActiveTrip: (driverId: string) =>
    api.get<ActiveTrip>(`/api/v1/drivers/${driverId}/active-trip`),

  getFuelNorm: (driverId: string, routeId: string) =>
    api.get<FuelNorm>(`/api/v1/drivers/${driverId}/fuel-norm`, {
      params: { routeId },
    }),

  getDrivers: (params?: { name?: string; page?: number; size?: number }) =>
    api.get('/api/v1/drivers', { params }),

  getRoutes: (params?: { page?: number; size?: number }) =>
    api.get('/api/v1/routes', { params }),
};
