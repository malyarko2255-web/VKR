export enum AdvanceStatus {
  PENDING = 'PENDING',
  DISPATCHER_REVIEW = 'DISPATCHER_REVIEW',
  FINANCE_REVIEW = 'FINANCE_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  PAID = 'PAID',
  CANCELLED = 'CANCELLED',
}

export enum AdvanceType {
  FUEL = 'FUEL',
  LOADING_UNLOADING = 'LOADING_UNLOADING',
  PER_DIEM = 'PER_DIEM',
  REPAIR = 'REPAIR',
  PLANNED = 'PLANNED',
}

export const ADVANCE_TYPE_LABELS: Record<AdvanceType, string> = {
  [AdvanceType.FUEL]: 'ГСМ',
  [AdvanceType.LOADING_UNLOADING]: 'ПРР',
  [AdvanceType.PER_DIEM]: 'Суточные',
  [AdvanceType.REPAIR]: 'Ремонт',
  [AdvanceType.PLANNED]: 'Плановый',
};

export enum TripStage {
  LOADING = 'LOADING',
  IN_TRANSIT = 'IN_TRANSIT',
  UNLOADING = 'UNLOADING',
  RETURN = 'RETURN',
}

export const TRIP_STAGE_LABELS: Record<TripStage, string> = {
  [TripStage.LOADING]: 'Загрузка',
  [TripStage.IN_TRANSIT]: 'В пути',
  [TripStage.UNLOADING]: 'Разгрузка',
  [TripStage.RETURN]: 'Возврат',
};

export interface AdvanceRequest {
  driverId: string;
  routeId: string;
  tripStage: TripStage;
  advanceType: AdvanceType;
  amount: number;
  notes?: string;
}

export interface AdvanceResponse {
  id: string;
  requestNo: string;
  driverName: string;
  routeName: string;
  advanceType: AdvanceType;
  amount: number;
  limitAvailable: number;
  status: AdvanceStatus;
  scoreAtCreation?: number;
  autoApproved?: boolean;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DriverLimit {
  advanceType: AdvanceType;
  available: number;
  monthlyLimit: number;
  used: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface ActiveTrip {
  routeId: string;
  routeName: string;
  tripStage: TripStage;
  departureDate: string;
}

export interface Notification {
  id: string;
  title: string;
  body: string;
  isRead: boolean;
  createdAt: string;
}
