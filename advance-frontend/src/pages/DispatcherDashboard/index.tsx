import React, { useCallback, useEffect, useState } from 'react';
import {
  AppBar, Box, Button, Dialog, DialogActions, DialogContent,
  DialogTitle, LinearProgress, TextField, Toolbar, Typography,
} from '@mui/material';
import { useAppDispatch, useAppSelector } from '../../store/store';
import { fetchAdvances, bulkApprove, rejectAdvance } from '../../store/advanceSlice';
import { AdvanceResponse } from '../../types/advance.types';
import { AdvanceFilters } from '../../api/advanceApi';
import { useNotifications } from '../../hooks/useNotifications';
import { useInstallPrompt } from '../../hooks/useInstallPrompt';
import NotificationBell from '../../components/NotificationBell';
import ApproveModal from '../../components/ApproveModal';
import AdvanceQueue from './AdvanceQueue';
import FiltersPanel from './FiltersPanel';

const DEFAULT_FILTERS: AdvanceFilters = { page: 0, size: 50 };

const DispatcherDashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const { selectedIds, bulkProgress } = useAppSelector((s) => s.advances);
  const [filters, setFilters] = useState<AdvanceFilters>(DEFAULT_FILTERS);
  const [approveTarget, setApproveTarget] = useState<AdvanceResponse | null>(null);
  const [rejectTarget, setRejectTarget] = useState<AdvanceResponse | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectError, setRejectError] = useState('');

  const refetch = useCallback(() => dispatch(fetchAdvances(filters)), [dispatch, filters]);

  const { notifications, unreadCount, markAllRead } = useNotifications(refetch);
  const { canInstall, install } = useInstallPrompt();

  useEffect(() => {
    refetch();
  }, [refetch]);

  useEffect(() => {
    const id = setTimeout(() => refetch(), 10_000);
    return () => clearTimeout(id);
  });

  const handleBulkApprove = () => dispatch(bulkApprove(selectedIds));

  const handleRejectConfirm = async () => {
    if (rejectReason.trim().length < 10) {
      setRejectError('Минимум 10 символов');
      return;
    }
    if (!rejectTarget) return;
    await dispatch(rejectAdvance({ id: rejectTarget.id, reason: rejectReason }));
    setRejectTarget(null);
    setRejectReason('');
    setRejectError('');
  };

  return (
    <Box sx={{ minWidth: 1024 }}>
      <AppBar position="sticky">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Панель диспетчера</Typography>
          {canInstall && (
            <Button color="inherit" size="small" onClick={install} sx={{ mr: 1 }}>
              Установить
            </Button>
          )}
          <NotificationBell
            unreadCount={unreadCount}
            notifications={notifications}
            onMarkAllRead={markAllRead}
          />
          {selectedIds.length > 0 && (
            <Button
              color="inherit"
              variant="outlined"
              sx={{ ml: 2, borderColor: 'white' }}
              onClick={handleBulkApprove}
              disabled={bulkProgress !== null}
            >
              Одобрить выбранные ({selectedIds.length})
            </Button>
          )}
        </Toolbar>
        {bulkProgress !== null && (
          <LinearProgress variant="determinate" value={bulkProgress} color="secondary" />
        )}
      </AppBar>

      <FiltersPanel
        filters={filters}
        onChange={(f) => setFilters({ ...DEFAULT_FILTERS, ...f })}
        onReset={() => setFilters(DEFAULT_FILTERS)}
      />

      <Box sx={{ p: 2 }}>
        <AdvanceQueue
          onApprove={setApproveTarget}
          onReject={(a) => { setRejectTarget(a); setRejectReason(''); setRejectError(''); }}
        />
      </Box>

      {approveTarget && (
        <ApproveModal advance={approveTarget} onClose={() => setApproveTarget(null)} />
      )}

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Отклонение заявки {rejectTarget?.requestNo}</DialogTitle>
        <DialogContent>
          <TextField
            label="Причина отклонения"
            value={rejectReason}
            onChange={(e) => { setRejectReason(e.target.value); setRejectError(''); }}
            fullWidth
            required
            multiline
            rows={3}
            error={!!rejectError}
            helperText={rejectError || 'Минимум 10 символов'}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectTarget(null)}>Отмена</Button>
          <Button variant="contained" color="error" onClick={handleRejectConfirm}>
            Отклонить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DispatcherDashboard;
