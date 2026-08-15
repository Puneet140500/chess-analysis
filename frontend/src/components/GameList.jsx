export default function GameList({ games, onSelect, loading }) {
  const formatTime = (unix) => {
    const d = new Date(unix * 1000)
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  const formatTimeControl = (tc) => {
    if (!tc) return '?'
    if (tc.includes('+')) {
      const [base, inc] = tc.split('+')
      return `${Math.floor(base / 60)}+${inc}`
    }
    return `${Math.floor(tc / 60)} min`
  }

  return (
    <div className="game-list">
      <h2>Recent Games</h2>
      {games.map(game => (
        <div
          key={game.gameId}
          className="game-item"
          onClick={() => !loading && onSelect(game)}
        >
          <div className="game-players">
            <span className="white-player">⬜ {game.whitePlayer}</span>
            <span className="vs">vs</span>
            <span className="black-player">⬛ {game.blackPlayer}</span>
          </div>
          <div className="game-meta">
            <span className={`result ${game.result === '1-0' ? 'white-win' : game.result === '0-1' ? 'black-win' : 'draw'}`}>
              {game.result}
            </span>
            <span className="time-control">{formatTimeControl(game.timeControl)}</span>
            <span className="end-time">{formatTime(game.endTime)}</span>
          </div>
          {loading && <span className="analyzing">Analyzing...</span>}
        </div>
      ))}
    </div>
  )
}
