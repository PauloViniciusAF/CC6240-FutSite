import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { authApi } from '../api';
import type { UserRole } from '../types';

export default function RegisterPage() {
  const [form, setForm] = useState({
    username: '', email: '', password: '', fullName: '', role: 'ATHLETE' as UserRole
  });
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await authApi.register(form);
      login(res.data.token, res.data.user);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erro ao cadastrar');
    }
  };

  const update = (field: string, value: string) =>
    setForm(prev => ({ ...prev, [field]: value }));

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>⚽ Cadastro</h1>
        <p>Crie sua conta no DaChamp</p>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Nome Completo</label>
            <input className="form-control" value={form.fullName}
              onChange={e => update('fullName', e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Usuário</label>
            <input className="form-control" value={form.username}
              onChange={e => update('username', e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input className="form-control" type="email" value={form.email}
              onChange={e => update('email', e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input className="form-control" type="password" value={form.password}
              onChange={e => update('password', e.target.value)} required minLength={6} />
          </div>
          <div className="form-group">
            <label>Tipo de Conta</label>
            <select className="form-control" value={form.role}
              onChange={e => update('role', e.target.value)}>
              <option value="ATHLETE">Atleta</option>
              <option value="MANAGER">Gerenciador</option>
            </select>
          </div>
          <button className="btn btn-primary btn-lg" style={{ width: '100%' }} type="submit">
            Cadastrar
          </button>
        </form>
        <div className="auth-footer">
          Já tem conta? <Link to="/login">Entrar</Link>
        </div>
      </div>
    </div>
  );
}
