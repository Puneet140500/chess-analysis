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
  if (moveAnalysis) {
    const toSquare = moveAnalysis.playedMove.slice(2, 4)
    customSquareStyles[toSquare] = {
      backgroundColor: CLASS_COLORS[moveAnalysis.classification] || 'rgba(158,158,158,0.4)'
    }
  }

  const customArrows = []
  if (moveAnalysis && moveAnalysis.bestMove && moveAnalysis.bestMove !== moveAnalysis.playedMove) {
    customArrows.push([
      moveAnalysis.bestMove.slice(0, 2),
      moveAnalysis.bestMove.slice(2, 4),
      'rgb(0, 128, 0)'
    ])
  }

  return (
    <div className="board-container" ref={containerRef}>
      <Chessboard
        position={fen}
        boardWidth={size}
        customSquareStyles={customSquareStyles}
        customArrows={customArrows}
        arePiecesDraggable={false}
      />
    </div>
  )
}
