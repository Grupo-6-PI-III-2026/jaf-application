import React from 'react';
import { Navigate } from 'react-router-dom';
import { authService } from '../../Service/Auth/Login/authService';

interface PrivateRouteProps {
  children: React.ReactNode;
}


const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
  const isAuthenticated = authService.isAuthenticated();
  
  return isAuthenticated ? <>{children}</> : <Navigate to="/" replace />;
};

export default PrivateRoute;
