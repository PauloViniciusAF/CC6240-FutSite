import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { championshipApi, teamApi, matchApi, statsApi } from '../api';
import { useAuth } from '../AuthContext';
import type { Championship, Team, Match as MatchType, ChampionshipStatistics } from '../types';

export default function ChampionshipDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [champ, setChamp] = useState<Championship | null>(null);
  const [allTeams, setAllTeams] = useState<Team[]>([]);
  const [matches, setMatches] = useState<MatchType[]>([]);
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [error, setError] = useState('');
  const [champStats, setChampStats] = useState<ChampionshipStatistics | null>(null);

  // Match creation
  const [showCreateMatch, setShowCreateMatch] = useState(false);
  const [matchForm, setMatchForm] = useState({ homeTeamId: '', awayTeamId: '', durationSeconds: '2700', goalLimit: '0', round: '1' });

  const loadChamp = () => {
    championshipApi.get(Number(id)).then(r => {
      setChamp(r.data);
      if (r.data.status === 'FINISHED' || r.data.status === 'STARTED') {
        statsApi.getChampionship(Number(id)).then(s => setChampStats(s.data)).catch(() => {});
      }
    }).catch(() => {});
    matchApi.getByChampionship(Number(id)).then(r => setMatches(r.data)).catch(() => {});
  };

  useEffect(() => {
    loadChamp();
    teamApi.getAll().then(r => setAllTeams(r.data)).catch(() => {});
  }, [id]);

  const isManager = champ && user && champ.manager.id === user.id;
  const isDraft = champ?.status === 'DRAFT';
  const isStarted = champ?.status === 'STARTED';
  const isFinished = champ?.status === 'FINISHED';

  const handleAddTeam = async () => {
    if (!selectedTeamId) return;
    setError('');
    try {
      await championshipApi.addTeam(Number(id), Number(selectedTeamId));
      setSelectedTeamId('');
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleRemoveTeam = async (teamId: number) => {
    try {
      await championshipApi.removeTeam(Number(id), teamId);
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleStart = async () => {
    setError('');
    try {
      await championshipApi.start(Number(id));
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleRandomBracket = async () => {
    setError('');
    try {
      await championshipApi.setBracket(Number(id), { randomDraw: true });
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleCreateMatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await matchApi.create(Number(id), {
        homeTeamId: Number(matchForm.homeTeamId),
        awayTeamId: Number(matchForm.awayTeamId),
        durationSeconds: Number(matchForm.durationSeconds),
        goalLimit: Number(matchForm.goalLimit),
        round: Number(matchForm.round),
      });
      setShowCreateMatch(false);
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleDelete = async () => {
    if (!confirm('Tem certeza que deseja excluir este campeonato?')) return;
    try {
      await championshipApi.delete(Number(id));
      navigate('/championships');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const handleFinish = async () => {
    if (!confirm('Tem certeza que deseja encerrar este campeonato? Esta ação não pode ser desfeita.')) return;
    setError('');
    try {
      await championshipApi.finish(Number(id));
      loadChamp();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro');
    }
  };

  const statusBadge = (s: string) => {
    const labels: Record<string, string> = { SCHEDULED: 'Agendado', LIVE: 'Ao Vivo', PAUSED: 'Pausado', FINISHED: 'Finalizado' };
    return <span className={`badge badge-${s.toLowerCase()}`}>{labels[s] || s}</span>;
  };

  if (!champ) return <><Navbar /><div className="main-content">Carregando...</div></>;

  const champTeamIds = new Set(champ.teams.map(t => t.id));
  const availableTeams = allTeams.filter(t => !champTeamIds.has(t.id));

  return (
    <>
      <Navbar />
      <div className="main-content">
        <div className="page-header">
          <div>
            <h1>{champ.name}</h1>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              {champ.format === 'ROUND_ROBIN' ? 'Pontos Corridos' : 'Mata-Mata'}
              {champ.format === 'ROUND_ROBIN' && (champ.homeAndAway ? ' (Turno e Returno)' : ' (Turno Único)')}
              {champ.format === 'KNOCKOUT' && (champ.twoLegged ? ' (Ida e Volta)' : ' (Jogo Único)')}
              {' • '}{champ.sports.join(', ')}
              {' • '}<span className={`badge badge-${champ.status.toLowerCase()}`}>
                {champ.status === 'DRAFT' ? 'Rascunho' : champ.status === 'STARTED' ? 'Em Andamento' : 'Finalizado'}
              </span>
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Link to={`/statistics/${champ.id}`} className="btn">📊 Estatísticas</Link>
            {isManager && isDraft && (
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>Excluir</button>
            )}
            {isManager && isStarted && (
              <button className="btn btn-danger" onClick={handleFinish}>🏁 Encerrar Campeonato</button>
            )}
          </div>
        </div>

        {error && <div className="error-message">{error}</div>}

        {/* Teams */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Times ({champ.teams.length})</span>
            {isManager && isDraft && champ.format === 'KNOCKOUT' && champ.teams.length >= 2 && (
              <button className="btn btn-warning" onClick={handleRandomBracket}>🎲 Sortear Chaveamento</button>
            )}
          </div>
          {champ.teams.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)' }}>Nenhum time adicionado ainda.</p>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr><th>Time</th><th>Esporte</th><th>Capitão</th><th>Jogadores</th>{isManager && isDraft && <th>Ações</th>}</tr>
                </thead>
                <tbody>
                  {champ.teams.map(t => (
                    <tr key={t.id}>
                      <td><Link to={`/teams/${t.id}`}>{t.name}</Link></td>
                      <td>{t.sport}</td>
                      <td>{t.captain.fullName}</td>
                      <td>{t.members.length}</td>
                      {isManager && isDraft && (
                        <td>
                          <button className="btn btn-danger btn-sm" onClick={() => handleRemoveTeam(t.id)}>Remover</button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {isManager && isDraft && (
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', alignItems: 'center' }}>
              <select className="form-control" style={{ flex: 1 }} value={selectedTeamId}
                onChange={e => setSelectedTeamId(e.target.value)}>
                <option value="">Selecione um time...</option>
                {availableTeams.map(t => (
                  <option key={t.id} value={t.id}>{t.name} ({t.sport})</option>
                ))}
              </select>
              <button className="btn btn-primary" onClick={handleAddTeam} disabled={!selectedTeamId}>
                Adicionar Time
              </button>
            </div>
          )}
        </div>

        {/* Start Championship */}
        {isManager && isDraft && champ.teams.length >= 2 && (
          <div className="card" style={{ textAlign: 'center' }}>
            <p style={{ marginBottom: '1rem' }}>O campeonato possui {champ.teams.length} time(s). Pronto para iniciar?</p>
            <button className="btn btn-primary btn-lg" onClick={handleStart}>
              🏆 Iniciar Campeonato
            </button>
          </div>
        )}

        {/* Ranking - shown when championship is finished */}
        {isFinished && champStats && champStats.standings && champStats.standings.length > 0 && (
          <div className="card" style={{ border: '2px solid var(--primary)', background: 'var(--bg-card)' }}>
            <div className="card-header">
              <span className="card-title">🏆 Ranking Final</span>
            </div>
            <div className="table-container">
              <table className="standings-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th style={{ textAlign: 'left' }}>Time</th>
                    <th>J</th>
                    <th>V</th>
                    <th>E</th>
                    <th>D</th>
                    <th>GP</th>
                    <th>GC</th>
                    <th>SG</th>
                    <th>PTS</th>
                  </tr>
                </thead>
                <tbody>
                  {champStats.standings.map((s, i) => (
                    <tr key={s.teamId} style={i === 0 ? { background: 'var(--primary)', color: 'white', fontWeight: 700 } : {}}>
                      <td className={i === 0 ? '' : 'standings-pos'}>{i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : i + 1}</td>
                      <td style={{ textAlign: 'left', fontWeight: 500 }}>{s.teamName}</td>
                      <td>{s.played}</td>
                      <td>{s.wins}</td>
                      <td>{s.draws}</td>
                      <td>{s.losses}</td>
                      <td>{s.goalsFor}</td>
                      <td>{s.goalsAgainst}</td>
                      <td>{s.goalDifference}</td>
                      <td><strong>{s.points}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {champStats.topScorers && champStats.topScorers.length > 0 && (
              <div style={{ marginTop: '1.5rem' }}>
                <span className="card-title">⚽ Artilharia</span>
                <div className="table-container" style={{ marginTop: '0.5rem' }}>
                  <table>
                    <thead>
                      <tr><th>#</th><th>Jogador</th><th>Time</th><th>Gols</th></tr>
                    </thead>
                    <tbody>
                      {champStats.topScorers.map((s, i) => (
                        <tr key={s.playerId}>
                          <td><strong>{i + 1}</strong></td>
                          <td>{s.playerName}</td>
                          <td>{s.teamName}</td>
                          <td><strong>{s.goals}</strong></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Matches */}
        {(isStarted || champ.status === 'FINISHED') && (
          <div className="card">
            <div className="card-header">
              <span className="card-title">Partidas ({matches.length})</span>
              {isManager && isStarted && (
                <button className="btn btn-primary" onClick={() => setShowCreateMatch(!showCreateMatch)}>
                  + Nova Partida
                </button>
              )}
            </div>

            {showCreateMatch && (
              <form onSubmit={handleCreateMatch} className="card" style={{ background: 'var(--bg-card-alt)', marginBottom: '1rem' }}>
                <div className="grid grid-2">
                  <div className="form-group">
                    <label>Time Casa</label>
                    <select className="form-control" value={matchForm.homeTeamId}
                      onChange={e => setMatchForm(prev => ({ ...prev, homeTeamId: e.target.value }))} required>
                      <option value="">Selecione...</option>
                      {champ.teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Time Visitante</label>
                    <select className="form-control" value={matchForm.awayTeamId}
                      onChange={e => setMatchForm(prev => ({ ...prev, awayTeamId: e.target.value }))} required>
                      <option value="">Selecione...</option>
                      {champ.teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Duração (segundos)</label>
                    <input className="form-control" type="number" value={matchForm.durationSeconds}
                      onChange={e => setMatchForm(prev => ({ ...prev, durationSeconds: e.target.value }))} required />
                  </div>
                  <div className="form-group">
                    <label>Limite de Gols (0 = sem limite)</label>
                    <input className="form-control" type="number" value={matchForm.goalLimit}
                      onChange={e => setMatchForm(prev => ({ ...prev, goalLimit: e.target.value }))} />
                  </div>
                </div>
                <button className="btn btn-primary" type="submit">Criar Partida</button>
              </form>
            )}

            {matches.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>Nenhuma partida criada ainda.</p>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr><th>Rodada</th><th>Casa</th><th>Placar</th><th>Visitante</th><th>Status</th><th>Ações</th></tr>
                  </thead>
                  <tbody>
                    {matches.map(m => (
                      <tr key={m.id}>
                        <td>{m.round}</td>
                        <td>{m.homeTeam.name}</td>
                        <td><strong>{m.homeScore} x {m.awayScore}</strong></td>
                        <td>{m.awayTeam.name}</td>
                        <td>{statusBadge(m.status)}</td>
                        <td>
                          {isManager && (m.status === 'SCHEDULED' || m.status === 'LIVE' || m.status === 'PAUSED') && (
                            <Link to={`/matches/${m.id}/control`} className="btn btn-sm btn-primary">
                              Controlar
                            </Link>
                          )}
                          {m.status === 'FINISHED' && (
                            <Link to={`/statistics/${champ.id}`} className="btn btn-sm">
                              Stats
                            </Link>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );
}
