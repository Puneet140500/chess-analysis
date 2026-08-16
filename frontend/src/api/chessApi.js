import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

// Fetch recent games for a username from chess.com (via our backend)
export const getGames = (username, limit = 10) =>
  api.get('/games', { params: { username, limit } }).then(r => r.data)

// Send a game to backend for full Stockfish analysis
export const analyzeGame = (game) =>
  api.post('/analyze', game).then(r => r.data)

// Analyze a single FEN position — returns bestMove + eval (for interactive board)
export const analyzePosition = (fen) =>
  api.post('/analyze-position', { fen }).then(r => r.data)
