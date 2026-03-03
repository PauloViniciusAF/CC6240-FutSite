import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { championshipApi } from '../api';
import type { ChampionshipFormat } from '../types';

export default function CreateChampionshipPage() {
  const [name, setName] = useState('');
  const [sports, setSports] = useState('');
  const [format, setFormat] = useState<ChampionshipFormat>('ROUND_ROBIN');
  const [homeAndAway, setHomeAndAway] = useState(false);
  const [winPoints, setWinPoints] = useState(3);
  const [drawPoints, setDrawPoints] = useState(1);
  const [lossPoints, setLossPoints] = useState(0);
  const [twoLegged, setTwoLegged] = useState(false);
  const [knockoutTeamCount, setKnockoutTeamCount] = useState(8);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const data: any = {
        name,
        sports: sports.split(',').map(s => s.trim()).filter(Boolean),
        format,
      };
      if (format === 'ROUND_ROBIN') {
        data.homeAndAway = homeAndAway;
        data.winPoints = winPoints;
        data.drawPoints = drawPoints;
        data.lossPoints = lossPoints;
      } else {
        data.twoLegged = twoLegged;
        data.knockoutTeamCount = knockoutTeamCount;
      }
      const res = await championshipApi.create(data);
      navigate(`/championships/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao criar campeonato');
    }
  };

  return (
    <>
      <Navbar />
      <div className="main-content" style={{ maxWidth: '600px' }}>
        <h1 style={{ marginBottom: '1.5rem' }}>Criar Campeonato</h1>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit} className="card">
          <div className="form-group">
            <label>Nome do Campeonato</label>
            <input className="form-control" value={name}
              onChange={e => setName(e.target.value)} required placeholder="Ex: Interclasse 2026" />
          </div>
          <div className="form-group">
            <label>Esportes (separados por vírgula)</label>
            <input className="form-control" value={sports}
              onChange={e => setSports(e.target.value)} required placeholder="Ex: Futebol, Vôlei" />
          </div>
          <div className="form-group">
            <label>Formato</label>
            <select className="form-control" value={format}
              onChange={e => setFormat(e.target.value as ChampionshipFormat)}>
              <option value="ROUND_ROBIN">Pontos Corridos</option>
              <option value="KNOCKOUT">Mata-Mata</option>
            </select>
          </div>

          {format === 'ROUND_ROBIN' && (
            <>
              <div className="form-group">
                <label>
                  <input type="checkbox" checked={homeAndAway}
                    onChange={e => setHomeAndAway(e.target.checked)} />{' '}
                  Turno e Returno
                </label>
              </div>
              <div className="grid grid-3">
                <div className="form-group">
                  <label>Pts Vitória</label>
                  <input className="form-control" type="number" value={winPoints}
                    onChange={e => setWinPoints(Number(e.target.value))} />
                </div>
                <div className="form-group">
                  <label>Pts Empate</label>
                  <input className="form-control" type="number" value={drawPoints}
                    onChange={e => setDrawPoints(Number(e.target.value))} />
                </div>
                <div className="form-group">
                  <label>Pts Derrota</label>
                  <input className="form-control" type="number" value={lossPoints}
                    onChange={e => setLossPoints(Number(e.target.value))} />
                </div>
              </div>
            </>
          )}

          {format === 'KNOCKOUT' && (
            <>
              <div className="form-group">
                <label>
                  <input type="checkbox" checked={twoLegged}
                    onChange={e => setTwoLegged(e.target.checked)} />{' '}
                  Ida e Volta (2 jogos)
                </label>
              </div>
              <div className="form-group">
                <label>Quantidade de Times</label>
                <select className="form-control" value={knockoutTeamCount}
                  onChange={e => setKnockoutTeamCount(Number(e.target.value))}>
                  <option value={2}>2 (Final)</option>
                  <option value={4}>4 (Semifinal)</option>
                  <option value={8}>8 (Quartas de Final)</option>
                  <option value={16}>16 (Oitavas de Final)</option>
                </select>
              </div>
            </>
          )}

          <button className="btn btn-primary btn-lg" type="submit" style={{ marginTop: '0.5rem' }}>
            Criar Campeonato
          </button>
        </form>
      </div>
    </>
  );
}
