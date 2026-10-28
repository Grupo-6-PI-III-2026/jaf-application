import React, { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { toast } from 'sonner';
import { authService } from '../../Service/Auth/Login/authService';
import { useUser } from '../../Context/useUser';

interface PrivateRouteProps {
  children: React.ReactNode;
  requiredPermission?: string;
  adminOnly?: boolean;
  deniedMessage?: string;
}


const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, requiredPermission, adminOnly, deniedMessage }) => {
  const { user, isLoading, error, refreshUser } = useUser();
  const isAuthenticated = authService.isAuthenticated();
  const validado = isAuthenticated && !error && !isLoading && Boolean(user);
  const negado = validado && Boolean((adminOnly && !authService.isAdmin()) ||
    (requiredPermission && !authService.hasAuthority(requiredPermission)));

  useEffect(() => {
    if (negado && deniedMessage) toast.error(deniedMessage, { id: deniedMessage });
  }, [negado, deniedMessage]);

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  if (error) return <div role="alert">Não foi possível validar o perfil. <button onClick={() => void refreshUser()}>Tentar novamente</button></div>;

  if (!validado) return <p role="status">Validando acesso...</p>;

  if (negado) {
    return <Navigate to="/home" replace />;
  }
  
  return <>{children}</>;
};

export default PrivateRoute;
