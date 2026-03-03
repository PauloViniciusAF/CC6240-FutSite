import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <nav className="navbar">
      <Link to="/" className="navbar-brand">
        ⚽ FutSite
      </Link>
      <div className="navbar-links">
        <Link to="/">Dashboard</Link>
        <Link to="/teams">Times</Link>
        <Link to="/championships">Campeonatos</Link>
      </div>
      <div className="navbar-user">
        <span>{user.fullName} ({user.role === 'MANAGER' ? 'Gerenciador' : 'Atleta'})</span>
        <button className="btn-logout" onClick={logout}>Sair</button>
      </div>
    </nav>
  );
}
