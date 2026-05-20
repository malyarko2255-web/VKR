import React from 'react';
import {
  Box, Button, FormControl, InputLabel, MenuItem, Select, TextField,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs, { Dayjs } from 'dayjs';
import { AdvanceStatus, AdvanceType, ADVANCE_TYPE_LABELS } from '../../types/advance.types';
import { AdvanceFilters } from '../../api/advanceApi';

interface Props {
  filters: AdvanceFilters;
  onChange: (f: AdvanceFilters) => void;
  onReset: () => void;
}

const STATUS_LABELS: Partial<Record<AdvanceStatus, string>> = {
  [AdvanceStatus.PENDING]: 'Ожидает',
  [AdvanceStatus.DISPATCHER_REVIEW]: 'У диспетчера',
  [AdvanceStatus.FINANCE_REVIEW]: 'У финансов',
  [AdvanceStatus.APPROVED]: 'Одобрено',
  [AdvanceStatus.REJECTED]: 'Отклонено',
};

const FiltersPanel: React.FC<Props> = ({ filters, onChange, onReset }) => {
  let debounceTimer: ReturnType<typeof setTimeout>;

  const handleDriverName = (val: string) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => onChange({ ...filters, driverName: val || undefined }), 300);
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center', p: 2 }}>
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Статус</InputLabel>
          <Select
            value={filters.status ?? ''}
            label="Статус"
            onChange={(e) => onChange({ ...filters, status: e.target.value || undefined })}
          >
            <MenuItem value="">Все</MenuItem>
            {Object.entries(STATUS_LABELS).map(([k, v]) => (
              <MenuItem key={k} value={k}>{v}</MenuItem>
            ))}
          </Select>
        </FormControl>

        <TextField
          size="small"
          label="Имя водителя"
          defaultValue={filters.driverName ?? ''}
          onChange={(e) => handleDriverName(e.target.value)}
          sx={{ minWidth: 180 }}
        />

        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Тип аванса</InputLabel>
          <Select
            value={filters.advanceType ?? ''}
            label="Тип аванса"
            onChange={(e) => onChange({ ...filters, advanceType: e.target.value || undefined })}
          >
            <MenuItem value="">Все</MenuItem>
            {Object.values(AdvanceType).map((t) => (
              <MenuItem key={t} value={t}>{ADVANCE_TYPE_LABELS[t]}</MenuItem>
            ))}
          </Select>
        </FormControl>

        <DatePicker
          label="С"
          value={filters.dateFrom ? dayjs(filters.dateFrom) : null}
          onChange={(d: Dayjs | null) => onChange({ ...filters, dateFrom: d?.toISOString() })}
          slotProps={{ textField: { size: 'small', sx: { width: 150 } } }}
        />

        <DatePicker
          label="По"
          value={filters.dateTo ? dayjs(filters.dateTo) : null}
          onChange={(d: Dayjs | null) => onChange({ ...filters, dateTo: d?.toISOString() })}
          slotProps={{ textField: { size: 'small', sx: { width: 150 } } }}
        />

        <Button variant="outlined" size="small" onClick={onReset}>Сбросить</Button>
      </Box>
    </LocalizationProvider>
  );
};

export default FiltersPanel;
