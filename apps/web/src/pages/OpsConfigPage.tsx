import { useEffect, useState } from "react";
import { ApiError, createSource, deleteSource, listSources, runSource, testSource, updateSource } from "../api/client";
import type { Source, SourceInput } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { SourceForm } from "../features/sources/SourceForm";
import { SourceList } from "../features/sources/SourceList";
import type { SourceAction } from "../features/sources/SourceList";

export function OpsConfigPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [editing, setEditing] = useState<Source | undefined>();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string>();
  const [pending, setPending] = useState<string>();
  useEffect(() => {
    const controller = new AbortController();
    listSources(controller.signal).then(setSources).catch((reason: unknown) => {
      if (!controller.signal.aborted) setError(message(reason, "无法加载来源配置。"));
    });
    return () => controller.abort();
  }, []);
  const save = async (input: SourceInput) => {
    const saved = editing ? await updateSource(editing.id, input) : await createSource(input);
    setSources((items) => editing ? items.map((item) => item.id === saved.id ? saved : item) : [saved, ...items]);
    setEditing(undefined); setCreating(false); setError(undefined);
  };
  const action = async (source: Source, kind: SourceAction) => {
    if (kind === "edit") { setEditing(source); setCreating(false); setError(undefined); return; }
    setPending(String(source.id)); setError(undefined);
    try {
      if (kind === "delete") {
        await deleteSource(source.id);
        setSources((items) => items.filter((item) => item.id !== source.id));
      }
      else {
        const run = kind === "test" ? await testSource(source.id) : await runSource(source.id);
        setSources((items) => items.map((item) => item.id === source.id ? { ...item, lastStatus: run.status, lastRunAt: run.finishedAt ?? run.startedAt } : item));
      }
    } catch (reason) { setError(message(reason, `${kind === "test" ? "测试" : kind === "run" ? "运行" : "删除"}来源失败。`)); }
    finally { setPending(undefined); }
  };
  const closeForm = () => { setEditing(undefined); setCreating(false); };
  return <main className="workbench"><PageHeader title="来源配置" description="只配置已确认的公开信息源和可选分类 Agent。"
    actions={<button type="button" onClick={() => { setCreating(true); setEditing(undefined); setError(undefined); }}>新增来源</button>} />
    {error ? <section className="panel state-panel" role="alert">{error}</section> : null}
    <div className="ops-grid single-column">{creating || editing ? <SourceForm source={editing} onCancel={closeForm} onSave={save} /> : null}
      <SourceList sources={sources} pending={pending} onAction={action} /></div>
  </main>;
}

function message(reason: unknown, fallback: string): string { return reason instanceof ApiError ? reason.message : fallback; }
