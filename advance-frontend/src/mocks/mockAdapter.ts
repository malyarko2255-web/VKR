import { InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import {
  mockActiveTrip,
  mockLimits,
  mockPageOf,
  myAdvances,
  dispatcherQueue,
  MOCK_DRIVER_ID,
} from './mockData';
import { AdvanceStatus } from '../types/advance.types';
import { v4 as uuidv4 } from 'uuid';

function ok(config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse {
  return { data, status, statusText: 'OK', headers: {}, config, request: {} };
}

function matchPath(url: string, pattern: RegExp): RegExpMatchArray | null {
  return url.match(pattern);
}

export async function mockAdapter(config: InternalAxiosRequestConfig): Promise<AxiosResponse> {
  const url = config.url ?? '';
  const method = (config.method ?? 'get').toLowerCase();

  await new Promise((r) => setTimeout(r, 80 + Math.random() * 120));

  // GET /api/v1/advances/my
  if (method === 'get' && url.includes('/advances/my')) {
    return ok(config, mockPageOf(myAdvances));
  }

  // GET /api/v1/advances (dispatcher queue)
  if (method === 'get' && /\/api\/v1\/advances(\?|$)/.test(url)) {
    return ok(config, mockPageOf(dispatcherQueue));
  }

  // GET /api/v1/drivers/{id}/active-trip
  if (method === 'get' && url.includes('/active-trip')) {
    return ok(config, mockActiveTrip);
  }

  // GET /api/v1/advances/limits/{driverId}
  if (method === 'get' && url.includes('/limits/')) {
    return ok(config, mockLimits);
  }

  // GET /api/v1/drivers/{id}/fuel-norm
  if (method === 'get' && url.includes('/fuel-norm')) {
    return ok(config, { advanceType: 'FUEL', recommendedAmount: 3200 });
  }

  // POST /api/v1/advances (create)
  if (method === 'post' && /\/api\/v1\/advances$/.test(url)) {
    const body = JSON.parse(config.data ?? '{}');
    const created = {
      id: uuidv4(),
      requestNo: `ADV-2026-0${Math.floor(Math.random() * 9000 + 1000)}`,
      driverName: 'Иванов Иван Петрович',
      routeName: mockActiveTrip.routeName,
      advanceType: body.advanceType,
      amount: body.amount,
      limitAvailable: 40700,
      status: AdvanceStatus.PENDING,
      scoreAtCreation: 87,
      autoApproved: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    myAdvances.unshift(created as any);
    return ok(config, created, 201);
  }

  // POST /api/v1/advances/{id}/approve
  const approveMatch = matchPath(url, /\/advances\/([^/]+)\/approve/);
  if (method === 'post' && approveMatch) {
    const id = approveMatch[1];
    const item = dispatcherQueue.find((a) => a.id === id);
    if (item) {
      (item as any).status = AdvanceStatus.APPROVED;
      dispatcherQueue.splice(dispatcherQueue.indexOf(item), 1);
    }
    return ok(config, { ...item, status: AdvanceStatus.APPROVED });
  }

  // POST /api/v1/advances/{id}/reject
  const rejectMatch = matchPath(url, /\/advances\/([^/]+)\/reject/);
  if (method === 'post' && rejectMatch) {
    const id = rejectMatch[1];
    const item = dispatcherQueue.find((a) => a.id === id);
    if (item) {
      (item as any).status = AdvanceStatus.REJECTED;
      dispatcherQueue.splice(dispatcherQueue.indexOf(item), 1);
    }
    return ok(config, { ...item, status: AdvanceStatus.REJECTED });
  }

  // Notifications SSE / fallback
  if (method === 'get' && url.includes('/notifications')) {
    return ok(config, { content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 });
  }

  // Default fallback
  return ok(config, {});
}
