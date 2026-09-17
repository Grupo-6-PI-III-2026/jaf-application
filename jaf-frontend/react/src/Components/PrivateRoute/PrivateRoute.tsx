import React from 'react';
import { Navigate } from 'react-router-dom';
import { authService } from '../../Service/Auth/Login/authService';

interface PrivateRouteProps {
  children: React.ReactNode;
  requiredPermission?: string;
}


const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, requiredPermission }) => {
  const isAuthenticated = authService.isAuthenticated();

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (requiredPermission && !authService.hasAuthority(requiredPermission)) {
    return <Navigate to="/home" replace />;
  }
  
  return <>{children}</>;
};

export default PrivateRoute;
