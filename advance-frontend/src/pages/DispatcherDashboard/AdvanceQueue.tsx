import React, { useEffect, useState } from 'react';
import {
  Box, Checkbox, Chip, IconButton, LinearProgress,
  Paper, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Tooltip, Typography,
} from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import { AdvanceResponse, AdvanceStatus, AdvanceType, ADVANCE_TYPE_LABELS } from '../../types/advance.types';
import { useAppDispatch, useAppSelector } from '../../store/store';
import { toggleSelection, selectAll, clearSelection } from '../../store/advanceSlice';
import StatusBadge from '../../components/StatusBadge';

interface Props {
  onApprove: (advance: AdvanceResponse) => void;
  onReject: (advance: AdvanceResponse) => void;
}

function waitingMinutes(createdAt: string): number {
  return Math.round((Date.now() - new Date(createdAt).getTime()) / 60000);
}

function rowBg(advance: AdvanceResponse): string | undefined {
  const waiting = waitingMinutes(advance.createdAt);
  if (advance.advanceType === AdvanceType.REPAIR && waiting > 30) return '#ffebee';
  if (advance.amount > 0.5 * advance.limitAvailable) return '#fffde7';
  return undefined;
}

const AdvanceQueue: React.FC<Props> = ({ onApprove, onReject }) => {
  const dispatch = useAppDispatch();
  const { items, loading, selectedIds, bulkProgress } = useAppSelector((s) => s.advances);
  const [, tick] = useState(0);

  useEffect(() => {
    const id = setInterval(() => tick((n) => n + 1), 30_000);
    return () => clearInterval(id);
  }, []);

  const allSelected = items.length > 0 && selectedIds.length === items.length;

  return (
    <TableContainer component={Paper}>
      {loading && <LinearProgress />}
      {bulkProgress !== null && (
        <LinearProgress variant="determinate" value={bulkProgress} color="success" />
      )}
      <Table size="small" stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell padding="checkbox">
              <Checkbox
                checked={allSelected}
                indeterminate={selectedIds.length > 0 && !allSelected}
                onChange={() => allSelected ? dispatch(clearSelection()) : dispatch(selectAll())}
              />
            </TableCell>
            <TableCell>№ заявки</TableCell>
            <TableCell>Водитель</TableCell>
            <TableCell>Маршрут</TableCell>
            <TableCell>Тип</TableCell>
            <TableCell align="right">Сумма</TableCell>
            <TableCell align="right">Лимит(ост.)</TableCell>
            <TableCell align="center">Скоринг</TableCell>
            <TableCell>Ожидание</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Действия</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((a) => {
            const scoreColor = a.scoreAtCreation != null
              ? a.scoreAtCreation >= 70 ? 'success' : a.scoreAtCreation >= 40 ? 'warning' : 'error'
              : 'default';
            return (
              <TableRow key={a.id} sx={{ bgcolor: rowBg(a) }}>
                <TableCell padding="checkbox">
                  <Checkbox
                    checked={selectedIds.includes(a.id)}
                    onChange={() => dispatch(toggleSelection(a.id))}
                  />
                </TableCell>
                <TableCell>{a.requestNo}</TableCell>
                <TableCell>{a.driverName}</TableCell>
                <TableCell>{a.routeName}</TableCell>
                <TableCell>{ADVANCE_TYPE_LABELS[a.advanceType]}</TableCell>
                <TableCell align="right">{a.amount.toLocaleString('ru-RU')} ₽</TableCell>
                <TableCell align="right">{a.limitAvailable.toLocaleString('ru-RU')} ₽</TableCell>
                <TableCell align="center">
                  {a.scoreAtCreation != null
                    ? <Chip label={a.scoreAtCreation} color={scoreColor as any} size="small" />
                    : <Typography variant="caption">—</Typography>}
                </TableCell>
                <TableCell>{waitingMinutes(a.createdAt)} мин</TableCell>
                <TableCell><StatusBadge status={a.status} /></TableCell>
                <TableCell>
                  {[AdvanceStatus.PENDING, AdvanceStatus.DISPATCHER_REVIEW].includes(a.status) && (
                    <Box sx={{ display: 'flex' }}>
                      <Tooltip title="Одобрить">
                        <IconButton size="small" color="success" onClick={() => onApprove(a)}>
                          <CheckCircleOutlineIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Отклонить">
                        <IconButton size="small" color="error" onClick={() => onReject(a)}>
                          <CancelOutlinedIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
          {items.length === 0 && !loading && (
            <TableRow>
              <TableCell colSpan={11} align="center">
                <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                  Заявок нет
                </Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default AdvanceQueue;
