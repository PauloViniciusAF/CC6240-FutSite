export type UserRole = 'MANAGER' | 'ATHLETE';

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  type: string;
  user: User;
}

export interface TeamMember {
  id: number;
  athlete: User;
  jerseyNumber: number;
}

export interface Team {
  id: number;
  name: string;
  sport: string;
  captain: User;
  members: TeamMember[];
}

export type ChampionshipFormat = 'ROUND_ROBIN' | 'KNOCKOUT';
export type ChampionshipStatus = 'DRAFT' | 'STARTED' | 'FINISHED';
export type MatchStatus = 'SCHEDULED' | 'LIVE' | 'PAUSED' | 'FINISHED';

export interface Championship {
  id: number;
  name: string;
  manager: User;
  format: ChampionshipFormat;
  status: ChampionshipStatus;
  sports: string[];
  teams: Team[];
  homeAndAway?: boolean;
  winPoints?: number;
  drawPoints?: number;
  lossPoints?: number;
  twoLegged?: boolean;
  knockoutTeamCount?: number;
}

export interface Goal {
  id: number;
  teamName: string;
  teamId: number;
  scorerName: string;
  scorerId: number | null;
  ownGoal: boolean;
  minute: number;
  second?: number;
}

export interface Match {
  id: number;
  championshipId: number;
  championshipName: string;
  homeTeam: Team;
  awayTeam: Team;
  round: number;
  bracketPosition?: number;
  scheduledAt?: string;
  status: MatchStatus;
  durationSeconds: number;
  goalLimit: number;
  homeScore: number;
  awayScore: number;
  goals: Goal[];
}

export interface MatchTimer {
  matchId: number;
  status: string;
  elapsedSeconds: number;
  totalSeconds: number;
}

// Statistics (from MongoDB)
export interface GoalDetail {
  scorerName: string;
  scorerId: number | null;
  teamName: string;
  ownGoal: boolean;
  minute: number;
  second?: number;
}

export interface MatchStatistics {
  id: string;
  matchId: number;
  championshipId: number;
  homeTeamName: string;
  awayTeamName: string;
  homeTeamId: number;
  awayTeamId: number;
  homeScore: number;
  awayScore: number;
  durationSeconds: number;
  goals: GoalDetail[];
  homeStats: Record<string, unknown>;
  awayStats: Record<string, unknown>;
  playedAt: string;
}

export interface TeamStanding {
  teamId: number;
  teamName: string;
  played: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface TopScorer {
  playerId: number;
  playerName: string;
  teamName: string;
  goals: number;
}

export interface ChampionshipStatistics {
  id: string;
  championshipId: number;
  championshipName: string;
  standings: TeamStanding[];
  topScorers: TopScorer[];
}
