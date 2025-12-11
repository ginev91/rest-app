import React from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import Login from "./pages/Login";
import Menu from "./pages/Menu";
import Orders from "./pages/Orders";
import Dashboard from "./pages/Dashboard";
import Reservations from "./pages/Reservations";
import Profile from "./pages/Profile";
import Tables from "./pages/Tables";
import Recommendations from "./pages/Recommendations";
import NotFound from "./pages/NotFound";
import TableDetails from "./pages/TableDetails";
import { Loader2 } from "lucide-react";
import Layout from "@/components/Layout";

/**
 * Simple protected-route wrapper used across the app
 */
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const App: React.FC = () => {
  const location = useLocation();

  const isLoginRoute = location.pathname === "/login";

  const routes = (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Navigate to="/menu" replace />} />
      <Route path="/menu" element={<ProtectedRoute><Menu /></ProtectedRoute>} />
      <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
      <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
      <Route path="/tables" element={<ProtectedRoute><Tables /></ProtectedRoute>} />
      <Route path="/tables/:tableId" element={<ProtectedRoute><TableDetails /></ProtectedRoute>} />
      <Route path="/recommendations" element={<ProtectedRoute><Recommendations /></ProtectedRoute>} />
      <Route path="/reservations" element={<ProtectedRoute><Reservations /></ProtectedRoute>} />
       <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );

  return (
    <TooltipProvider>
      <Toaster />
      <Sonner />
      {isLoginRoute ? (
        routes
      ) : (
        <Layout>{routes}</Layout>
      )}
    </TooltipProvider>
  );
};

export default App;