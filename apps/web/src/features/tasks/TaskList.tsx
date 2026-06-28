import type { Task } from "../../api/client";

type TaskListProps = {
  tasks: Task[];
};

export function TaskList({ tasks }: TaskListProps) {
  return (
    <section className="panel" id="tasks" aria-labelledby="tasks-title">
      <div className="section-heading">
        <h2 id="tasks-title">任务</h2>
        <span>{tasks.length} 个任务</span>
      </div>
      {tasks.length === 0 ? (
        <p className="empty-state">暂无系统任务。</p>
      ) : (
        <ul className="task-list">
          {tasks.map((task) => (
            <li key={task.id}>
              <strong>{task.title}</strong>
              <span>{task.owner}</span>
              <span>{task.due}</span>
              <span className="status">{task.state}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
