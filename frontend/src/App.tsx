import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import TeamsPage from './pages/TeamsPage';
import CreateTeamPage from './pages/CreateTeamPage';
import TeamDetailPage from './pages/TeamDetailPage';
import ChampionshipsPage from './pages/ChampionshipsPage';
import CreateChampionshipPage from './pages/CreateChampionshipPage';
import ChampionshipDetailPage from './pages/ChampionshipDetailPage';
import MatchControlPage from './pages/MatchControlPage';
import StatisticsPage from './pages/StatisticsPage';
import DatabasePage from './pages/DatabasePage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="main-content">Carregando...</div>;
  if (!user) return <Navigate to="/login" />;
  return <>{children}</>;
}

function ManagerOnlyRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="main-content">Carregando...</div>;
  if (!user) return <Navigate to="/login" />;
  if (user.role !== 'MANAGER') {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '2rem' }}>
        <h1>Acesso Negado</h1>
        <p>Apenas gerenciadores podem criar campeonatos.</p>
        <button onClick={() => window.location.href = '/'} style={{ 
          padding: '0.5rem 1rem', 
          fontSize: '1rem',
          cursor: 'pointer',
          backgroundColor: '#007bff',
          color: 'white',
          border: 'none',
          borderRadius: '4px'
        }}>
          Voltar para Dashboard
        </button>
      </div>
    );
  }
  return <>{children}</>;
}

function AppRoutes() {
  const { user } = useAuth();

  return (
    <Routes>
      <Route path="/database" element={<DatabasePage />} />
      <Route path="/login" element={user ? <Navigate to="/" /> : <LoginPage />} />
      <Route path="/register" element={user ? <Navigate to="/" /> : <RegisterPage />} />
      <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      <Route path="/teams" element={<ProtectedRoute><TeamsPage /></ProtectedRoute>} />
      <Route path="/teams/create" element={<ProtectedRoute><CreateTeamPage /></ProtectedRoute>} />
      <Route path="/teams/:id" element={<ProtectedRoute><TeamDetailPage /></ProtectedRoute>} />
      <Route path="/championships" element={<ProtectedRoute><ChampionshipsPage /></ProtectedRoute>} />
      <Route path="/championships/create" element={<ManagerOnlyRoute><CreateChampionshipPage /></ManagerOnlyRoute>} />
      <Route path="/championships/:id" element={<ProtectedRoute><ChampionshipDetailPage /></ProtectedRoute>} />
      <Route path="/matches/:id/control" element={<ProtectedRoute><MatchControlPage /></ProtectedRoute>} />
      <Route path="/statistics/:champId" element={<ProtectedRoute><StatisticsPage /></ProtectedRoute>} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="app-container">
          <AppRoutes />
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}
