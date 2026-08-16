import { useState, useEffect, useCallback, useRef } from 'react'
import { Chess } from 'chess.js'
import { Chessboard } from 'react-chessboard'
import { searchOpenings } from '../api/chessApi'

const ECO_GROUPS = [
  { label: 'All',  prefix: '' },
  { label: 'A',    prefix: 'A', desc: 'Flank / Irregular' },
  { label: 'B',    prefix: 'B', desc: 'Semi-Open (1.e4)' },
  { label: 'C',    prefix: 'C', desc: 'Open (1.e4 e5)'   },
  { label: 'D',    prefix: 'D', desc: 'Closed (1.d4 d5)' },
  { label: 'E',    prefix: 'E', desc: 'Indian Defenses'  },
]

// Parse "1. e4 e5 2. Nf3 Nc6" → ["e4","e5","Nf3","Nc6"]
function parseSan(movesStr) {
  return movesStr.replace(/\d+\.\s*/g, '').trim().split(/\s+/).filter(Boolean)
}

// Build array of FENs (one per move, starting from move 1)
function buildFens(sanMoves) {
  const chess = new Chess()
  const fens = []
  for (const san of sanMoves) {
    try { chess.move(san) } catch { break }
    fens.push(chess.fen())
  }
  return fens
}

export default function LessonsPage() {
  const [query, setQuery]           = useState('')
  const [ecoGroup, setEcoGroup]     = useState('')
  const [openings, setOpenings]     = useState([])
  const [loading, setLoading]       = useState(false)
  const [selected, setSelected]     = useState(null)   // { eco, name, moves, sanMoves, fens }
  const [moveIndex, setMoveIndex]   = useState(-1)
  const [boardSize, setBoardSize]   = useState(380)
  const boardRef = useRef(null)
  const searchTimer = useRef(null)

  // Fetch openings from backend (debounced)
  const fetch_ = useCallback(async (q, eco) => {
    setLoading(true)
    try {
      const results = await searchOpenings(q, eco, 120)
      setOpenings(results)
    } catch { setOpenings([]) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => {
    clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => fetch_(query, ecoGroup), 300)
    return () => clearTimeout(searchTimer.current)
  }, [query, ecoGroup, fetch_])

  // Board resize
  useEffect(() => {
    if (!boardRef.current) return
    const obs = new ResizeObserver(entries => {
      const w = Math.floor(entries[0].contentRect.width)
      if (w > 0) setBoardSize(w)
    })
    obs.observe(boardRef.current)
    return () => obs.disconnect()
  }, [])

  // Keyboard navigation
  useEffect(() => {
    const handler = (e) => {
      if (!selected) return
      if (e.key === 'ArrowRight') { e.preventDefault(); setMoveIndex(i => Math.min(i + 1, selected.fens.length - 1)) }
      if (e.key === 'ArrowLeft')  { e.preventDefault(); setMoveIndex(i => Math.max(i - 1, -1)) }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [selected])

  const selectOpening = (op) => {
    const sanMoves = parseSan(op.moves)
    const fens = buildFens(sanMoves)
    setSelected({ ...op, sanMoves, fens })
    setMoveIndex(-1)
  }

  const currentFen = selected
    ? (moveIndex < 0 ? 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1' : selected.fens[moveIndex])
    : 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

  // Highlight last move
  const customSquareStyles = {}
  const customArrows = []
  if (selected && moveIndex >= 0) {
    const chess = new Chess()
    const prevFen = moveIndex === 0
      ? 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
      : selected.fens[moveIndex - 1]
    chess.loadFen ? chess.loadFen(prevFen) : (() => { const c = new Chess(prevFen); Object.assign(chess, c) })()
    try {
      const c2 = new Chess(prevFen)
      const mv = c2.move(selected.sanMoves[moveIndex])
      if (mv) {
        customSquareStyles[mv.from] = { backgroundColor: 'rgba(76,175,80,0.4)' }
        customSquareStyles[mv.to]   = { backgroundColor: 'rgba(76,175,80,0.5)' }
        customArrows.push({ startSquare: mv.from, endSquare: mv.to, color: 'rgba(76,175,80,1)' })
      }
    } catch {}
  }

  // Group move pairs for display
  const movePairs = selected ? [] : []
  if (selected) {
    for (let i = 0; i < selected.sanMoves.length; i += 2) {
      movePairs.push({ num: Math.floor(i / 2) + 1, w: i, b: i + 1 })
    }
  }

  return (
    <div className="lessons-layout">

      {/* LEFT — search + opening list */}
      <div className="lessons-left">
        <input
          className="opening-search"
          placeholder="Search openings..."
          value={query}
          onChange={e => setQuery(e.target.value)}
        />

        <div className="eco-group-tabs">
          {ECO_GROUPS.map(g => (
            <button
              key={g.label}
              className={`eco-group-btn ${ecoGroup === g.prefix ? 'active' : ''}`}
              onClick={() => setEcoGroup(g.prefix)}
              title={g.desc}
            >{g.label}</button>
          ))}
        </div>

        {loading
          ? <div className="openings-loading">Loading...</div>
          : <div className="openings-list">
              {openings.length === 0
                ? <div className="openings-empty">No results</div>
                : openings.map((op, i) => (
                    <div
                      key={i}
                      className={`opening-item ${selected?.name === op.name && selected?.eco === op.eco ? 'active' : ''}`}
                      onClick={() => selectOpening(op)}
                    >
                      <span className="opening-item-eco">{op.eco}</span>
                      <span className="opening-item-name">{op.name}</span>
                    </div>
                  ))
              }
            </div>
        }
      </div>

      {/* CENTER — board */}
      <div className="lessons-center">
        {!selected ? (
          <div className="lessons-placeholder">
            <p>Search and select an opening to study</p>
            <p style={{ fontSize: '0.78rem', color: '#555', marginTop: 8 }}>3,810 openings available</p>
          </div>
        ) : (
          <>
            <div className="lesson-header">
              <span className="lesson-header-eco">{selected.eco}</span>
              <span className="lesson-header-name">{selected.name}</span>
              <span className="lesson-progress">
                {moveIndex < 0 ? 'Start' : `${moveIndex + 1} / ${selected.sanMoves.length}`}
              </span>
            </div>

            <div className="lessons-board-wrap" ref={boardRef}>
              <Chessboard
                options={{
                  position: currentFen,
                  boardWidth: boardSize,
                  squareStyles: customSquareStyles,
                  arrows: customArrows,
                  allowDragging: false,
                  animationDurationInMs: 180,
                }}
              />
            </div>

            <div className="lesson-nav">
              <button className="lesson-nav-btn"
                onClick={() => setMoveIndex(i => Math.max(i - 1, -1))}
                disabled={moveIndex < 0}>← Prev</button>
              <button className="lesson-nav-btn primary"
                onClick={() => setMoveIndex(i => Math.min(i + 1, selected.fens.length - 1))}
                disabled={moveIndex === selected.fens.length - 1}>Next →</button>
            </div>
            <div className="nav-hint">← → arrow keys to navigate</div>
          </>
        )}
      </div>

      {/* RIGHT — moves */}
      <div className="lessons-right">
        {selected ? (
          <>
            <h3 className="lessons-movelist-title">Moves</h3>
            <div className="lessons-moves">
              {movePairs.map(pair => (
                <div key={pair.num} className="lesson-move-row">
                  <span className="lesson-move-num">{pair.num}.</span>
                  <span
                    className={`lesson-move-cell w ${pair.w === moveIndex ? 'active' : ''}`}
                    onClick={() => setMoveIndex(pair.w)}
                  >{selected.sanMoves[pair.w]}</span>
                  {selected.sanMoves[pair.b] !== undefined && (
                    <span
                      className={`lesson-move-cell b ${pair.b === moveIndex ? 'active' : ''}`}
                      onClick={() => setMoveIndex(pair.b)}
                    >{selected.sanMoves[pair.b]}</span>
                  )}
                </div>
              ))}
            </div>

            <div className="lesson-explanation">
              <span className="expl-text" style={{ fontSize: '0.78rem', color: '#777' }}>
                Full PGN:
              </span>
              <div style={{ fontSize: '0.75rem', color: '#aaa', marginTop: 4, lineHeight: 1.6, wordBreak: 'break-word' }}>
                {selected.moves}
              </div>
            </div>
          </>
        ) : (
          <div className="lessons-right-empty">
            <p>Moves will appear here</p>
          </div>
        )}
      </div>

    </div>
  )
}
