import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8088',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'advance',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'advance-frontend',
});

export default keycloak;
