import { useState, useEffect, useCallback } from 'react'
import { Chess } from 'chess.js'
import { Chessboard } from 'react-chessboard'
import { LESSONS } from '../data/lessons'

const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

export default function LessonsPage() {
  const [selectedOpening, setSelectedOpening] = useState(null)
  const [selectedLine, setSelectedLine]       = useState(null)
  const [moveIndex, setMoveIndex]             = useState(-1)
  const [fen, setFen]                         = useState(START_FEN)
  const [boardSize, setBoardSize]             = useState(400)

  const opening = LESSONS.find(l => l.id === selectedOpening)
  const line    = opening?.lines.find(l => l.id === selectedLine)

  const fenAtIndex = useCallback((moves, idx) => {
    if (idx < 0) return START_FEN
    const chess = new Chess()
    for (let i = 0; i <= idx; i++) {
      chess.move({ from: moves[i].uci.slice(0, 2), to: moves[i].uci.slice(2, 4), promotion: 'q' })
    }
    return chess.fen()
  }, [])

  useEffect(() => {
    setFen(line ? fenAtIndex(line.moves, moveIndex) : START_FEN)
  }, [line, moveIndex, fenAtIndex])

  useEffect(() => {
    const handleKey = (e) => {
      if (!line) return
      if (e.key === 'ArrowRight') { e.preventDefault(); setMoveIndex(i => Math.min(i + 1, line.moves.length - 1)) }
      if (e.key === 'ArrowLeft')  { e.preventDefault(); setMoveIndex(i => Math.max(i - 1, -1)) }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [line])

  const selectOpening = (id) => {
    const op = LESSONS.find(l => l.id === id)
    setSelectedOpening(id)
    setSelectedLine(op.lines[0].id)
    setMoveIndex(-1)
  }

  const selectLine = (id) => {
    setSelectedLine(id)
    setMoveIndex(-1)
  }

  const currentMove = line && moveIndex >= 0 ? line.moves[moveIndex] : null
  const isLast = line && moveIndex === line.moves.length - 1

  const customArrows = []
  const customSquareStyles = {}
  if (currentMove) {
    const from = currentMove.uci.slice(0, 2)
    const to   = currentMove.uci.slice(2, 4)
    customSquareStyles[from] = { backgroundColor: 'rgba(76, 175, 80, 0.4)' }
    customSquareStyles[to]   = { backgroundColor: 'rgba(76, 175, 80, 0.5)' }
    customArrows.push({ startSquare: from, endSquare: to, color: 'rgba(76, 175, 80, 1)' })
  }

  return (
    <div className="lessons-layout">

      {/* LEFT — opening cards */}
      <div className="lessons-left">
        <h3 className="lessons-title">Openings</h3>
        {LESSONS.map(op => (
          <div key={op.id}
            className={`lesson-card ${selectedOpening === op.id ? 'active' : ''}`}
            onClick={() => selectOpening(op.id)}
          >
            <div className="lesson-card-eco">{op.eco}</div>
            <div className="lesson-card-name">{op.name}</div>
            <div className="lesson-card-desc">{op.description}</div>
          </div>
        ))}
      </div>

      {/* CENTER — board */}
      <div className="lessons-center">
        {!opening ? (
          <div className="lessons-placeholder"><p>Select an opening on the left to begin</p></div>
        ) : (
          <>
            {/* Line tabs */}
            <div className="line-tabs">
              {opening.lines.map(l => (
                <button
                  key={l.id}
                  className={`line-tab ${selectedLine === l.id ? 'active' : ''}`}
                  onClick={() => selectLine(l.id)}
                >
                  <span className="line-tab-eco">{l.eco}</span>
                  {l.name}
                </button>
              ))}
            </div>

            {line && (
              <>
                <div className="lesson-header">
                  <span className="lesson-header-name">{line.name}</span>
                  <span className="lesson-progress">{moveIndex + 1} / {line.moves.length}</span>
                </div>

                <div className="lessons-board-wrap">
                  <Chessboard
                    options={{
                      position: fen,
                      boardWidth: boardSize,
                      squareStyles: customSquareStyles,
                      arrows: customArrows,
                      allowDragging: false,
                      animationDurationInMs: 200,
                    }}
                  />
                </div>

                <div className="lesson-nav">
                  <button className="lesson-nav-btn" onClick={() => setMoveIndex(i => Math.max(i - 1, -1))} disabled={moveIndex < 0}>← Prev</button>
                  <button className="lesson-nav-btn primary" onClick={() => setMoveIndex(i => Math.min(i + 1, line.moves.length - 1))} disabled={isLast}>Next →</button>
                </div>
                <div className="nav-hint">← → arrow keys to navigate</div>
              </>
            )}
          </>
        )}
      </div>

      {/* RIGHT — move list + explanation */}
      <div className="lessons-right">
        {line ? (
          <>
            <h3 className="lessons-movelist-title">Moves</h3>
            <div className="lessons-moves">
              {Array.from({ length: Math.ceil(line.moves.length / 2) }, (_, pairIdx) => {
                const wIdx = pairIdx * 2
                const bIdx = pairIdx * 2 + 1
                return (
                  <div key={pairIdx} className="lesson-move-row">
                    <span className="lesson-move-num">{pairIdx + 1}.</span>
                    <LessonMoveCell move={line.moves[wIdx]} index={wIdx} current={moveIndex} onClick={setMoveIndex} />
                    {line.moves[bIdx] && <LessonMoveCell move={line.moves[bIdx]} index={bIdx} current={moveIndex} onClick={setMoveIndex} />}
                  </div>
                )
              })}
            </div>

            <div className="lesson-explanation">
              {moveIndex < 0
                ? <span className="expl-text">{opening.description}</span>
                : <>
                    <span className={`expl-side ${currentMove.side === 'w' ? 'white' : 'black'}`}>
                      {currentMove.side === 'w' ? 'White' : 'Black'}:
                    </span>
                    <span className="expl-san"> {currentMove.san} — </span>
                    <span className="expl-text">{currentMove.explanation}</span>
                  </>
              }
            </div>
          </>
        ) : (
          <div className="lessons-right-empty"><p>Select an opening to see moves</p></div>
        )}
      </div>

    </div>
  )
}

function LessonMoveCell({ move, index, current, onClick }) {
  return (
    <span
      className={`lesson-move-cell ${index === current ? 'active' : ''} ${move.side}`}
      onClick={() => onClick(index)}
    >
      {move.san}
    </span>
  )
}
