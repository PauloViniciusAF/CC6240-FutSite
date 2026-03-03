import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { statsApi } from '../api';
import type { ChampionshipStatistics, MatchStatistics } from '../types';

export default function StatisticsPage() {
  const { champId } = useParams<{ champId: string }>();
  const [champStats, setChampStats] = useState<ChampionshipStatistics | null>(null);
  const [matchStats, setMatchStats] = useState<MatchStatistics[]>([]);
  const [selectedMatch, setSelectedMatch] = useState<MatchStatistics | null>(null);

  useEffect(() => {
    statsApi.getChampionship(Number(champId)).then(r => setChampStats(r.data)).catch(() => {});
    statsApi.getChampionshipMatches(Number(champId)).then(r => setMatchStats(r.data)).catch(() => {});
  }, [champId]);

  return (
    <>
      <Navbar />
      <div className="main-content">
        <div className="page-header">
          <h1>📊 Estatísticas{champStats ? ` — ${champStats.championshipName}` : ''}</h1>
          <Link to={`/championships/${champId}`} className="btn">← Voltar</Link>
        </div>

        {/* Standings */}
        {champStats && champStats.standings.length > 0 && (
          <div className="card">
            <span className="card-title">Classificação</span>
            <div className="table-container" style={{ marginTop: '1rem' }}>
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
                    <tr key={s.teamId}>
                      <td className="standings-pos">{i + 1}</td>
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
          </div>
        )}

        {/* Top Scorers */}
        {champStats && champStats.topScorers.length > 0 && (
          <div className="card">
            <span className="card-title">Artilharia</span>
            <div className="table-container" style={{ marginTop: '1rem' }}>
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

        {/* Match Statistics */}
        <div className="card">
          <span className="card-title">Estatísticas das Partidas ({matchStats.length})</span>
          {matchStats.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
              Nenhuma estatística de partida disponível ainda.
            </p>
          ) : (
            <div className="grid grid-2" style={{ marginTop: '1rem' }}>
              {matchStats.map(ms => (
                <div key={ms.id} className="card" style={{ cursor: 'pointer' }}
                  onClick={() => setSelectedMatch(selectedMatch?.id === ms.id ? null : ms)}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{ms.homeTeamName}</strong>
                    <span style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                      {ms.homeScore} x {ms.awayScore}
                    </span>
                    <strong>{ms.awayTeamName}</strong>
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                    {new Date(ms.playedAt).toLocaleString('pt-BR')}
                  </div>

                  {selectedMatch?.id === ms.id && (
                    <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border)', paddingTop: '0.75rem' }}>
                      <strong>Gols:</strong>
                      <ul className="goal-list">
                        {ms.goals.sort((a, b) => a.minute - b.minute).map((g, i) => (
                          <li className="goal-item" key={i}>
                            <span className="goal-minute">{g.minute}'</span>
                            <span>
                              {g.ownGoal ? '🔴 Gol Contra' : `⚽ ${g.scorerName}`}
                              {' — '}<em>{g.teamName}</em>
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {!champStats && matchStats.length === 0 && (
          <div className="empty-state">
            <h3>Sem estatísticas disponíveis</h3>
            <p>As estatísticas serão geradas automaticamente após a finalização das partidas.</p>
          </div>
        )}
      </div>
    </>
  );
}
