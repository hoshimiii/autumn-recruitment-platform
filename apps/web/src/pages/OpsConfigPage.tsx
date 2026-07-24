import { useEffect, useState } from "react";
import { ApiError, createSource, listSources } from "../api/client";
import type { Source, SourceInput } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { SourceList } from "../features/sources/SourceList";

const initial: SourceInput = { name: "", type: "official", baseUrl: "", adapterType: "OfficialCareerSiteAdapter", mode: "HTML", trustLevel: "P1", companyCategory: "", enabled: false, cron: "0 0 8 * * *", timezone: "Asia/Shanghai", minIntervalSeconds: 5, maxPages: 10, selectors: "", fieldMapping: "", robotsUrl: "", termsUrl: "", complianceStatus: "unknown", aiEnabled: false, aiProvider: "", aiModel: "", aiSecretRef: "" };

export function OpsConfigPage() {
  const [sources, setSources] = useState<Source[]>([]); const [showForm, setShowForm] = useState(false); const [error, setError] = useState<string>();
  useEffect(() => { const controller = new AbortController(); listSources(controller.signal).then(setSources).catch((reason: unknown) => { if (!controller.signal.aborted) setError(reason instanceof ApiError ? reason.message : "无法加载来源配置。"); }); return () => controller.abort(); }, []);
  const save = async (source: SourceInput) => { const created = await createSource(source); setSources((items) => [created, ...items]); setShowForm(false); };
  return <main className="workbench"><PageHeader title="来源配置" description="只配置已确认的公开信息源和可选分类 Agent。" actions={<button type="button" onClick={() => setShowForm(true)}>新增来源</button>} />
    {error ? <section className="panel state-panel" role="alert">{error}</section> : null}
    <div className="ops-grid single-column">{showForm ? <SourceForm onCancel={() => setShowForm(false)} onSave={save} /> : null}<SourceList sources={sources} title="来源清单" /></div>
  </main>;
}

function SourceForm({ onCancel, onSave }: { onCancel: () => void; onSave: (source: SourceInput) => Promise<void> }) {
  const [value, setValue] = useState(initial); const [error, setError] = useState<string>(); const [saving, setSaving] = useState(false);
  const update = <K extends keyof SourceInput>(key: K, next: SourceInput[K]) => setValue((current) => ({ ...current, [key]: next }));
  const submit = async (event: React.FormEvent) => { event.preventDefault(); if (!value.name || !isHttpUrl(value.baseUrl)) return setError("请填写来源名称和合法的官网 URL。"); if (value.aiEnabled && !/^[A-Z][A-Z0-9_]*$/u.test(value.aiSecretRef ?? "")) return setError("密钥环境变量引用必须是大写环境变量名，不能填写明文 token。"); setSaving(true); setError(undefined); try { await onSave(value); } catch (reason) { setError(reason instanceof ApiError ? reason.message : "保存来源失败。"); } finally { setSaving(false); } };
  return <section className="panel" aria-labelledby="source-form-title"><div className="section-heading"><h2 id="source-form-title">新增来源</h2><span>配置版本由服务端维护</span></div><form className="manual-form" onSubmit={submit}><div className="form-grid">
    <Field label="来源名称" value={value.name} onChange={(next) => update("name", next)} required /><Field label="官网 URL" type="url" value={value.baseUrl} onChange={(next) => update("baseUrl", next)} required />
    <Select label="采集模式" value={value.mode} values={["JSON", "HTML", "MANUAL"]} onChange={(next) => update("mode", next as SourceInput["mode"])} /><Select label="信任级别" value={value.trustLevel} values={["P1", "P2", "P3", "P4"]} onChange={(next) => update("trustLevel", next)} />
    <Field label="企业分类" value={value.companyCategory} onChange={(next) => update("companyCategory", next)} /><Field label="适配器类型" value={value.adapterType} onChange={(next) => update("adapterType", next)} />
    <Field label="Cron" value={value.cron} onChange={(next) => update("cron", next)} /><Field label="时区" value={value.timezone} onChange={(next) => update("timezone", next)} />
    <Field label="最小间隔（秒）" type="number" value={String(value.minIntervalSeconds)} onChange={(next) => update("minIntervalSeconds", Number(next))} /><Field label="最大页面数" type="number" value={String(value.maxPages)} onChange={(next) => update("maxPages", Number(next))} />
    <Field label="robots URL" type="url" value={value.robotsUrl ?? ""} onChange={(next) => update("robotsUrl", next)} /><Field label="条款 URL" type="url" value={value.termsUrl ?? ""} onChange={(next) => update("termsUrl", next)} />
    <Select label="合规状态" value={value.complianceStatus} values={["allowed", "review_required", "unknown", "forbidden", "login_required", "captcha_required"]} onChange={(next) => update("complianceStatus", next)} />
    <label className="check-label"><input aria-label="启用来源" type="checkbox" checked={value.enabled} onChange={(event) => update("enabled", event.target.checked)} />启用来源</label>
  </div><label>Selectors<textarea value={value.selectors} onChange={(event) => update("selectors", event.target.value)} rows={3} /></label><label>字段映射<textarea value={value.fieldMapping} onChange={(event) => update("fieldMapping", event.target.value)} rows={3} /></label>
  <label className="check-label"><input aria-label="启用 AI" type="checkbox" checked={value.aiEnabled} onChange={(event) => update("aiEnabled", event.target.checked)} />启用 AI 分类/摘要</label>{value.aiEnabled ? <div className="form-grid"><Field label="Provider" value={value.aiProvider ?? ""} onChange={(next) => update("aiProvider", next)} /><Field label="Model" value={value.aiModel ?? ""} onChange={(next) => update("aiModel", next)} /><Field label="密钥环境变量引用" value={value.aiSecretRef ?? ""} onChange={(next) => update("aiSecretRef", next)} /></div> : null}
  <div className="form-actions"><button disabled={saving} type="submit">保存来源</button><button className="secondary-button" type="button" onClick={onCancel}>取消</button></div>{error ? <p className="form-error" role="alert">{error}</p> : null}</form></section>;
}

function Field({ label, value, onChange, type = "text", required = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean }) { return <label>{label}<input aria-label={label} required={required} type={type} value={value} onChange={(event) => onChange(event.target.value)} /></label>; }
function Select({ label, value, values, onChange }: { label: string; value: string; values: string[]; onChange: (value: string) => void }) { return <label>{label}<select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>{values.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>; }
function isHttpUrl(value: string): boolean { try { return ["http:", "https:"].includes(new URL(value).protocol); } catch { return false; } }
