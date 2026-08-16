import { Chessboard } from 'react-chessboard'
import { useEffect, useRef, useState, useCallback } from 'react'
import { Chess } from 'chess.js'
import { analyzePosition } from '../api/chessApi'

const CLASS_COLORS = {
  BRILLIANT: 'rgba(27, 172, 166, 0.6)',
  BEST:      'rgba(76, 175, 80, 0.6)',
  EXCELLENT: 'rgba(139, 195, 74, 0.6)',
  GOOD:      'rgba(158, 158, 158, 0.4)',
  INACCURACY:'rgba(255, 193, 7, 0.6)',
  MISTAKE:   'rgba(255, 152, 0, 0.6)',
  BLUNDER:   'rgba(244, 67, 54, 0.6)',
}

export default function Board({ fen, moveAnalysis, onExploreScore }) {
  const containerRef = useRef(null)
  const [size, setSize] = useState(300)

  const [exploreFen, setExploreFen] = useState(null)
  const [exploreChess, setExploreChess] = useState(null)
  const [engineResult, setEngineResult] = useState(null)

  // Exit explore when game position changes (user navigates moves)
  useEffect(() => {
    setExploreFen(null)
    setExploreChess(null)
    setEngineResult(null)
    if (onExploreScore) onExploreScore(null)
  }, [fen])

  useEffect(() => {
    if (!containerRef.current) return
    const observer = new ResizeObserver(entries => {
      for (const entry of entries) {
        const w = Math.floor(entry.contentRect.width)
        if (w > 0) setSize(w)
      }
    })
    observer.observe(containerRef.current)
    return () => observer.disconnect()
  }, [])

  const analyze = useCallback(async (currentFen) => {
    try {
      const result = await analyzePosition(currentFen)
      setEngineResult(result)
      if (onExploreScore) {
        const chess = new Chess(currentFen)
        const isWhiteTurn = chess.turn() === 'w'
        const score = isWhiteTurn ? result.centipawnScore : -result.centipawnScore
        onExploreScore(score)
      }
    } catch {
      // silent
    }
  }, [onExploreScore])

  const onDrop = useCallback(({ sourceSquare, targetSquare }) => {
    const baseFen = exploreFen ?? fen
    const chess = new Chess(baseFen)
    try {
      const move = chess.move({ from: sourceSquare, to: targetSquare, promotion: 'q' })
      if (!move) return false
      const newFen = chess.fen()
      setExploreFen(newFen)
      setExploreChess(chess)
      analyze(newFen)
      return true
    } catch {
      return false
    }
  }, [fen, exploreFen, analyze])

  const isExploring = exploreFen !== null
  const displayFen = exploreFen ?? fen

  const customSquareStyles = {}
  const customArrows = []

  if (!isExploring && moveAnalysis) {
    const from = moveAnalysis.playedMove.slice(0, 2)
    const to   = moveAnalysis.playedMove.slice(2, 4)
    const moveColor = moveAnalysis.bookMove
      ? 'rgba(200, 160, 94, 0.5)'
      : (CLASS_COLORS[moveAnalysis.classification] || 'rgba(158,158,158,0.4)')
    customSquareStyles[from] = { backgroundColor: moveColor }
    customSquareStyles[to]   = { backgroundColor: moveColor }

    const arrowColor = moveAnalysis.bookMove
      ? 'rgba(200, 160, 94, 1)'
      : (CLASS_COLORS[moveAnalysis.classification]?.replace(/[\d.]+\)$/, '1)') || 'rgba(158,158,158,1)')
    customArrows.push({ startSquare: from, endSquare: to, color: arrowColor })

    if (moveAnalysis.bestMove && moveAnalysis.bestMove !== moveAnalysis.playedMove) {
      customArrows.push({
        startSquare: moveAnalysis.bestMove.slice(0, 2),
        endSquare:   moveAnalysis.bestMove.slice(2, 4),
        color: 'rgba(76, 175, 80, 1)',
      })
    }
  }

  // In explore mode: show best move arrow from engine
  if (isExploring && engineResult?.bestMove?.length >= 4) {
    customArrows.push({
      startSquare: engineResult.bestMove.slice(0, 2),
      endSquare:   engineResult.bestMove.slice(2, 4),
      color: 'rgba(76, 175, 80, 1)',
    })
  }

  return (
    <div className="board-container" ref={containerRef}>
      <Chessboard
        options={{
          position: displayFen,
          boardWidth: size,
          squareStyles: customSquareStyles,
          arrows: customArrows,
          allowDragging: true,
          onPieceDrop: onDrop,
          animationDurationInMs: 150,
        }}
      />
    </div>
  )
}
