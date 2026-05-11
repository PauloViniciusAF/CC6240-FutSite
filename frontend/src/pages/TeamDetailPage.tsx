import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { teamApi, authApi } from '../api';
import { useAuth } from '../AuthContext';
import type { Team, User } from '../types';

export default function TeamDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [team, setTeam] = useState<Team | null>(null);
  const [athletes, setAthletes] = useState<User[]>([]);
  const [selectedAthlete, setSelectedAthlete] = useState('');
  const [jerseyNumber, setJerseyNumber] = useState('');
  const [error, setError] = useState('');

  const loadTeam = () => {
    teamApi.get(Number(id)).then(r => setTeam(r.data)).catch(() => {});
  };

  useEffect(() => {
    loadTeam();
    authApi.athletes().then(r => setAthletes(r.data)).catch(() => {});
  }, [id]);

  const isCaptain = team && user && team.captain.id === user.id;

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await teamApi.addMember(Number(id), {
        athleteId: Number(selectedAthlete),
        jerseyNumber: Number(jerseyNumber),
      });
      setSelectedAthlete('');
      setJerseyNumber('');
      loadTeam();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao adicionar membro');
    }
  };

  const handleRemoveMember = async (athleteId: number) => {
    try {
      await teamApi.removeMember(Number(id), athleteId);
      loadTeam();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao remover membro');
    }
  };

  if (!team) return <><div className="main-content">Carregando...</div></>;

  const memberIds = new Set(team.members.map(m => m.athlete.id));
  const availableAthletes = athletes.filter(a => !memberIds.has(a.id));

  return (
    <>
      <div className="main-content">
        <div className="page-header">
          <div>
            <h1>{team.name}</h1>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              {team.sport} • Capitão: {team.captain.fullName}
            </div>
          </div>
        </div>

        {error && <div className="error-message">{error}</div>}

        <div className="card">
          <div className="card-header">
            <span className="card-title">Jogadores ({team.members.length})</span>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Camisa</th>
                  <th>Nome</th>
                  <th>Email</th>
                  {isCaptain && <th>Ações</th>}
                </tr>
              </thead>
              <tbody>
                {team.members.map(m => (
                  <tr key={m.id}>
                    <td><strong>#{m.jerseyNumber}</strong></td>
                    <td>
                      {m.athlete.fullName}
                      {m.athlete.id === team.captain.id && ' (C)'}
                    </td>
                    <td>{m.athlete.email}</td>
                    {isCaptain && (
                      <td>
                        {m.athlete.id !== team.captain.id && (
                          <button className="btn btn-danger btn-sm"
                            onClick={() => handleRemoveMember(m.athlete.id)}>
                            Remover
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {isCaptain && (
          <div className="card">
            <span className="card-title">Adicionar Jogador</span>
            <form onSubmit={handleAddMember} style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ flex: 2, marginBottom: 0 }}>
                <label>Atleta</label>
                <select className="form-control" value={selectedAthlete}
                  onChange={e => setSelectedAthlete(e.target.value)} required>
                  <option value="">Selecione...</option>
                  {availableAthletes.map(a => (
                    <option key={a.id} value={a.id}>{a.fullName} ({a.username})</option>
                  ))}
                </select>
              </div>
              <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                <label>Nº Camisa</label>
                <input className="form-control" type="number" min="0" value={jerseyNumber}
                  onChange={e => setJerseyNumber(e.target.value)} required />
              </div>
              <button className="btn btn-primary" type="submit">Adicionar</button>
            </form>
          </div>
        )}
      </div>
    </>
  );
}
