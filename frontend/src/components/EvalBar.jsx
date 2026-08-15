// Vertical bar showing who is winning
// score is centipawns from white's POV: +300 = white winning by 3 pawns
export default function EvalBar({ score }) {
  const clamped = Math.max(-1000, Math.min(1000, score))
  const whitePct = 50 + (clamped / 1000) * 45

  const displayScore = Math.abs(score) >= 32000
    ? 'M'
    : (Math.abs(score) / 100).toFixed(1)

  const advantage = score > 0 ? 'white' : score < 0 ? 'black' : 'equal'

  return (
    <div className="eval-bar-container">
      <div className="eval-bar">
        <div className="eval-black" style={{ height: `${100 - whitePct}%` }} />
        <div className="eval-white" style={{ height: `${whitePct}%` }} />
      </div>
      <div className={`eval-score ${advantage}`}>
        {score >= 0 ? '+' : '-'}{displayScore}
      </div>
    </div>
  )
}
