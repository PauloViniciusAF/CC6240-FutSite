import { useState, useEffect, useRef } from 'react';
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

const REFRESH_INTERVAL_MS = 5000;

export default function DatabasePage() {
  const [status, setStatus] = useState<DatabaseStatus | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    fetchStatus(true);

    const interval = setInterval(() => fetchStatus(false), REFRESH_INTERVAL_MS);
    return () => {
      isMounted.current = false;
      clearInterval(interval);
    };
  }, []);

  const fetchStatus = async (isInitial: boolean) => {
    if (isInitial) {
      setInitialLoading(true);
    } else {
      setRefreshing(true);
    }

    try {
      const res = await databaseApi.getStatus();
      if (!isMounted.current) return;
      setStatus(res.data);
      setLastUpdated(new Date());
      setError('');
    } catch (err: any) {
      if (!isMounted.current) return;
      // Only show error if we have no previous data to display
      if (!status) {
        const errorMsg =
          err.response?.data?.error ||
          err.response?.data?.message ||
          'Erro ao buscar status dos bancos de dados';
        setError(errorMsg);
      }
      console.error('Error fetching status:', err);
    } finally {
      if (!isMounted.current) return;
      if (isInitial) setInitialLoading(false);
      setRefreshing(false);
    }
  };

  const handleManualRefresh = () => {
    fetchStatus(false);
  };

  return (
    <>
      <div className="main-content">
        <div className="db-container">
          <div className="db-header">
            <div className="db-title-row">
              <h1>Database Status</h1>
              <button
                className="db-refresh-btn"
                onClick={handleManualRefresh}
                disabled={refreshing || initialLoading}
                title="Atualizar agora"
              >
                <span className={refreshing ? 'db-spin' : ''}>↻</span>
              </button>
            </div>
            <div className="db-meta-row">
              {refreshing && <span className="db-refreshing-indicator">Atualizando...</span>}
              <div className="db-last-update">
                Última atualização:{' '}
                {lastUpdated ? lastUpdated.toLocaleTimeString('pt-BR') : '—'}
              </div>
            </div>
          </div>

          {initialLoading && (
            <div className="db-loading">Carregando...</div>
          )}

          {error && !status && (
            <div className="db-error">{error}</div>
          )}

          {status && (
            <div className={`db-grid ${refreshing ? 'db-grid-refreshing' : ''}`}>
              <DbCard
                label="PostgreSQL"
                variant="postgres"
                info={status.postgres}
              />
              <DbCard
                label="MongoDB"
                variant="mongo"
                info={status.mongo}
              />
              <DbCard
                label="Redis"
                variant="redis"
                info={status.redis}
              />
            </div>
          )}
        </div>
      </div>
    </>
  );
}

interface DbCardProps {
  label: string;
  variant: 'postgres' | 'mongo' | 'redis';
  info: DatabaseInfo;
}

function DbCard({ label, variant, info }: DbCardProps) {
  const isOnline = info.status === 'ACTIVE';

  return (
    <div className={`db-card db-card-${variant} db-card-${info.status.toLowerCase()}`}>
      <div className="db-card-header">
        <h2>{label}</h2>
        <span className={`db-status-badge db-status-${info.status.toLowerCase()}`}>
          {isOnline ? 'Online' : 'Offline'}
        </span>
      </div>
    </div>
  );
}