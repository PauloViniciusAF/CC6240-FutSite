import { useState, useEffect } from 'react';
import { databaseApi } from '../api';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import './DatabasePage.css';

interface DatabaseStatus {
  timestamp: string;
  postgres: {
    status: string;
    connection: string;
    database: string;
    connectionPoolSize: number;
    responseTimeMs: number;
    message: string;
  };
  mongo: {
    status: string;
    connection: string;
    database: string;
    responseTimeMs: number;
    message: string;
  };
  redis: {
    status: string;
    connection: string;
    responseTimeMs: number;
    message: string;
  };
  overallStatus: string;
}

interface ChartData {
  time: string;
  postgres: number;
  mongo: number;
  redis: number;
}

export default function DatabasePage() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [token, setToken] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loginError, setLoginError] = useState('');
  const [status, setStatus] = useState<DatabaseStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [chartData, setChartData] = useState<ChartData[]>([]);

  // Auto-refresh effect every 5 seconds
  useEffect(() => {
    if (!isLoggedIn) return;

    const interval = setInterval(() => {
      fetchStatus(token);
    }, 5000);

    return () => clearInterval(interval);
  }, [isLoggedIn, token]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    setLoading(true);

    try {
      console.log('Tentando login com username:', username);
      const res = await databaseApi.login(username, password);
      console.log('Login response:', res.data);
      const newToken = res.data.token;
      setToken(newToken);
      sessionStorage.setItem('db-token', newToken);
      setIsLoggedIn(true);
      setUsername('');
      setPassword('');
      // Fetch status immediately after login
      await fetchStatus(newToken);
    } catch (err: any) {
      console.error('Login error:', err);
      const errorMsg = 
        err.response?.data?.error || 
        err.response?.data?.message || 
        err.message || 
        'Erro ao fazer login. Verifique a conexão com o servidor.';
      setLoginError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatus = async (dbToken: string) => {
    try {
      setLoading(true);
      const res = await databaseApi.getStatus(dbToken);
      setStatus(res.data);
      
      // Update chart data
      const newDataPoint: ChartData = {
        time: new Date(res.data.timestamp).toLocaleTimeString(),
        postgres: res.data.postgres.responseTimeMs,
        mongo: res.data.mongo.responseTimeMs,
        redis: res.data.redis.responseTimeMs,
      };
      
      setChartData(prev => {
        const updated = [...prev, newDataPoint];
        // Keep only last 30 data points
        return updated.slice(-30);
      });
    } catch (err: any) {
      setLoginError('Erro ao buscar status dos bancos de dados');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setToken('');
    sessionStorage.removeItem('db-token');
    setStatus(null);
    setChartData([]);
  };

  if (!isLoggedIn) {
    return (
      <div className="db-login-container">
        <div className="db-login-card">
          <div className="db-login-header">
            <pre>╔════════════════════════════════════╗</pre>
            <pre>║   DATABASE MONITORING SYSTEM       ║</pre>
            <pre>╚════════════════════════════════════╝</pre>
          </div>
          
          {loginError && <div className="db-error-message">{loginError}</div>}
          
          <form onSubmit={handleLogin} className="db-login-form">
            <div className="db-form-group">
              <label>username@db:</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="dev"
                disabled={loading}
                required
              />
            </div>
            
            <div className="db-form-group">
              <label>password:</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                disabled={loading}
                required
              />
            </div>
            
            <button type="submit" className="db-btn db-btn-primary" disabled={loading}>
              {loading ? '⏳ AUTH...' : '▶ LOGIN'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="db-dashboard-container">
      <div className="db-dashboard-header">
        <div>
          <pre>╔════════════════════════════════════╗</pre>
          <pre>║   DATABASE MONITORING SYSTEM       ║</pre>
          <pre>╚════════════════════════════════════╝</pre>
        </div>
        <button onClick={handleLogout} className="db-btn db-btn-logout">
          ⬅ EXIT
        </button>
      </div>

      {status && (
        <div className="db-main-content">
          {/* Database Cards */}
          <div className="db-databases-grid">
            {/* PostgreSQL */}
            <div className="db-database-card">
              <div className="db-card-header">
                <span className={`db-status-dot db-status-${status.postgres.status.toLowerCase()}`}></span>
                <span className="db-card-title">PostgreSQL</span>
                <span className="db-response-time">{status.postgres.responseTimeMs}ms</span>
              </div>
              <div className="db-chart-container">
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                    <XAxis 
                      dataKey="time" 
                      stroke="#00ff00"
                      tick={{ fontSize: 11 }}
                      interval={Math.floor(chartData.length / 3)}
                    />
                    <YAxis stroke="#00ff00" tick={{ fontSize: 11 }} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1a1a1a', border: '1px solid #00ff00', color: '#00ff00' }}
                      labelStyle={{ color: '#00ff00' }}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="postgres" 
                      stroke="#00ff00" 
                      dot={false}
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="db-card-info">
                <div className="db-info-row">
                  <span>database:</span>
                  <span>{status.postgres.database}</span>
                </div>
              </div>
            </div>

            {/* MongoDB */}
            <div className="db-database-card">
              <div className="db-card-header">
                <span className={`db-status-dot db-status-${status.mongo.status.toLowerCase()}`}></span>
                <span className="db-card-title">MongoDB</span>
                <span className="db-response-time">{status.mongo.responseTimeMs}ms</span>
              </div>
              <div className="db-chart-container">
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                    <XAxis 
                      dataKey="time" 
                      stroke="#00d9ff"
                      tick={{ fontSize: 11 }}
                      interval={Math.floor(chartData.length / 3)}
                    />
                    <YAxis stroke="#00d9ff" tick={{ fontSize: 11 }} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1a1a1a', border: '1px solid #00d9ff', color: '#00d9ff' }}
                      labelStyle={{ color: '#00d9ff' }}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="mongo" 
                      stroke="#00d9ff" 
                      dot={false}
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="db-card-info">
                <div className="db-info-row">
                  <span>database:</span>
                  <span>{status.mongo.database}</span>
                </div>
              </div>
            </div>

            {/* Redis */}
            <div className="db-database-card">
              <div className="db-card-header">
                <span className={`db-status-dot db-status-${status.redis.status.toLowerCase()}`}></span>
                <span className="db-card-title">Redis</span>
                <span className="db-response-time">{status.redis.responseTimeMs}ms</span>
              </div>
              <div className="db-chart-container">
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                    <XAxis 
                      dataKey="time" 
                      stroke="#ffaa00"
                      tick={{ fontSize: 11 }}
                      interval={Math.floor(chartData.length / 3)}
                    />
                    <YAxis stroke="#ffaa00" tick={{ fontSize: 11 }} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1a1a1a', border: '1px solid #ffaa00', color: '#ffaa00' }}
                      labelStyle={{ color: '#ffaa00' }}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="redis" 
                      stroke="#ffaa00" 
                      dot={false}
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="db-card-info">
                <div className="db-info-row">
                  <span>connection:</span>
                  <span>{status.redis.connection}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Overall Status */}
          <div className="db-overall-status">
            <div className="db-status-title">
              <span className={`db-status-dot db-status-${status.overallStatus.toLowerCase()}`}></span>
              system status: {status.overallStatus}
            </div>
            <div className="db-last-update">
              last update: {new Date(status.timestamp).toLocaleTimeString()}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
