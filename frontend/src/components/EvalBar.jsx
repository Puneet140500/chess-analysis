// Vertical bar showing who is winning
// score is centipawns from white's POV: +300 = white winning by 3 pawns
export default function EvalBar({ score }) {
  // Clamp score to [-1000, 1000] so the bar doesn't go fully one color too easily
  const clamped = Math.max(-1000, Math.min(1000, score))

  // Convert to 0-100 percentage for white's portion of the bar
  // score=0 → 50%, score=1000 → 95%, score=-1000 → 5%
  const whitePct = 50 + (clamped / 1000) * 45

  // Display label: convert centipawns to pawns with 1 decimal
  const displayScore = Math.abs(score) >= 32000
    ? 'M'  // mate
    : (Math.abs(score) / 100).toFixed(1)

  const advantage = score > 0 ? 'white' : score < 0 ? 'black' : 'equal'

  return (
    <div className="eval-bar-container">
      <div className="eval-bar">
        {/* Black's portion (top) */}
        <div
          className="eval-black"
          style={{ height: `${100 - whitePct}%` }}
        />
        {/* White's portion (bottom) */}
        <div
          className="eval-white"
          style={{ height: `${whitePct}%` }}
        />
      </div>
      <div className={`eval-score ${advantage}`}>
        {score >= 0 ? '+' : '-'}{displayScore}
      </div>
    </div>
  )
}
