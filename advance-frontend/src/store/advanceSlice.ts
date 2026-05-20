import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { advanceApi, AdvanceFilters } from '../api/advanceApi';
import { AdvanceResponse, Page } from '../types/advance.types';
import { toast } from 'react-toastify';

interface AdvanceState {
  items: AdvanceResponse[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  loading: boolean;
  selectedIds: string[];
  bulkProgress: number | null;
}

const initialState: AdvanceState = {
  items: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 20,
  loading: false,
  selectedIds: [],
  bulkProgress: null,
};

export const fetchAdvances = createAsyncThunk(
  'advances/fetchAll',
  async (params: AdvanceFilters) => {
    const res = await advanceApi.getAllAdvances(params);
    return res.data;
  }
);

export const fetchMyAdvances = createAsyncThunk(
  'advances/fetchMy',
  async (params?: AdvanceFilters) => {
    const res = await advanceApi.getMyAdvances(params);
    return res.data;
  }
);

export const approveAdvance = createAsyncThunk(
  'advances/approve',
  async ({ id, comment }: { id: string; comment?: string }) => {
    const res = await advanceApi.approveAdvance(id, comment);
    return res.data;
  }
);

export const rejectAdvance = createAsyncThunk(
  'advances/reject',
  async ({ id, reason }: { id: string; reason: string }) => {
    const res = await advanceApi.rejectAdvance(id, reason);
    return res.data;
  }
);

export const bulkApprove = createAsyncThunk(
  'advances/bulkApprove',
  async (ids: string[], { dispatch }) => {
    const results: AdvanceResponse[] = [];
    for (let i = 0; i < ids.length; i++) {
      const res = await advanceApi.approveAdvance(ids[i]);
      results.push(res.data);
      dispatch(setBulkProgress(Math.round(((i + 1) / ids.length) * 100)));
    }
    return results;
  }
);

const advanceSlice = createSlice({
  name: 'advances',
  initialState,
  reducers: {
    toggleSelection(state, action: PayloadAction<string>) {
      const id = action.payload;
      const idx = state.selectedIds.indexOf(id);
      if (idx >= 0) state.selectedIds.splice(idx, 1);
      else state.selectedIds.push(id);
    },
    selectAll(state) {
      state.selectedIds = state.items.map((a) => a.id);
    },
    clearSelection(state) {
      state.selectedIds = [];
    },
    setBulkProgress(state, action: PayloadAction<number | null>) {
      state.bulkProgress = action.payload;
    },
    updateItem(state, action: PayloadAction<AdvanceResponse>) {
      const idx = state.items.findIndex((a) => a.id === action.payload.id);
      if (idx >= 0) state.items[idx] = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAdvances.pending, (state) => { state.loading = true; })
      .addCase(fetchAdvances.fulfilled, (state, action: PayloadAction<Page<AdvanceResponse>>) => {
        state.loading = false;
        state.items = action.payload.content;
        state.totalElements = action.payload.totalElements;
        state.totalPages = action.payload.totalPages;
        state.page = action.payload.page;
      })
      .addCase(fetchAdvances.rejected, (state) => { state.loading = false; })
      .addCase(fetchMyAdvances.pending, (state) => { state.loading = true; })
      .addCase(fetchMyAdvances.fulfilled, (state, action: PayloadAction<Page<AdvanceResponse>>) => {
        state.loading = false;
        state.items = action.payload.content;
        state.totalElements = action.payload.totalElements;
        state.totalPages = action.payload.totalPages;
        state.page = action.payload.page;
      })
      .addCase(fetchMyAdvances.rejected, (state) => { state.loading = false; })
      .addCase(approveAdvance.fulfilled, (state, action: PayloadAction<AdvanceResponse>) => {
        const idx = state.items.findIndex((a) => a.id === action.payload.id);
        if (idx >= 0) state.items[idx] = action.payload;
        toast.success('Заявка одобрена');
      })
      .addCase(rejectAdvance.fulfilled, (state, action: PayloadAction<AdvanceResponse>) => {
        const idx = state.items.findIndex((a) => a.id === action.payload.id);
        if (idx >= 0) state.items[idx] = action.payload;
        toast.info('Заявка отклонена');
      })
      .addCase(bulkApprove.fulfilled, (state, action: PayloadAction<AdvanceResponse[]>) => {
        action.payload.forEach((updated) => {
          const idx = state.items.findIndex((a) => a.id === updated.id);
          if (idx >= 0) state.items[idx] = updated;
        });
        state.selectedIds = [];
        state.bulkProgress = null;
        toast.success(`Одобрено ${action.payload.length} заявок`);
      })
      .addCase(bulkApprove.rejected, (state) => { state.bulkProgress = null; });
  },
});

export const { toggleSelection, selectAll, clearSelection, setBulkProgress, updateItem } = advanceSlice.actions;
export default advanceSlice.reducer;
