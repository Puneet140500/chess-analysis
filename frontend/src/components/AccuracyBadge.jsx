// Shows accuracy score with color coding — green=high, yellow=medium, red=low
export default function AccuracyBadge({ label, accuracy, color }) {
  const getColor = (acc) => {
    if (acc >= 90) return '#4caf50'  // green
    if (acc >= 75) return '#8bc34a'  // light green
    if (acc >= 60) return '#ffc107'  // yellow
    if (acc >= 45) return '#ff9800'  // orange
    return '#f44336'                 // red
  }

  return (
    <div className="accuracy-badge" style={{ borderColor: getColor(accuracy) }}>
      <div className="accuracy-label">{label}</div>
      <div className="accuracy-value" style={{ color: getColor(accuracy) }}>
        {accuracy}%
      </div>
    </div>
  )
}
