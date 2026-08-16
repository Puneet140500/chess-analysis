import { Chessboard } from 'react-chessboard'
import { useEffect, useRef, useState } from 'react'

const CLASS_COLORS = {
  BRILLIANT: 'rgba(27, 172, 166, 0.6)',
  BEST:      'rgba(76, 175, 80, 0.6)',
  EXCELLENT: 'rgba(139, 195, 74, 0.6)',
  GOOD:      'rgba(158, 158, 158, 0.4)',
  INACCURACY:'rgba(255, 193, 7, 0.6)',
  MISTAKE:   'rgba(255, 152, 0, 0.6)',
  BLUNDER:   'rgba(244, 67, 54, 0.6)',
}

export default function Board({ fen, moveAnalysis }) {
  const containerRef = useRef(null)
  const [size, setSize] = useState(300)

  // Observe the container and match boardWidth to its actual rendered width
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

  const customSquareStyles = {}
  const customArrows = []

  if (moveAnalysis) {
    const fromSquare = moveAnalysis.playedMove.slice(0, 2)
    const toSquare   = moveAnalysis.playedMove.slice(2, 4)

    // highlight the from/to squares — brown if book move, otherwise classification color
    const moveColor = moveAnalysis.bookMove
      ? 'rgba(200, 160, 94, 0.5)'
      : (CLASS_COLORS[moveAnalysis.classification] || 'rgba(158,158,158,0.4)')
    customSquareStyles[fromSquare] = { backgroundColor: moveColor }
    customSquareStyles[toSquare]   = { backgroundColor: moveColor }

    // arrow for the played move — brown if book move, otherwise classification color
    const arrowColor = moveAnalysis.bookMove
      ? 'rgba(200, 160, 94, 1)'
      : (CLASS_COLORS[moveAnalysis.classification]?.replace(/[\d.]+\)$/, '1)') || 'rgba(158,158,158,1)')
    customArrows.push({ startSquare: fromSquare, endSquare: toSquare, color: arrowColor })

    // green arrow for best move if it differs (and isn't the played move)
    if (moveAnalysis.bestMove && moveAnalysis.bestMove !== moveAnalysis.playedMove) {
      customArrows.push({
        startSquare: moveAnalysis.bestMove.slice(0, 2),
        endSquare:   moveAnalysis.bestMove.slice(2, 4),
        color: 'rgba(76, 175, 80, 1)',
      })
    }
  }

  return (
    <div className="board-container" ref={containerRef}>
      <Chessboard
        options={{
          position: fen,
          boardWidth: size,
          squareStyles: customSquareStyles,
          arrows: customArrows,
          allowDragging: false,
        }}
      />
    </div>
  )
}
