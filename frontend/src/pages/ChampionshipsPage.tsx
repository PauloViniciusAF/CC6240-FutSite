import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { championshipApi } from '../api';
import type { Championship } from '../types';

export default function ChampionshipsPage() {
  const [championships, setChampionships] = useState<Championship[]>([]);

  useEffect(() => {
    championshipApi.getAll().then(r => setChampionships(r.data)).catch(() => {});
  }, []);

  const formatLabel = (f: string) => f === 'ROUND_ROBIN' ? 'Pontos Corridos' : 'Mata-Mata';
  const statusLabel = (s: string) => {
    if (s === 'DRAFT') return 'Rascunho';
    if (s === 'STARTED') return 'Em Andamento';
    return 'Finalizado';
  };

  return (
    <>
      <Navbar />
      <div className="main-content">
        <div className="page-header">
          <h1>Campeonatos</h1>
          <Link to="/championships/create" className="btn btn-primary">+ Novo Campeonato</Link>
        </div>
        {championships.length === 0 ? (
          <div className="empty-state">
            <h3>Nenhum campeonato cadastrado</h3>
          </div>
        ) : (
          <div className="grid grid-3">
            {championships.map(c => (
              <Link to={`/championships/${c.id}`} key={c.id} className="card" style={{ textDecoration: 'none', color: 'inherit' }}>
                <strong>{c.name}</strong>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                  {formatLabel(c.format)}
                  {' • '}
                  <span className={`badge badge-${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span>
                </div>
                <div style={{ fontSize: '0.8125rem', marginTop: '0.5rem' }}>
                  {c.teams.length} time(s) • {c.sports.join(', ')}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Gerenciador: {c.manager.fullName}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
