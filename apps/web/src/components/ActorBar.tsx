import { getActor, setActor } from '../api/client'

type Props = { onChange?: () => void }

export function ActorBar({ onChange }: Props) {
  const actor = getActor()
  return (
    <div className="actor-bar">
      <label>
        Acting as
        <input
          defaultValue={actor.name}
          onBlur={(e) => {
            setActor({ name: e.target.value || 'anonymous' })
            onChange?.()
          }}
        />
      </label>
      <label>
        Source
        <select
          defaultValue={actor.source}
          onChange={(e) => {
            setActor({ source: e.target.value as 'web' | 'api' | 'agent' })
            onChange?.()
          }}
        >
          <option value="web">web</option>
          <option value="api">api</option>
          <option value="agent">agent</option>
        </select>
      </label>
    </div>
  )
}
