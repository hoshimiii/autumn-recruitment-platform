import type { ManualActionInput } from "../../api/client";

type ManualRecordListProps = {
  records: ManualActionInput[];
};

export function ManualRecordList({ records }: ManualRecordListProps) {
  return (
    <section className="panel" aria-labelledby="manual-records-title">
      <div className="section-heading">
        <h2 id="manual-records-title">人工记录</h2>
        <span>{records.length} 条</span>
      </div>
      {records.length === 0 ? (
        <p className="empty-state">暂无人工记录。</p>
      ) : (
        <ul className="stack-list">
          {records.map((record, index) => {
            const title = record.jobTitle || record.searchKeywords?.join(", ") || "未命名岗位";

            return (
              <li key={`${record.platform}-${record.companyName}-${index}`}>
                <div>
                  <strong>{record.companyName} · {title}</strong>
                  <span>{record.requiredAction} · {record.status ?? "pending"}</span>
                </div>
                <span className="priority">P{record.priority ?? 0}</span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
