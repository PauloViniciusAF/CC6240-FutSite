import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { authApi } from '../api';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await authApi.login({ username, password });
      login(res.data.token, res.data.user);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao fazer login');
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>⚽ DaChamp</h1>
        <p>Gerenciamento de Campeonatos Esportivos</p>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Usuário</label>
            <input className="form-control" type="text" value={username}
              onChange={e => setUsername(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input className="form-control" type="password" value={password}
              onChange={e => setPassword(e.target.value)} required />
          </div>
          <button className="btn btn-primary btn-lg" style={{ width: '100%' }} type="submit">
            Entrar
          </button>
        </form>
        <div className="auth-footer">
          Não tem conta? <Link to="/register">Cadastre-se</Link>
        </div>
      </div>
    </div>
  );
}
