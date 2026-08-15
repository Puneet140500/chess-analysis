const getColor = (acc) => {
  if (acc >= 90) return '#4caf50'
  if (acc >= 75) return '#8bc34a'
  if (acc >= 60) return '#ffc107'
  if (acc >= 45) return '#ff9800'
  return '#f44336'
}

export default function AccuracyBadge({ accuracy }) {
  return (
    <div className="accuracy-badge">
      <div style={{ fontSize: '0.72rem', color: '#888' }}>Accuracy</div>
      <div className="accuracy-value" style={{ color: getColor(accuracy) }}>
        {accuracy}%
      </div>
    </div>
  )
}
