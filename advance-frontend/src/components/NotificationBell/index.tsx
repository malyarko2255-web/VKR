import React, { useState } from 'react';
import {
  Badge, IconButton, Popover, List, ListItem,
  ListItemText, Typography, Box, Button,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { Notification } from '../../types/advance.types';

interface Props {
  unreadCount: number;
  notifications: Notification[];
  onMarkAllRead: () => void;
}

const NotificationBell: React.FC<Props> = ({ unreadCount, notifications, onMarkAllRead }) => {
  const [anchor, setAnchor] = useState<HTMLButtonElement | null>(null);

  return (
    <>
      <IconButton color="inherit" onClick={(e) => setAnchor(e.currentTarget)}>
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      <Popover
        open={!!anchor}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ width: 320, maxHeight: 400, overflow: 'auto' }}>
          <Box sx={{ p: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="subtitle2">Уведомления</Typography>
            {unreadCount > 0 && (
              <Button size="small" onClick={onMarkAllRead}>Прочитать все</Button>
            )}
          </Box>
          {notifications.length === 0 ? (
            <Typography variant="body2" sx={{ p: 2, color: 'text.secondary' }}>
              Нет уведомлений
            </Typography>
          ) : (
            <List dense>
              {notifications.map((n) => (
                <ListItem key={n.id} sx={{ bgcolor: n.isRead ? 'transparent' : 'action.hover' }}>
                  <ListItemText
                    primary={n.title}
                    secondary={n.body}
                    primaryTypographyProps={{ fontWeight: n.isRead ? 'normal' : 'bold' }}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Popover>
    </>
  );
};

export default NotificationBell;
