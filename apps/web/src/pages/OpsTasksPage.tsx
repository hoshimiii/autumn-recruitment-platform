import type { Task } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { TaskList } from "../features/tasks/TaskList";

type OpsTasksPageProps = {
  tasks: Task[];
};

export function OpsTasksPage({ tasks }: OpsTasksPageProps) {
  return (
    <main className="workbench">
      <PageHeader
        title="任务队列"
        description="沉淀导入、复核、导出等运营动作，避免干扰岗位展示主页。"
      />

      <div className="ops-grid single-column">
        <TaskList tasks={tasks} />
      </div>
    </main>
  );
}
