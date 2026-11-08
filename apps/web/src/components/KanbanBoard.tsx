import {
  DndContext,
  DragOverlay,
  PointerSensor,
  closestCorners,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  type UniqueIdentifier,
} from '@dnd-kit/core'
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Tag, TaskSummary } from '../api/client'
import { formatDay, initials, ticketKey } from '../lib/ticketMeta'

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const
export type BoardStatus = (typeof STATUSES)[number]

const STATUS_LABEL: Record<BoardStatus, string> = {
  TODO: 'Todo',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
}

function isStatus(id: UniqueIdentifier): id is BoardStatus {
  return STATUSES.includes(String(id) as BoardStatus)
}

function tagTone(name: string): string {
  const n = name.toLowerCase()
  if (n.includes('auth') || n.includes('access')) return 'violet'
  if (n.includes('agent') || n.includes('api')) return 'blue'
  if (n.includes('infra') || n.includes('dev')) return 'slate'
  if (n.includes('doc')) return 'cyan'
  if (n.includes('pay') || n.includes('android') || n.includes('ios')) return 'amber'
  return 'slate'
}

type Props = {
  projectName: string
  tasks: TaskSummary[]
  userDisplayName?: string
  onPlace: (taskId: string, status: BoardStatus, sortOrder: number) => Promise<void>
}

export function KanbanBoard({ projectName, tasks, userDisplayName, onPlace }: Props) {
  const navigate = useNavigate()
  const [items, setItems] = useState<TaskSummary[]>(tasks)
  const [activeId, setActiveId] = useState<string | null>(null)
  const [overColumn, setOverColumn] = useState<BoardStatus | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  useEffect(() => {
    if (!activeId) setItems(tasks)
  }, [tasks, activeId])

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 6 },
    }),
  )

  const byStatus = useMemo(() => {
    const map: Record<BoardStatus, TaskSummary[]> = {
      TODO: [],
      IN_PROGRESS: [],
      DONE: [],
    }
    for (const t of items) {
      const status = (STATUSES.includes(t.status as BoardStatus) ? t.status : 'TODO') as BoardStatus
      map[status].push(t)
    }
    for (const status of STATUSES) {
      map[status].sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.createdAt.localeCompare(b.createdAt),
      )
    }
    return map
  }, [items])

  const activeTask = activeId ? items.find((t) => t.id === activeId) ?? null : null

  function findContainer(id: UniqueIdentifier): BoardStatus | null {
    if (isStatus(id)) return id
    const task = items.find((t) => t.id === id)
    return task && isStatus(task.status) ? (task.status as BoardStatus) : null
  }

  function handleDragStart(event: DragStartEvent) {
    setActiveId(String(event.active.id))
  }

  function handleDragOver(event: DragOverEvent) {
    const { active, over } = event
    if (!over) return

    const activeContainer = findContainer(active.id)
    const overContainer = findContainer(over.id)
    if (!activeContainer || !overContainer) return
    setOverColumn(overContainer)

    if (activeContainer === overContainer) return

    setItems((prev) => {
      const activeTask = prev.find((t) => t.id === active.id)
      if (!activeTask) return prev

      const without = prev.filter((t) => t.id !== active.id)
      const dest = without
        .filter((t) => t.status === overContainer)
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.createdAt.localeCompare(b.createdAt))

      let overIndex = isStatus(over.id) ? dest.length : dest.findIndex((t) => t.id === over.id)
      if (overIndex < 0) overIndex = dest.length

      dest.splice(overIndex, 0, { ...activeTask, status: overContainer, sortOrder: overIndex })
      const reindexed = dest.map((t, i) => ({ ...t, status: overContainer, sortOrder: i }))
      const others = without.filter((t) => t.status !== overContainer)
      return [...others, ...reindexed]
    })
  }

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    setActiveId(null)
    setOverColumn(null)
    if (!over) {
      setItems(tasks)
      return
    }

    const activeContainer = findContainer(active.id)
    const overContainer = findContainer(over.id)
    if (!activeContainer || !overContainer) {
      setItems(tasks)
      return
    }

    const columnTasks = items
      .filter((t) => t.status === overContainer)
      .sort((a, b) => a.sortOrder - b.sortOrder)
    const oldIndex = columnTasks.findIndex((t) => t.id === active.id)
    let newIndex = isStatus(over.id)
      ? columnTasks.length - 1
      : columnTasks.findIndex((t) => t.id === over.id)
    if (newIndex < 0) newIndex = columnTasks.length - 1

    let nextColumn = columnTasks
    if (oldIndex !== newIndex && oldIndex >= 0) {
      nextColumn = arrayMove(columnTasks, oldIndex, newIndex)
    }

    const nextItems = items.map((t) => {
      if (t.status !== overContainer && t.id !== active.id) return t
      const idx = nextColumn.findIndex((x) => x.id === t.id)
      if (idx < 0) return t
      return { ...t, status: overContainer, sortOrder: idx }
    })
    setItems(nextItems)

    const placed = nextItems.find((t) => t.id === active.id)
    if (!placed) return

    const original = tasks.find((t) => t.id === active.id)
    if (original && original.status === placed.status && original.sortOrder === placed.sortOrder) {
      return
    }

    setBusyId(placed.id)
    try {
      await onPlace(placed.id, placed.status as BoardStatus, placed.sortOrder)
    } catch {
      setItems(tasks)
    } finally {
      setBusyId(null)
    }
  }

  function handleDragCancel() {
    setActiveId(null)
    setOverColumn(null)
    setItems(tasks)
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={(e) => void handleDragEnd(e)}
      onDragCancel={handleDragCancel}
    >
      <div className={`kanban${activeId ? ' is-dragging' : ''}`}>
        {STATUSES.map((status) => (
          <KanbanColumn
            key={status}
            status={status}
            tasks={byStatus[status]}
            projectName={projectName}
            userDisplayName={userDisplayName}
            activeId={activeId}
            busyId={busyId}
            dropActive={overColumn === status}
            onOpen={(id) => navigate(`/tasks/${id}`)}
          />
        ))}
      </div>
      <DragOverlay dropAnimation={null}>
        {activeTask ? (
          <TicketCard
            task={activeTask}
            projectName={projectName}
            userDisplayName={userDisplayName}
            overlay
          />
        ) : null}
      </DragOverlay>
    </DndContext>
  )
}

function KanbanColumn({
  status,
  tasks,
  projectName,
  userDisplayName,
  activeId,
  busyId,
  dropActive,
  onOpen,
}: {
  status: BoardStatus
  tasks: TaskSummary[]
  projectName: string
  userDisplayName?: string
  activeId: string | null
  busyId: string | null
  dropActive: boolean
  onOpen: (id: string) => void
}) {
  const { setNodeRef } = useDroppable({ id: status })
  const ids = tasks.map((t) => t.id)

  return (
    <section className={`kanban-col${dropActive ? ' drop-active' : ''}`}>
      <header className="kanban-head">
        <span>{STATUS_LABEL[status]}</span>
        <span className="count">{tasks.length}</span>
      </header>
      <div className="kanban-body" ref={setNodeRef}>
        <SortableContext items={ids} strategy={verticalListSortingStrategy}>
          {tasks.map((t) => (
            <SortableTicket
              key={t.id}
              task={t}
              projectName={projectName}
              userDisplayName={userDisplayName}
              dragging={activeId === t.id}
              busy={busyId === t.id}
              onOpen={() => onOpen(t.id)}
            />
          ))}
        </SortableContext>
        {tasks.length === 0 && <p className="kanban-empty">Drop tickets here</p>}
      </div>
    </section>
  )
}

function SortableTicket({
  task,
  projectName,
  userDisplayName,
  dragging,
  busy,
  onOpen,
}: {
  task: TaskSummary
  projectName: string
  userDisplayName?: string
  dragging: boolean
  busy: boolean
  onOpen: () => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
    disabled: busy,
  })
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <TicketCard
        task={task}
        projectName={projectName}
        userDisplayName={userDisplayName}
        dragging={dragging || isDragging}
        busy={busy}
        onOpen={onOpen}
      />
    </div>
  )
}

function TicketCard({
  task,
  projectName,
  userDisplayName,
  dragging,
  busy,
  overlay,
  onOpen,
}: {
  task: TaskSummary
  projectName: string
  userDisplayName?: string
  dragging?: boolean
  busy?: boolean
  overlay?: boolean
  onOpen?: () => void
}) {
  const key = ticketKey(projectName, task.id)
  return (
    <article
      className={`ticket-card${task.status === 'DONE' ? ' done' : ''}${
        dragging ? ' dragging' : ''
      }${busy ? ' busy' : ''}${overlay ? ' drag-overlay' : ''}`}
      onClick={() => {
        if (!dragging && onOpen) onOpen()
      }}
    >
      <div className="ticket-tags">
        <span className="tag key">{key}</span>
        {(task.tags ?? []).slice(0, 2).map((tag: Tag) => (
          <span key={tag.id} className={`tag ${tagTone(tag.name)}`}>
            {tag.name}
          </span>
        ))}
      </div>
      <h3>{task.title}</h3>
      {task.description ? <p className="ticket-desc">{task.description}</p> : null}
      <div className="ticket-meta">
        <span className="meta-left">
          {formatDay(task.createdAt) && <span>Created {formatDay(task.createdAt)}</span>}
          {task.estimatedDeliveryDate && <span> · Due {formatDay(task.estimatedDeliveryDate)}</span>}
        </span>
        <span className="avatar xs" title={userDisplayName}>
          {initials(userDisplayName ?? 'U')}
        </span>
      </div>
    </article>
  )
}
