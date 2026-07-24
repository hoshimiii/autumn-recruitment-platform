import type { Source } from "../../api/client";
import { displayLabel } from "../../api/labels";

export type SourceAction = "test" | "run" | "edit" | "delete";

export function SourceList({ sources, onAction, pending }: {
  sources: Source[];
  onAction: (source: Source, action: SourceAction) => void;
  pending?: string;
}) {
  return <section className="panel" aria-labelledby="sources-title">
    <div className="section-heading"><h2 id="sources-title">来源清单</h2><span>{sources.length} 个来源</span></div>
    {sources.length === 0 ? <p className="empty-state">暂无岗位来源。</p> : <ul className="source-list">{sources.map((source) => {
      const disabled = pending === String(source.id);
      return <li key={source.id}>
        <div><strong>{source.name}</strong><span>{source.mode} · {displayLabel(source.trustLevel)} · {source.lastStatus ? displayLabel(source.lastStatus) : source.lastRunAt ?? "尚未运行"}</span></div>
        <div className="source-flags"><span className="status">{displayLabel(source.complianceStatus)}</span><span className="status">{source.enabled ? "启用" : "停用"}</span><span className="status">v{source.configVersion}</span></div>
        <div className="source-actions">
          <ActionButton action="test" label="测试" source={source} disabled={disabled} onAction={onAction} />
          <ActionButton action="run" label="运行" source={source} disabled={disabled} onAction={onAction} />
          <ActionButton action="edit" label="编辑" source={source} disabled={disabled} onAction={onAction} />
          <ActionButton action="delete" label="删除" source={source} disabled={disabled} onAction={onAction} />
        </div>
      </li>;
    })}</ul>}
  </section>;
}

function ActionButton({ action, label, source, disabled, onAction }: {
  action: SourceAction; label: string; source: Source; disabled: boolean; onAction: (source: Source, action: SourceAction) => void;
}) {
  return <button type="button" className={action === "delete" ? "secondary-button" : undefined}
    aria-label={`${label}来源 ${source.name}`} disabled={disabled} onClick={() => onAction(source, action)}>{disabled ? "处理中" : label}</button>;
}
