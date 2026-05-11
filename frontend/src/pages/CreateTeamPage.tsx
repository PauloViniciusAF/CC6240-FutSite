import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { teamApi } from '../api';

export default function CreateTeamPage() {
  const [name, setName] = useState('');
  const [sport, setSport] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await teamApi.create({ name, sport });
      navigate(`/teams/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao criar time');
    }
  };

  return (
    <>
      <div className="main-content" style={{ maxWidth: '600px' }}>
        <h1 style={{ marginBottom: '1.5rem' }}>Criar Novo Time</h1>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit} className="card">
          <div className="form-group">
            <label>Nome do Time</label>
            <input className="form-control" value={name}
              onChange={e => setName(e.target.value)} required placeholder="Ex: Estrelas FC" />
          </div>
          <div className="form-group">
            <label>Esporte</label>
            <input className="form-control" value={sport}
              onChange={e => setSport(e.target.value)} required placeholder="Ex: Futebol" />
          </div>
          <button className="btn btn-primary btn-lg" type="submit">Criar Time</button>
        </form>
      </div>
    </>
  );
}
