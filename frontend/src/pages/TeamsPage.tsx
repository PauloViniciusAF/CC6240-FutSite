import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { teamApi } from '../api';
import type { Team } from '../types';

export default function TeamsPage() {
  const [teams, setTeams] = useState<Team[]>([]);

  useEffect(() => {
    teamApi.getAll().then(r => setTeams(r.data)).catch(() => {});
  }, []);

  return (
    <>
      <div className="main-content">
        <div className="page-header">
          <h1>Times</h1>
          <Link to="/teams/create" className="btn btn-primary">+ Criar Time</Link>
        </div>
        {teams.length === 0 ? (
          <div className="empty-state">
            <h3>Nenhum time cadastrado</h3>
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
  );
}
