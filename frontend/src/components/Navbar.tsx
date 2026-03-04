import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { useState, useEffect } from 'react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const [dark, setDark] = useState(() => localStorage.getItem('theme') === 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }, [dark]);

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
        <button className="theme-toggle" onClick={() => setDark(d => !d)} title={dark ? 'Modo claro' : 'Modo escuro'}>
          {dark ? '☀️' : '🌙'}
        </button>
        <span>{user.fullName} ({user.role === 'MANAGER' ? 'Gerenciador' : 'Atleta'})</span>
        <button className="btn-logout" onClick={logout}>Sair</button>
      </div>
    </nav>
  );
}
