import React, { useEffect, useState } from 'react';
import {
  Box, Button, CircularProgress, FormControl,
  InputLabel, MenuItem, Select, TextField,
} from '@mui/material';
import { AdvanceType, ADVANCE_TYPE_LABELS, TripStage, TRIP_STAGE_LABELS, DriverLimit } from '../../types/advance.types';
import { referenceApi } from '../../api/referenceApi';
import { useAppSelector } from '../../store/store';

const FIXED_AMOUNTS: Partial<Record<AdvanceType, number>> = {
  [AdvanceType.PER_DIEM]: 2800,
  [AdvanceType.LOADING_UNLOADING]: 5000,
  [AdvanceType.PLANNED]: 10000,
};

interface Props {
  limit: DriverLimit | null;
  currentStage: TripStage | null;
  currentRouteId: string | null;
  onSubmit: (data: {
    advanceType: AdvanceType;
    amount: number;
    tripStage: TripStage;
    notes: string;
  }) => Promise<void>;
}

const AdvanceRequestForm: React.FC<Props> = ({ limit, currentStage, currentRouteId, onSubmit }) => {
  const { driverId } = useAppSelector((s) => s.user);
  const [advanceType, setAdvanceType] = useState<AdvanceType>(AdvanceType.FUEL);
  const [amount, setAmount] = useState('');
  const [tripStage, setTripStage] = useState<TripStage>(currentStage ?? TripStage.IN_TRANSIT);
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (currentStage) setTripStage(currentStage);
  }, [currentStage]);

  useEffect(() => {
    const prefill = async () => {
      if (advanceType === AdvanceType.FUEL && driverId && currentRouteId) {
        try {
          const res = await referenceApi.getFuelNorm(driverId, currentRouteId);
          setAmount(String(res.data.recommendedAmount));
        } catch {
          setAmount('');
        }
      } else if (FIXED_AMOUNTS[advanceType] !== undefined) {
        setAmount(String(FIXED_AMOUNTS[advanceType]));
      } else {
        setAmount('');
      }
    };
    prefill();
  }, [advanceType, driverId, currentRouteId]);

  const maxAmount = limit?.available ?? 0;
  const numAmount = Number(amount);
  const isValid = numAmount >= 100 && numAmount <= maxAmount && tripStage;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    setLoading(true);
    await onSubmit({ advanceType, amount: numAmount, tripStage, notes });
    setLoading(false);
    setAmount('');
    setNotes('');
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <FormControl fullWidth>
        <InputLabel>Тип расхода</InputLabel>
        <Select
          value={advanceType}
          label="Тип расхода"
          onChange={(e) => setAdvanceType(e.target.value as AdvanceType)}
        >
          {Object.values(AdvanceType).map((t) => (
            <MenuItem key={t} value={t}>{ADVANCE_TYPE_LABELS[t]}</MenuItem>
          ))}
        </Select>
      </FormControl>

      <TextField
        label="Сумма (руб.)"
        type="number"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        inputProps={{ min: 100, max: maxAmount, step: 100 }}
        helperText={`Доступно: ${maxAmount.toLocaleString('ru-RU')} ₽`}
        required
      />

      <FormControl fullWidth>
        <InputLabel>Этап рейса</InputLabel>
        <Select
          value={tripStage}
          label="Этап рейса"
          onChange={(e) => setTripStage(e.target.value as TripStage)}
        >
          {Object.values(TripStage).map((s) => (
            <MenuItem key={s} value={s}>{TRIP_STAGE_LABELS[s]}</MenuItem>
          ))}
        </Select>
      </FormControl>

      <TextField
        label="Примечание"
        multiline
        rows={2}
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
      />

      <Button
        type="submit"
        variant="contained"
        disabled={!isValid || loading}
        startIcon={loading ? <CircularProgress size={16} /> : null}
      >
        Отправить заявку
      </Button>
    </Box>
  );
};

export default AdvanceRequestForm;
