import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth";
import { AppLayout, RequireAuth } from "./AppLayout";
import { LoginPage } from "./pages/LoginPage";
import { SearchPage } from "./pages/SearchPage";
import { ItemPage } from "./pages/ItemPage";
import { ClaimWizardPage } from "./pages/ClaimWizardPage";
import { MyClaimsPage } from "./pages/MyClaimsPage";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/items/:itemId" element={<ItemPage />} />
            <Route
              path="/items/:itemId/claim"
              element={
                <RequireAuth>
                  <ClaimWizardPage />
                </RequireAuth>
              }
            />
            <Route
              path="/claims"
              element={
                <RequireAuth>
                  <MyClaimsPage />
                </RequireAuth>
              }
            />
            <Route path="/" element={<SearchPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>
);
