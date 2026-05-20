import api from './axiosClient';
import { AdvanceRequest, AdvanceResponse, DriverLimit, Page } from '../types/advance.types';

export interface AdvanceFilters {
  status?: string;
  driverName?: string;
  advanceType?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export const advanceApi = {
  getMyAdvances: (params?: AdvanceFilters) =>
    api.get<Page<AdvanceResponse>>('/api/v1/advances/my', { params }),

  getAllAdvances: (params?: AdvanceFilters) =>
    api.get<Page<AdvanceResponse>>('/api/v1/advances', { params }),

  createAdvance: (data: AdvanceRequest) =>
    api.post<AdvanceResponse>('/api/v1/advances', data),

  approveAdvance: (id: string, comment?: string) =>
    api.post<AdvanceResponse>(`/api/v1/advances/${id}/approve`, { comment }),

  rejectAdvance: (id: string, reason: string) =>
    api.post<AdvanceResponse>(`/api/v1/advances/${id}/reject`, { reason }),

  getDriverLimits: (driverId: string) =>
    api.get<DriverLimit[]>(`/api/v1/advances/limits/${driverId}`),

  attachReceipt: (id: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/api/v1/advances/${id}/receipt`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
