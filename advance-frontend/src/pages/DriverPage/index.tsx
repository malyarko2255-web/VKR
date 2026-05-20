import React, { useEffect, useRef, useState } from 'react';
import {
  Alert, AppBar, Box, Card, CardContent, CardHeader,
  IconButton, Toolbar, Typography,
} from '@mui/material';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import { useAppSelector } from '../../store/store';
import { referenceApi } from '../../api/referenceApi';
import { advanceApi } from '../../api/advanceApi';
import { AdvanceType, ActiveTrip, DriverLimit, TRIP_STAGE_LABELS, ADVANCE_TYPE_LABELS, AdvanceStatus } from '../../types/advance.types';
import { useAdvances } from '../../hooks/useAdvances';
import { useNotifications } from '../../hooks/useNotifications';
import { useInstallPrompt } from '../../hooks/useInstallPrompt';
import NotificationBell from '../../components/NotificationBell';
import LimitIndicator from '../../components/LimitIndicator';
import StatusBadge from '../../components/StatusBadge';
import AdvanceRequestForm from './AdvanceRequestForm';
import Button from '@mui/material/Button';

const DriverPage: React.FC = () => {
  const { username, driverId } = useAppSelector((s) => s.user);
  const [activeTrip, setActiveTrip] = useState<ActiveTrip | null>(null);
  const [limit, setLimit] = useState<DriverLimit | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [receiptAdvanceId, setReceiptAdvanceId] = useState<string | null>(null);

  const { items, loading, refetch, submitAdvance } = useAdvances('my');
  const { notifications, unreadCount, markAllRead } = useNotifications(refetch);
  const { canInstall, install } = useInstallPrompt();

  useEffect(() => {
    if (!driverId) return;
    referenceApi.getActiveTrip(driverId)
      .then((r) => setActiveTrip(r.data))
      .catch(() => setActiveTrip(null));
    advanceApi.getDriverLimits(driverId)
      .then((r) => setLimit(r.data.find((l) => l.advanceType === AdvanceType.FUEL) ?? r.data[0] ?? null))
      .catch(() => setLimit(null));
  }, [driverId]);

  const handleSubmit = async (form: any) => {
    if (!driverId || !activeTrip) return;
    await submitAdvance({
      driverId,
      routeId: activeTrip.routeId,
      ...form,
    });
  };

  const handleAttach = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !receiptAdvanceId) return;
    await advanceApi.attachReceipt(receiptAdvanceId, file);
    setReceiptAdvanceId(null);
  };

  return (
    <Box sx={{ maxWidth: 480, mx: 'auto', pb: 4 }}>
      <AppBar position="sticky">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>{username ?? 'Водитель'}</Typography>
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
        </Toolbar>
      </AppBar>

      <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
        {/* Active trip */}
        <Card>
          <CardHeader title="Текущий рейс" />
          <CardContent>
            {activeTrip ? (
              <>
                <Typography><b>Маршрут:</b> {activeTrip.routeName}</Typography>
                <Typography><b>Этап:</b> {TRIP_STAGE_LABELS[activeTrip.tripStage]}</Typography>
                <Typography variant="caption" color="text.secondary">
                  Выезд: {new Date(activeTrip.departureDate).toLocaleDateString('ru-RU')}
                </Typography>
              </>
            ) : (
              <Alert severity="warning">Нет активного рейса</Alert>
            )}
          </CardContent>
        </Card>

        {/* Limit */}
        {limit && (
          <Card>
            <CardHeader title="Лимит аванса" />
            <CardContent>
              <LimitIndicator limit={limit} />
            </CardContent>
          </Card>
        )}

        {/* Form */}
        <Card>
          <CardHeader title="Новая заявка" />
          <CardContent>
            <AdvanceRequestForm
              limit={limit}
              currentStage={activeTrip?.tripStage ?? null}
              currentRouteId={activeTrip?.routeId ?? null}
              onSubmit={handleSubmit}
            />
          </CardContent>
        </Card>

        {/* My advances */}
        <Typography variant="h6">Мои заявки</Typography>
        {loading && <Typography color="text.secondary">Загрузка…</Typography>}
        {items.map((a) => (
          <Card key={a.id}>
            <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Box>
                <Typography variant="body2" fontWeight="bold">{a.requestNo}</Typography>
                <Typography variant="body2">{ADVANCE_TYPE_LABELS[a.advanceType]} · {a.amount.toLocaleString('ru-RU')} ₽</Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(a.createdAt).toLocaleString('ru-RU')}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 0.5 }}>
                <StatusBadge status={a.status} />
                {a.status === AdvanceStatus.PAID && (
                  <IconButton
                    size="small"
                    onClick={() => {
                      setReceiptAdvanceId(a.id);
                      fileInputRef.current?.click();
                    }}
                  >
                    <AttachFileIcon fontSize="small" />
                  </IconButton>
                )}
              </Box>
            </CardContent>
          </Card>
        ))}
        <input ref={fileInputRef} type="file" hidden accept="image/*,.pdf" onChange={handleAttach} />
      </Box>
    </Box>
  );
};

export default DriverPage;
