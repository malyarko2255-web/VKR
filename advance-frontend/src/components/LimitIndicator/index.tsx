import React from 'react';
import { Box, LinearProgress, Typography } from '@mui/material';
import { DriverLimit } from '../../types/advance.types';

interface Props {
  limit: DriverLimit;
}

const LimitIndicator: React.FC<Props> = ({ limit }) => {
  const ratio = limit.monthlyLimit > 0 ? limit.used / limit.monthlyLimit : 0;
  const pct = Math.min(ratio * 100, 100);

  const color: 'success' | 'warning' | 'error' =
    ratio < 0.6 ? 'success' : ratio < 0.8 ? 'warning' : 'error';

  return (
    <Box sx={{ mb: 2 }}>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={color}
        sx={{ height: 10, borderRadius: 5 }}
      />
      <Typography variant="caption" color="text.secondary">
        Использовано {limit.used.toLocaleString('ru-RU')} из {limit.monthlyLimit.toLocaleString('ru-RU')} руб.
      </Typography>
    </Box>
  );
};

export default LimitIndicator;
