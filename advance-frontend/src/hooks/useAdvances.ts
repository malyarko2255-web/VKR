import { useCallback, useEffect } from 'react';
import { openDB } from 'idb';
import { v4 as uuidv4 } from 'uuid';
import { toast } from 'react-toastify';
import { useAppDispatch, useAppSelector } from '../store/store';
import { fetchMyAdvances, fetchAdvances } from '../store/advanceSlice';
import { advanceApi } from '../api/advanceApi';
import { AdvanceRequest } from '../types/advance.types';
import { AdvanceFilters } from '../api/advanceApi';

const DB_NAME = 'advance-pwa';
const STORE_NAME = 'pending_advances';

async function getDb() {
  return openDB(DB_NAME, 1, {
    upgrade(db) {
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    },
  });
}

async function savePendingAdvance(data: AdvanceRequest) {
  const db = await getDb();
  await db.put(STORE_NAME, { id: uuidv4(), ...data, createdAt: Date.now() });
}

async function flushPendingAdvances() {
  const db = await getDb();
  const all = await db.getAll(STORE_NAME);
  for (const pending of all) {
    try {
      const { id, createdAt, ...req } = pending;
      await advanceApi.createAdvance(req as AdvanceRequest);
      await db.delete(STORE_NAME, id);
    } catch {
      // keep for next retry
    }
  }
}

export function useAdvances(mode: 'my' | 'all' = 'my', filters?: AdvanceFilters) {
  const dispatch = useAppDispatch();
  const { items, loading, totalElements, totalPages } = useAppSelector((s) => s.advances);

  const refetch = useCallback(() => {
    if (mode === 'my') dispatch(fetchMyAdvances(filters));
    else dispatch(fetchAdvances(filters || {}));
  }, [dispatch, mode, filters]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  useEffect(() => {
    window.addEventListener('online', flushPendingAdvances);
    return () => window.removeEventListener('online', flushPendingAdvances);
  }, []);

  const submitAdvance = useCallback(async (data: AdvanceRequest) => {
    if (!navigator.onLine) {
      await savePendingAdvance(data);
      toast.info('Нет сети. Заявка будет отправлена автоматически.');
      return;
    }
    await advanceApi.createAdvance(data);
    toast.success('Заявка отправлена');
    refetch();
  }, [refetch]);

  return { items, loading, totalElements, totalPages, refetch, submitAdvance };
}
