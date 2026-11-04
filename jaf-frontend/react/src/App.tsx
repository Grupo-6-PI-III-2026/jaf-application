import { AppRoutes } from "./routes";
import { Toaster } from "sonner";
import { DialogProvider } from "./Components/Feedback/DialogProvider";

function App() {
  return (
    <DialogProvider>
      <div className="app-container">
        <AppRoutes />
        <Toaster position="top-right" richColors />
      </div>
    </DialogProvider>
  );
}

export default App;
