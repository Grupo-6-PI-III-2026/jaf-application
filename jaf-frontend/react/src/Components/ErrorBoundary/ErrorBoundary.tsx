import { Component, type ErrorInfo, type ReactNode } from "react";
import { logger } from "../../Utils/logger";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  falhou: boolean;
}

export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { falhou: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { falhou: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    logger.erro(`Falha de renderização: ${info.componentStack}`, error);
  }

  render() {
    if (!this.state.falhou) return this.props.children;
    return (
      <main role="alert" style={{ padding: "32px", textAlign: "center" }}>
        <h1>Não foi possível carregar esta tela</h1>
        <p>Recarregue a página para tentar novamente.</p>
        <button type="button" onClick={() => window.location.reload()}>Recarregar</button>
      </main>
    );
  }
}
