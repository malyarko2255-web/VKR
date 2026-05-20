import { useEffect, useRef, useState } from 'react';
import keycloak from '../auth/keycloak';
import { Notification } from '../types/advance.types';
import { toast } from 'react-toastify';

export function useNotifications(onNew?: () => void) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const esRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!keycloak.token) return;

    const baseUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
    const url = `${baseUrl}/api/v1/notifications/stream?token=${keycloak.token}`;
    const es = new EventSource(url);
    esRef.current = es;

    es.onmessage = (event) => {
      try {
        const n: Notification = JSON.parse(event.data);
        setNotifications((prev) => [n, ...prev]);
        setUnreadCount((c) => c + 1);
        toast.info(n.title);
        onNew?.();
      } catch {
        // ignore parse errors
      }
    };

    es.onerror = () => {
      es.close();
    };

    return () => {
      es.close();
    };
  }, [keycloak.token]);

  const markAllRead = () => {
    setUnreadCount(0);
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  return { notifications, unreadCount, markAllRead };
}
