import React from 'react';
import { Box, Button, Container, Typography } from '@mui/material';
import keycloak from '../../auth/keycloak';

const LoginPage: React.FC = () => (
  <Container maxWidth="xs">
    <Box sx={{ mt: 16, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
      <Typography variant="h5" fontWeight="bold">Аванс Логистика</Typography>
      <Typography variant="body2" color="text.secondary">Система управления авансами водителей</Typography>
      <Button
        variant="contained"
        size="large"
        fullWidth
        onClick={() => keycloak.login()}
      >
        Войти через Keycloak
      </Button>
    </Box>
  </Container>
);

export default LoginPage;
