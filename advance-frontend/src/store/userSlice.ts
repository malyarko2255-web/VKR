import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type UserRole = 'DRIVER' | 'DISPATCHER' | 'FINANCE_MANAGER' | 'FINANCE_DIRECTOR' | 'ADMIN';

interface UserState {
  authenticated: boolean;
  loading: boolean;
  id: string | null;
  username: string | null;
  role: UserRole | null;
  driverId: string | null;
}

const initialState: UserState = {
  authenticated: false,
  loading: true,
  id: null,
  username: null,
  role: null,
  driverId: null,
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser(state, action: PayloadAction<Omit<UserState, 'loading'>>) {
      Object.assign(state, action.payload, { loading: false });
    },
    setLoading(state, action: PayloadAction<boolean>) {
      state.loading = action.payload;
    },
    clearUser(state) {
      Object.assign(state, { ...initialState, loading: false });
    },
  },
});

export const { setUser, setLoading, clearUser } = userSlice.actions;
export default userSlice.reducer;
