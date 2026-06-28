import type { Source } from "../../api/client";

type SourceListProps = {
  sources: Source[];
  title?: string;
};

export function SourceList({ sources, title = "来源配置" }: SourceListProps) {
  return (
    <section className="panel" id="sources" aria-labelledby="sources-title">
      <div className="section-heading">
        <h2 id="sources-title">{title}</h2>
        <span>{sources.length} 个来源</span>
      </div>
      {sources.length === 0 ? (
        <p className="empty-state">暂无岗位来源。</p>
      ) : (
        <ul className="source-list">
          {sources.map((source) => (
            <li key={source.id}>
              <div>
                <strong>{source.name}</strong>
                <span>{source.type} · 最近同步 {source.lastSync}</span>
              </div>
              <div className="source-flags">
                <span className="status">{source.status}</span>
                {source.needsLogin ? <span>登录</span> : null}
                {source.needsCaptcha ? <span>验证码</span> : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
