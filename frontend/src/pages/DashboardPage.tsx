import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import Navbar from '../components/Navbar';
import { teamApi, championshipApi } from '../api';
import type { Team, Championship } from '../types';

export default function DashboardPage() {
  const { user } = useAuth();
  const [teams, setTeams] = useState<Team[]>([]);
  const [championships, setChampionships] = useState<Championship[]>([]);

  useEffect(() => {
    if (user?.role === 'ATHLETE') {
      teamApi.getMyMemberships().then(r => setTeams(r.data)).catch(() => {});
    } else if (user?.role === 'MANAGER') {
      championshipApi.getMy().then(r => setChampionships(r.data)).catch(() => {});
    }
    if (user?.role === 'ATHLETE') {
      teamApi.getMyTeams().then(r => setTeams(prev => {
        const ids = new Set(prev.map(t => t.id));
        return [...prev, ...r.data.filter(t => !ids.has(t.id))];
      })).catch(() => {});
    }
  }, [user]);

  return (
    <>
      <Navbar />
      <div className="main-content">
        <div className="page-header">
          <h1>Bem-vindo, {user?.fullName}!</h1>
        </div>

        {user?.role === 'MANAGER' && (
          <>
            <div className="card">
              <div className="card-header">
                <span className="card-title">Meus Campeonatos</span>
                <Link to="/championships/create" className="btn btn-primary">+ Novo Campeonato</Link>
              </div>
              {championships.length === 0 ? (
                <div className="empty-state">
                  <h3>Nenhum campeonato ainda</h3>
                  <p>Crie seu primeiro campeonato para começar!</p>
                </div>
              ) : (
                <div className="grid grid-3">
                  {championships.map(c => (
                    <Link to={`/championships/${c.id}`} key={c.id} className="card" style={{ textDecoration: 'none', color: 'inherit' }}>
                      <strong>{c.name}</strong>
                      <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                        {c.format === 'ROUND_ROBIN' ? 'Pontos Corridos' : 'Mata-Mata'}
                        {' • '}
                        <span className={`badge badge-${c.status.toLowerCase()}`}>{c.status}</span>
                      </div>
                      <div style={{ fontSize: '0.8125rem', marginTop: '0.5rem' }}>
                        {c.teams.length} time(s) • {c.sports.join(', ')}
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </>
        )}

        {user?.role === 'ATHLETE' && (
          <>
            <div className="card">
              <div className="card-header">
                <span className="card-title">Meus Times</span>
                <Link to="/teams/create" className="btn btn-primary">+ Criar Time</Link>
              </div>
              {teams.length === 0 ? (
                <div className="empty-state">
                  <h3>Nenhum time ainda</h3>
                  <p>Crie seu primeiro time ou aguarde ser adicionado a um!</p>
                </div>
              ) : (
                <div className="grid grid-3">
                  {teams.map(t => (
                    <Link to={`/teams/${t.id}`} key={t.id} className="card" style={{ textDecoration: 'none', color: 'inherit' }}>
                      <strong>{t.name}</strong>
                      <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                        {t.sport} • {t.members.length} jogador(es)
                      </div>
                      <div style={{ fontSize: '0.8125rem', marginTop: '0.25rem' }}>
                        Capitão: {t.captain.fullName}
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </>
  );
}
