import { AppRoutes } from "./routes";
import { Toaster } from "sonner";

function App() {
  return (
    <div className="app-container">
      <AppRoutes />
      <Toaster position="top-right" richColors />
    </div>
  );
}

export default App;
