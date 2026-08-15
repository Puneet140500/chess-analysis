import { useState } from 'react'

export default function UsernameForm({ onSubmit, loading }) {
  const [username, setUsername] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    if (username.trim()) onSubmit(username.trim())
  }

  return (
    <div className="username-form">
      <h1>Chess Analysis Engine</h1>
      <p>Enter a chess.com username to analyze their recent games</p>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="chess.com username"
          value={username}
          onChange={e => setUsername(e.target.value)}
          disabled={loading}
        />
        <button type="submit" disabled={loading || !username.trim()}>
          {loading ? 'Loading...' : 'Fetch Games'}
        </button>
      </form>
    </div>
  )
}
