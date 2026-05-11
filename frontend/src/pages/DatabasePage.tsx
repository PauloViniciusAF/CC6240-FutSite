import { useState, useEffect } from 'react';
import { databaseApi } from '../api';
import './DatabasePage.css';

interface DatabaseInfo {
  name: string;
  status: string;
  responseTimeMs: number;
  uptime: string;
}

interface DatabaseStatus {
  timestamp: string;
  postgres: DatabaseInfo;
  mongo: DatabaseInfo;
  redis: DatabaseInfo;
}

export default function DatabasePage() {
  const [status, setStatus] = useState<DatabaseStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Auto-refresh effect every 5 seconds
  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchStatus = async () => {
    try {
      setLoading(true);
      setError('');
      const res = await databaseApi.getStatus();
      setStatus(res.data);
    } catch (err: any) {
      const errorMsg = 
        err.response?.data?.error || 
        err.response?.data?.message || 
        'Erro ao buscar status dos bancos de dados';
      setError(errorMsg);
      console.error('Error fetching status:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !status) {
    return (
      <div className="main-content">
        <div className="db-container">
          <div className="db-header">
            <h1>Database Status</h1>
          </div>
          <div className="db-loading">Carregando...</div>
        </div>
      </div>
    );
  }

  if (error && !status) {
    return (
      <div className="main-content">
        <div className="db-container">
          <div className="db-header">
            <h1>Database Status</h1>
          </div>
          <div className="db-error">{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <div className="db-container">
        <div className="db-header">
          <h1>Database Status</h1>
          <div className="db-last-update">
            Última atualização: {status ? new Date(status.timestamp).toLocaleTimeString() : '—'}
          </div>
        </div>

        {status && (
          <div className="db-grid">
            {/* PostgreSQL */}
            <div className={`db-card db-card-postgres db-card-${status.postgres.status.toLowerCase()}`}>
              <div className="db-card-header">
                <h2>PostgreSQL</h2>
                <span className={`db-status-badge db-status-${status.postgres.status.toLowerCase()}`}>
                  {status.postgres.status === 'ACTIVE' ? '🟢 Online' : '🔴 Offline'}
                </span>
              </div>
              <div className="db-card-body">
                <div className="db-info-item">
                  <span className="db-label">Resposta:</span>
                  <span className="db-value">{status.postgres.responseTimeMs}ms</span>
                </div>
                <div className="db-info-item">
                  <span className="db-label">Uptime:</span>
                  <span className="db-value">{status.postgres.uptime}</span>
                </div>
              </div>
            </div>

            {/* MongoDB */}
            <div className={`db-card db-card-mongo db-card-${status.mongo.status.toLowerCase()}`}>
              <div className="db-card-header">
                <h2>MongoDB</h2>
                <span className={`db-status-badge db-status-${status.mongo.status.toLowerCase()}`}>
                  {status.mongo.status === 'ACTIVE' ? '🟢 Online' : '🔴 Offline'}
                </span>
              </div>
              <div className="db-card-body">
                <div className="db-info-item">
                  <span className="db-label">Resposta:</span>
                  <span className="db-value">{status.mongo.responseTimeMs}ms</span>
                </div>
                <div className="db-info-item">
                  <span className="db-label">Uptime:</span>
                  <span className="db-value">{status.mongo.uptime}</span>
                </div>
              </div>
            </div>

            {/* Redis */}
            <div className={`db-card db-card-redis db-card-${status.redis.status.toLowerCase()}`}>
              <div className="db-card-header">
                <h2>Redis</h2>
                <span className={`db-status-badge db-status-${status.redis.status.toLowerCase()}`}>
                  {status.redis.status === 'ACTIVE' ? '🟢 Online' : '🔴 Offline'}
                </span>
              </div>
              <div className="db-card-body">
                <div className="db-info-item">
                  <span className="db-label">Resposta:</span>
                  <span className="db-value">{status.redis.responseTimeMs}ms</span>
                </div>
                <div className="db-info-item">
                  <span className="db-label">Uptime:</span>
                  <span className="db-value">{status.redis.uptime}</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
