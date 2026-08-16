import { useEffect, useRef } from 'react'

// Color for each classification
const CLASS_COLORS = {
  BRILLIANT: '#1baca6',
  BEST:      '#4caf50',
  EXCELLENT: '#8bc34a',
  GOOD:      '#9e9e9e',
  INACCURACY:'#ffc107',
  MISTAKE:   '#ff9800',
  BLUNDER:   '#f44336',
}

// Symbol shown next to each move
const CLASS_SYMBOLS = {
  BRILLIANT: '!!',
  BEST:      '!',
  EXCELLENT: '!',
  GOOD:      '',
  INACCURACY:'?!',
  MISTAKE:   '?',
  BLUNDER:   '??',
}

export default function MoveList({ moves, currentIndex, onMoveClick }) {
  const activeRef = useRef(null)

  // Auto-scroll to keep active move visible
  useEffect(() => {
    if (activeRef.current) {
      activeRef.current.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  }, [currentIndex])

  // Group moves into pairs: [[white_move, black_move], ...]
  const movePairs = []
  for (let i = 0; i < moves.length; i += 2) {
    movePairs.push([moves[i], moves[i + 1]])
  }

  return (
    <div className="move-list">
      <h3>Moves</h3>
      <div className="move-pairs">
        {movePairs.map((pair, pairIdx) => (
          <div key={pairIdx} className="move-pair">
            <span className="move-number">{pairIdx + 1}.</span>

            {/* White's move */}
            <MoveCell
              move={pair[0]}
              index={pairIdx * 2}
              currentIndex={currentIndex}
              onClick={onMoveClick}
              ref={pairIdx * 2 === currentIndex ? activeRef : null}
            />

            {/* Black's move (may not exist if game ended on white's move) */}
            {pair[1] && (
              <MoveCell
                move={pair[1]}
                index={pairIdx * 2 + 1}
                currentIndex={currentIndex}
                onClick={onMoveClick}
                ref={pairIdx * 2 + 1 === currentIndex ? activeRef : null}
              />
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// Single move cell — shows move + classification symbol + tooltip on hover
import { forwardRef } from 'react'

const MoveCell = forwardRef(({ move, index, currentIndex, onClick }, ref) => {
  const color = CLASS_COLORS[move.classification] || '#9e9e9e'
  const symbol = CLASS_SYMBOLS[move.classification] || ''
  const isActive = index === currentIndex

  return (
    <span
      ref={ref}
      className={`move-cell ${isActive ? 'active' : ''} ${move.bookMove ? 'book-move' : ''}`}
      onClick={() => onClick(index)}
      title={`${move.bookMove ? 'Book move — ' : ''}${move.classification} — ${move.centipawnLoss}cp loss — accuracy ${Math.round(move.accuracy)}%`}
    >
      {move.playedMove}
      {symbol && (
        <sup style={{ color }}>{symbol}</sup>
      )}
    </span>
  )
})
