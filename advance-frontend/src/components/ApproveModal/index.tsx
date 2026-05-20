import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, Typography, Box, Chip,
} from '@mui/material';
import { AdvanceResponse, ADVANCE_TYPE_LABELS } from '../../types/advance.types';
import { useAppDispatch } from '../../store/store';
import { approveAdvance } from '../../store/advanceSlice';

interface Props {
  advance: AdvanceResponse | null;
  onClose: () => void;
}

const ApproveModal: React.FC<Props> = ({ advance, onClose }) => {
  const dispatch = useAppDispatch();
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);

  if (!advance) return null;

  const handleConfirm = async () => {
    setLoading(true);
    await dispatch(approveAdvance({ id: advance.id, comment }));
    setLoading(false);
    setComment('');
    onClose();
  };

  const scoreColor = advance.scoreAtCreation != null
    ? advance.scoreAtCreation >= 70 ? 'success'
    : advance.scoreAtCreation >= 40 ? 'warning' : 'error'
    : 'default';

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Одобрение заявки {advance.requestNo}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1, mb: 2 }}>
          <Typography variant="body2"><b>Водитель:</b> {advance.driverName}</Typography>
          <Typography variant="body2"><b>Маршрут:</b> {advance.routeName}</Typography>
          <Typography variant="body2"><b>Тип:</b> {ADVANCE_TYPE_LABELS[advance.advanceType]}</Typography>
          <Typography variant="body2"><b>Сумма:</b> {advance.amount.toLocaleString('ru-RU')} ₽</Typography>
          <Typography variant="body2"><b>Остаток лимита:</b> {advance.limitAvailable.toLocaleString('ru-RU')} ₽</Typography>
          {advance.scoreAtCreation != null && (
            <Box>
              <Typography variant="body2" component="span"><b>Скоринг: </b></Typography>
              <Chip label={advance.scoreAtCreation} color={scoreColor as any} size="small" />
            </Box>
          )}
        </Box>
        <TextField
          label="Комментарий"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          multiline
          rows={2}
          fullWidth
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Отмена</Button>
        <Button variant="contained" color="success" onClick={handleConfirm} disabled={loading}>
          Подтвердить
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ApproveModal;
