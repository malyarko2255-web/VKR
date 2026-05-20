import React from 'react';
import { Chip } from '@mui/material';
import { AdvanceStatus } from '../../types/advance.types';

const STATUS_LABELS: Record<AdvanceStatus, string> = {
  [AdvanceStatus.PENDING]: 'Ожидает',
  [AdvanceStatus.DISPATCHER_REVIEW]: 'У диспетчера',
  [AdvanceStatus.FINANCE_REVIEW]: 'У финансов',
  [AdvanceStatus.APPROVED]: 'Одобрено',
  [AdvanceStatus.REJECTED]: 'Отклонено',
  [AdvanceStatus.PAID]: 'Выплачено',
  [AdvanceStatus.CANCELLED]: 'Отменено',
};

const STATUS_COLORS: Record<AdvanceStatus, 'default' | 'primary' | 'success' | 'error' | 'warning'> = {
  [AdvanceStatus.PENDING]: 'default',
  [AdvanceStatus.DISPATCHER_REVIEW]: 'default',
  [AdvanceStatus.FINANCE_REVIEW]: 'warning',
  [AdvanceStatus.APPROVED]: 'primary',
  [AdvanceStatus.REJECTED]: 'error',
  [AdvanceStatus.PAID]: 'success',
  [AdvanceStatus.CANCELLED]: 'default',
};

interface Props {
  status: AdvanceStatus;
  size?: 'small' | 'medium';
}

const StatusBadge: React.FC<Props> = ({ status, size = 'small' }) => (
  <Chip
    label={STATUS_LABELS[status] ?? status}
    color={STATUS_COLORS[status] ?? 'default'}
    size={size}
  />
);

export default StatusBadge;
