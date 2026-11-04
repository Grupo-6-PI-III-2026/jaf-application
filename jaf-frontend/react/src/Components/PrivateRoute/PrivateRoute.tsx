import React, { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { toast } from 'sonner';
import { useUser } from '../../Context/useUser';
import { useAuthz } from '../../Hooks/useAuthz';

interface PrivateRouteProps {
  children: React.ReactNode;
  requiredPermission?: string;
  adminOnly?: boolean;
  deniedMessage?: string;
  mostrarSemAcesso?: boolean;
}


const PrivateRoute: React.FC<PrivateRouteProps> = ({ children, requiredPermission, adminOnly, deniedMessage, mostrarSemAcesso }) => {
  const { user, isLoading, error, refreshUser } = useUser();
  const { can, isAdmin } = useAuthz();
  const location = useLocation();
  const validado = !error && !isLoading && Boolean(user);
  const negado = validado && Boolean((adminOnly && !isAdmin) ||
    (requiredPermission && !can(requiredPermission)));

  useEffect(() => {
    if (negado && deniedMessage) toast.error(deniedMessage, { id: deniedMessage });
  }, [negado, deniedMessage]);

  if (isLoading) return <p role="status">Validando acesso...</p>;
  if (error) return <div role="alert">Não foi possível validar o perfil. <button onClick={() => void refreshUser()}>Tentar novamente</button></div>;

  // Sem sessão validada: volta ao login e lembra de onde veio para retornar depois.
  if (!user) return <Navigate to="/" replace state={{ de: location }} />;

  if (negado && mostrarSemAcesso) {
    return <div role="alert"><h2>Sem acesso</h2><p>Você não tem permissão para ver esta página. Fale com o administrador.</p></div>;
  }

  if (negado) {
    return <Navigate to="/home" replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
