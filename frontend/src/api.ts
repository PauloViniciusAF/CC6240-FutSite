import axios from 'axios';
import type {
  AuthResponse, User, Team, Championship, Match, Goal, MatchTimer,
  MatchStatistics, ChampionshipStatistics
} from './types';

const api = axios.create({
  baseURL: '/api',
});

// Interceptor to add JWT token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor to handle database errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Check for database connection errors
    if (error.response?.status === 503 || error.response?.status === 500) {
      const errorMsg = error.response?.data?.message || error.message || '';
      
      // Identify which database is down
      if (errorMsg.includes('PostgreSQL') || errorMsg.includes('postgres')) {
        error.message = 'PostgreSQL database is unavailable. Please try again later.';
      } else if (errorMsg.includes('MongoDB') || errorMsg.includes('mongo')) {
        error.message = 'MongoDB database is unavailable. Please try again later.';
      } else if (errorMsg.includes('Redis') || errorMsg.includes('redis')) {
        error.message = 'Redis cache service is unavailable. Please try again later.';
      } else if (error.code === 'ECONNREFUSED' || error.message.includes('Connection refused')) {
        error.message = 'Backend server is unavailable. Please try again later.';
      }
    }
    
    return Promise.reject(error);
  }
);

// ============ AUTH ============
export const authApi = {
  register: (data: { username: string; email: string; password: string; fullName: string; role: string }) =>
    api.post<AuthResponse>('/auth/register', data),
  login: (data: { username: string; password: string }) =>
    api.post<AuthResponse>('/auth/login', data),
  me: () => api.get<User>('/auth/me'),
  athletes: () => api.get<User[]>('/auth/athletes'),
};

// ============ TEAMS ============
export const teamApi = {
  create: (data: { name: string; sport: string; members?: { athleteId: number; jerseyNumber: number }[] }) =>
    api.post<Team>('/teams', data),
  addMember: (teamId: number, data: { athleteId: number; jerseyNumber: number }) =>
    api.post<Team>(`/teams/${teamId}/members`, data),
  removeMember: (teamId: number, athleteId: number) =>
    api.delete(`/teams/${teamId}/members/${athleteId}`),
  get: (teamId: number) => api.get<Team>(`/teams/${teamId}`),
  getAll: () => api.get<Team[]>('/teams'),
  getMyTeams: () => api.get<Team[]>('/teams/my-teams'),
  getMyMemberships: () => api.get<Team[]>('/teams/my-memberships'),
  getBySport: (sport: string) => api.get<Team[]>(`/teams/sport/${sport}`),
};

// ============ CHAMPIONSHIPS ============
export const championshipApi = {
  create: (data: {
    name: string; sports: string[]; format: string;
    homeAndAway?: boolean; winPoints?: number; drawPoints?: number; lossPoints?: number;
    twoLegged?: boolean; knockoutTeamCount?: number; teamIds?: number[];
  }) => api.post<Championship>('/championships', data),
  addTeam: (champId: number, teamId: number) =>
    api.post<Championship>(`/championships/${champId}/teams/${teamId}`),
  removeTeam: (champId: number, teamId: number) =>
    api.delete<Championship>(`/championships/${champId}/teams/${teamId}`),
  start: (champId: number) =>
    api.post<Championship>(`/championships/${champId}/start`),
  setBracket: (champId: number, data: { bracketSeeding?: Record<number, number>; randomDraw: boolean }) =>
    api.post<Championship>(`/championships/${champId}/bracket`, data),
  get: (champId: number) => api.get<Championship>(`/championships/${champId}`),
  getAll: () => api.get<Championship[]>('/championships'),
  getMy: () => api.get<Championship[]>('/championships/my-championships'),
  delete: (champId: number) => api.delete(`/championships/${champId}`),
  finish: (champId: number) => api.post<Championship>(`/championships/${champId}/finish`),
};

// ============ MATCHES ============
export const matchApi = {
  create: (champId: number, data: {
    homeTeamId: number; awayTeamId: number; durationSeconds: number;
    goalLimit?: number; scheduledAt?: string; round?: number; bracketPosition?: number;
  }) => api.post<Match>(`/matches/championship/${champId}`, data),
  start: (matchId: number) => api.post<Match>(`/matches/${matchId}/start`),
  pause: (matchId: number) => api.post<Match>(`/matches/${matchId}/pause`),
  resume: (matchId: number) => api.post<Match>(`/matches/${matchId}/resume`),
  finish: (matchId: number) => api.post<Match>(`/matches/${matchId}/finish`),
  adjustTimer: (matchId: number, deltaSeconds: number) =>
    api.post<MatchTimer>(`/matches/${matchId}/timer/adjust?deltaSeconds=${deltaSeconds}`),
  recordGoal: (matchId: number, data: {
    teamId: number; scorerId?: number; ownGoal: boolean; minute?: number; second?: number;
  }) => api.post<Goal>(`/matches/${matchId}/goals`, data),
  get: (matchId: number) => api.get<Match>(`/matches/${matchId}`),
  getByChampionship: (champId: number) => api.get<Match[]>(`/matches/championship/${champId}`),
  getByRound: (champId: number, round: number) =>
    api.get<Match[]>(`/matches/championship/${champId}/round/${round}`),
};

// ============ STATISTICS (MongoDB) ============
export const statsApi = {
  getMatch: (matchId: number) => api.get<MatchStatistics>(`/statistics/match/${matchId}`),
  getChampionship: (champId: number) =>
    api.get<ChampionshipStatistics>(`/statistics/championship/${champId}`),
  getChampionshipMatches: (champId: number) =>
    api.get<MatchStatistics[]>(`/statistics/championship/${champId}/matches`),
};

// ============ DATABASE MONITORING ============
export const databaseApi = {
  login: (username: string, password: string) =>
    api.post<{ token: string; message: string }>('/database/login', { username, password }),
  getStatus: () =>
    api.get('/database/status'),
  ping: () => api.get('/database/status/ping'),
};

export default api;
