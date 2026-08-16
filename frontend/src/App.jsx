import { useState, useEffect } from 'react'
import { getGames, analyzeGame } from './api/chessApi'
import UsernameForm from './components/UsernameForm'
import GameList from './components/GameList'
import Board from './components/Board'
import EvalBar from './components/EvalBar'
import MoveList from './components/MoveList'
import AccuracyBadge from './components/AccuracyBadge'
import './App.css'

const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

export default function App() {
  const [games, setGames] = useState([])
  const [analysis, setAnalysis] = useState(null)
  const [currentIndex, setCurrentIndex] = useState(-1)
  const [loadingGames, setLoadingGames] = useState(false)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [error, setError] = useState(null)

  const handleFetchGames = async (username) => {
    setError(null)
    setGames([])
    setAnalysis(null)
    setCurrentIndex(-1)
    setLoadingGames(true)
    try {
      const data = await getGames(username, 10)
      setGames(data)
    } catch (e) {
      setError(`Could not fetch games for "${username}". Check the username and try again.`)
    } finally {
      setLoadingGames(false)
    }
  }

  const handleSelectGame = async (game) => {
    setError(null)
    setAnalysis(null)
    setCurrentIndex(-1)
    setLoadingAnalysis(true)
    try {
      const data = await analyzeGame(game)
      setAnalysis(data)
      setCurrentIndex(0)
    } catch (e) {
      setError('Analysis failed. Please try again.')
    } finally {
      setLoadingAnalysis(false)
    }
  }

  const handleMoveClick = (index) => setCurrentIndex(index)

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (!analysis) return
      if (e.key === 'ArrowRight') {
        e.preventDefault()
        setCurrentIndex(i => Math.min(i + 1, analysis.moves.length - 1))
      }
      if (e.key === 'ArrowLeft') {
        e.preventDefault()
        setCurrentIndex(i => Math.max(i - 1, 0))
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [analysis])

  const currentMove  = analysis && currentIndex >= 0 ? analysis.moves[currentIndex] : null
  const currentFen   = currentMove ? currentMove.fenAfter : START_FEN
  // scoreAfter is from the moving player's POV; negate for black moves to get white's POV
  const currentScore = currentMove
    ? (currentMove.whiteMove ? currentMove.scoreAfter : -currentMove.scoreAfter)
    : 0

  return (
    <div className="app">
      <UsernameForm onSubmit={handleFetchGames} loading={loadingGames} />

      {error && <div className="error">{error}</div>}

      {/* Game picker */}
      {games.length > 0 && !analysis && !loadingAnalysis && (
        <div className="main-layout">
          <GameList games={games} onSelect={handleSelectGame} loading={loadingAnalysis} />
        </div>
      )}

      {/* Stockfish loading */}
      {loadingAnalysis && (
        <div className="main-layout">
          <div className="loading-analysis">
            <div className="spinner" />
            <p>Analyzing with Stockfish...</p>
            <p className="loading-sub">This takes about 15 seconds</p>
          </div>
        </div>
      )}

      {/* Analysis — 3-column layout */}
      {analysis && (
        <div className="analysis-layout">

          {/* LEFT PANEL — eval bar + black player info */}
          <div className="left-panel">
            <div className="player-card black">
              <span className="player-color-dot black-dot" />
              <span className="player-name">{analysis.blackPlayer}</span>
              <AccuracyBadge accuracy={analysis.blackAccuracy} />
            </div>
            <EvalBar score={currentScore} />
            <div className="player-card white">
              <span className="player-color-dot white-dot" />
              <span className="player-name">{analysis.whitePlayer}</span>
              <AccuracyBadge accuracy={analysis.whiteAccuracy} />
            </div>
          </div>

          {/* CENTER PANEL — board fills all available space */}
          <div className="center-panel">
            <Board fen={currentFen} moveAnalysis={currentMove} />
            {currentMove && (
              <div className="move-detail">
                <span>Played: <strong>{currentMove.playedMove}</strong></span>
                <span>Best: <strong style={{ color: '#4caf50' }}>{currentMove.bestMove}</strong></span>
                <span>Loss: <strong>{currentMove.centipawnLoss}cp</strong></span>
                <span className={`classification ${currentMove.classification.toLowerCase()}`}>
                  {currentMove.classification}
                </span>
                {currentMove.bookMove && (
                  <span className="book-badge">&#9670; Book Move</span>
                )}
              </div>
            )}
            <div className="nav-hint">← → arrow keys to navigate moves</div>
          </div>

          {/* RIGHT PANEL — move list */}
          <div className="right-panel">
            <div className="game-result-badge">{analysis.result}</div>
            {(currentMove?.openingName || analysis.openingName) && (
              <div className="opening-name">
                {(currentMove?.openingEco || analysis.openingEco) && (
                  <span className="opening-eco">{currentMove?.openingEco || analysis.openingEco}</span>
                )}
                {currentMove?.openingName || analysis.openingName}
              </div>
            )}
            <MoveList
              moves={analysis.moves}
              currentIndex={currentIndex}
              onMoveClick={handleMoveClick}
            />
          </div>

        </div>
      )}
    </div>
  )
}
