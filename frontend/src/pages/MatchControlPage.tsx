import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { matchApi } from '../api';
import type { Match, MatchTimer, User } from '../types';

export default function MatchControlPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [match, setMatch] = useState<Match | null>(null);
  const [timer, setTimer] = useState<MatchTimer | null>(null);
  const [displayElapsed, setDisplayElapsed] = useState(0);
  const lastSyncRef = useRef<number>(Date.now());
  const syncedElapsedRef = useRef<number>(0);
  const timerStatusRef = useRef<string>('');
  const [error, setError] = useState('');

  // Goal form
  const [goalTeamId, setGoalTeamId] = useState('');
  const [goalScorerId, setGoalScorerId] = useState('');
  const [goalOwnGoal, setGoalOwnGoal] = useState(false);

  const loadMatch = useCallback(() => {
    matchApi.get(Number(id)).then(r => setMatch(r.data)).catch(() => {});
  }, [id]);

  const loadTimer = useCallback(() => {
    matchApi.getTimer(Number(id)).then(r => {
      setTimer(r.data);
      if (r.data) {
        syncedElapsedRef.current = r.data.elapsedSeconds;
        lastSyncRef.current = Date.now();
        timerStatusRef.current = r.data.status;
        setDisplayElapsed(r.data.elapsedSeconds);
      }
    }).catch(() => {});
  }, [id]);

  // Poll server every 5s for sync
  useEffect(() => {
    loadMatch();
    loadTimer();
    const interval = setInterval(() => {
      loadTimer();
      loadMatch();
    }, 5000);
    return () => clearInterval(interval);
  }, [id, loadMatch, loadTimer]);

  // Local 1-second tick for smooth display
  useEffect(() => {
    const tick = setInterval(() => {
      if (timerStatusRef.current === 'RUNNING') {
        const now = Date.now();
        const delta = Math.floor((now - lastSyncRef.current) / 1000);
        setDisplayElapsed(syncedElapsedRef.current + delta);
      }
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  const handleStart = async () => {
    try {
      const r = await matchApi.start(Number(id));
      setMatch(r.data);
      loadTimer();
    } catch (e: any) { setError(e.response?.data?.message || 'Erro'); }
  };
  const handlePause = async () => {
    try {
      const r = await matchApi.pause(Number(id));
      setMatch(r.data);
      timerStatusRef.current = 'PAUSED';
      loadTimer();
    } catch (e: any) { setError(e.response?.data?.message || 'Erro'); }
  };
  const handleResume = async () => {
    try {
      const r = await matchApi.resume(Number(id));
      setMatch(r.data);
      timerStatusRef.current = 'RUNNING';
      lastSyncRef.current = Date.now();
      loadTimer();
    } catch (e: any) { setError(e.response?.data?.message || 'Erro'); }
  };
  const handleFinish = async () => {
    if (!confirm('Finalizar esta partida?')) return;
    try {
      const r = await matchApi.finish(Number(id));
      setMatch(r.data);
      timerStatusRef.current = 'STOPPED';
      setTimer(null);
    } catch (e: any) { setError(e.response?.data?.message || 'Erro'); }
  };
  const handleAdjust = async (delta: number) => {
    try { const r = await matchApi.adjustTimer(Number(id), delta); setTimer(r.data); } catch (e: any) { setError(e.response?.data?.message || 'Erro'); }
  };

  const handleGoal = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await matchApi.recordGoal(Number(id), {
        teamId: Number(goalTeamId),
        scorerId: goalOwnGoal ? undefined : (goalScorerId ? Number(goalScorerId) : undefined),
        ownGoal: goalOwnGoal,
      });
      setGoalTeamId('');
      setGoalScorerId('');
      setGoalOwnGoal(false);
      loadMatch();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao registrar gol');
    }
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  if (!match) return <><Navbar /><div className="main-content">Carregando...</div></>;

  const allPlayers = [
    ...(match.homeTeam.members || []).map(m => ({ ...m.athlete, teamId: match.homeTeam.id, teamName: match.homeTeam.name })),
    ...(match.awayTeam.members || []).map(m => ({ ...m.athlete, teamId: match.awayTeam.id, teamName: match.awayTeam.name })),
  ];

  const isLive = match.status === 'LIVE';
  const isPaused = match.status === 'PAUSED';
  const isScheduled = match.status === 'SCHEDULED';
  const isFinished = match.status === 'FINISHED';

  return (
    <>
      <Navbar />
      <div className="main-content" style={{ maxWidth: '800px' }}>
        {error && <div className="error-message">{error}</div>}

        {/* Live Panel */}
        <div className="match-live-panel">
          <div style={{ fontSize: '0.875rem', opacity: 0.7, marginBottom: '0.5rem' }}>
            {match.championshipName} • Rodada {match.round}
          </div>
          <div className="match-teams">
            <div>
              <div className="match-team-name">{match.homeTeam.name}</div>
            </div>
            <div className="match-score">
              {match.homeScore} &ndash; {match.awayScore}
            </div>
            <div>
              <div className="match-team-name">{match.awayTeam.name}</div>
            </div>
          </div>

          {timer && (
            <div className="timer-display">
              ⏱ {formatTime(displayElapsed)} / {formatTime(timer.totalSeconds)}
            </div>
          )}

          <div style={{ marginTop: '0.5rem' }}>
            <span className={`badge badge-${match.status.toLowerCase()}`}>
              {match.status === 'SCHEDULED' ? 'Agendado' : match.status === 'LIVE' ? '🔴 AO VIVO' : match.status === 'PAUSED' ? '⏸ Pausado' : '✅ Finalizado'}
            </span>
          </div>

          {/* Controls */}
          <div className="timer-controls">
            {isScheduled && (
              <button className="btn btn-primary" onClick={handleStart}>▶ Iniciar Partida</button>
            )}
            {isLive && (
              <>
                <button className="btn btn-warning" onClick={handlePause}>⏸ Pausar</button>
                <button className="btn btn-danger" onClick={handleFinish}>⏹ Finalizar</button>
              </>
            )}
            {isPaused && (
              <>
                <button className="btn btn-primary" onClick={handleResume}>▶ Retomar</button>
                <button className="btn btn-danger" onClick={handleFinish}>⏹ Finalizar</button>
              </>
            )}
            {(isLive || isPaused) && (
              <>
                <button className="btn" onClick={() => handleAdjust(-30)}>-30s</button>
                <button className="btn" onClick={() => handleAdjust(30)}>+30s</button>
              </>
            )}
            {isFinished && (
              <button className="btn" onClick={() => navigate(`/championships/${match.championshipId}`)}>
                Voltar ao Campeonato
              </button>
            )}
          </div>
        </div>

        {/* Record Goal */}
        {(isLive || isPaused) && (
          <div className="card" style={{ marginTop: '1rem' }}>
            <span className="card-title">⚽ Registrar Gol</span>
            <form onSubmit={handleGoal} style={{ marginTop: '1rem' }}>
              <div className="grid grid-2">
                <div className="form-group">
                  <label>Time que marcou</label>
                  <select className="form-control" value={goalTeamId}
                    onChange={e => setGoalTeamId(e.target.value)} required>
                    <option value="">Selecione...</option>
                    <option value={match.homeTeam.id}>{match.homeTeam.name}</option>
                    <option value={match.awayTeam.id}>{match.awayTeam.name}</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Minuto (automático)</label>
                  <input className="form-control" type="text" readOnly
                    value={formatTime(displayElapsed)} />
                </div>
              </div>
              <div className="form-group">
                <label>
                  <input type="checkbox" checked={goalOwnGoal}
                    onChange={e => setGoalOwnGoal(e.target.checked)} />{' '}
                  Gol Contra
                </label>
              </div>
              {!goalOwnGoal && (
                <div className="form-group">
                  <label>Jogador que marcou</label>
                  <select className="form-control" value={goalScorerId}
                    onChange={e => setGoalScorerId(e.target.value)}>
                    <option value="">Selecione...</option>
                    {allPlayers
                      .filter(p => !goalTeamId || p.teamId === Number(goalTeamId))
                      .map(p => (
                        <option key={p.id} value={p.id}>{p.fullName} ({p.teamName})</option>
                      ))}
                  </select>
                </div>
              )}
              <button className="btn btn-primary" type="submit">Registrar Gol</button>
            </form>
          </div>
        )}

        {/* Goal List */}
        <div className="card" style={{ marginTop: '1rem' }}>
          <span className="card-title">Gols ({match.goals.length})</span>
          {match.goals.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Nenhum gol registrado.</p>
          ) : (
            <ul className="goal-list" style={{ marginTop: '0.5rem' }}>
              {match.goals
                .sort((a, b) => a.minute - b.minute)
                .map(g => (
                  <li className="goal-item" key={g.id}>
                    <span className="goal-minute">{g.minute}'</span>
                    <span>
                      {g.ownGoal ? '🔴 Gol Contra' : `⚽ ${g.scorerName}`}
                      {' — '}
                      <em>{g.teamName}</em>
                    </span>
                  </li>
                ))}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}
