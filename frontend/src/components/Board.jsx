import { Chessboard } from 'react-chessboard'

// Classification colors for the move highlight square
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
  // Build custom square styles — highlight the square the player moved TO
  const customSquareStyles = {}
  if (moveAnalysis) {
    const toSquare = moveAnalysis.playedMove.slice(2, 4) // "e2e4" → "e4"
    customSquareStyles[toSquare] = {
      backgroundColor: CLASS_COLORS[moveAnalysis.classification] || 'rgba(158,158,158,0.4)'
    }
  }

  // Build best move arrow — from square → to square
  const customArrows = []
  if (moveAnalysis && moveAnalysis.bestMove && moveAnalysis.bestMove !== moveAnalysis.playedMove) {
    const from = moveAnalysis.bestMove.slice(0, 2) // "e2"
    const to   = moveAnalysis.bestMove.slice(2, 4) // "e4"
    customArrows.push([from, to, 'rgb(0, 128, 0)']) // green arrow
  }

  return (
    <div className="board-container">
      <Chessboard
        position={fen}
        boardWidth={480}
        customSquareStyles={customSquareStyles}
        customArrows={customArrows}
        arePiecesDraggable={false}
      />
    </div>
  )
}
