import { useState } from "react";
import { ApiError } from "../../api/client";
import type { Source, SourceInput } from "../../api/client";

type FormValue = Omit<SourceInput, "termsConfirmedAt"> & { termsConfirmedAt: string };

const initial: FormValue = {
  name: "", type: "COMPANY", baseUrl: "", listUrl: "", adapterType: "HTML", mode: "HTML",
  complianceStatus: "UNKNOWN", trustLevel: "P1", companyCategory: "", enabled: false,
  cron: "0 0 8 * * *", timezone: "Asia/Shanghai", minIntervalSeconds: 5, maxPages: 1, maxItems: 50,
  selectors: "{}", fieldMapping: "{}", termsUrl: "", termsConfirmedAt: "", robotsUrl: "", timeoutSeconds: 15,
  aiEnabled: false, aiProvider: "", aiModel: "", secretEnvRef: ""
};

export function SourceForm({ source, onCancel, onSave }: {
  source?: Source;
  onCancel: () => void;
  onSave: (source: SourceInput) => Promise<void>;
}) {
  const [value, setValue] = useState<FormValue>(() => source ? fromSource(source) : initial);
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const update = <K extends keyof FormValue>(key: K, next: FormValue[K]) => setValue((current) => ({ ...current, [key]: next }));
  const canEnable = value.complianceStatus === "ALLOWED" && Boolean(value.termsConfirmedAt);
  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const input = toInput(value);
    const validation = validate(input, canEnable);
    if (validation) return setError(validation);
    setSaving(true); setError(undefined);
    try { await onSave(input); } catch (reason) { setError(apiMessage(reason, "保存来源失败。")); } finally { setSaving(false); }
  };
  return <section className="panel" aria-labelledby="source-form-title">
    <div className="section-heading"><h2 id="source-form-title">{source ? "编辑来源" : "新增来源"}</h2><span>{source ? `版本 ${source.configVersion}` : "默认停用"}</span></div>
    <form className="manual-form" onSubmit={submit}><div className="form-grid">
      <Field label="来源名称" value={value.name} onChange={(next) => update("name", next)} required />
      <Select label="来源类型" value={value.type} values={["COMPANY", "CAMPUS", "PLATFORM", "GOVERNMENT"]} onChange={(next) => update("type", next)} />
      <Field label="官网 URL" type="url" value={value.baseUrl} onChange={(next) => update("baseUrl", next)} required />
      <Field label="列表 URL" type="url" value={value.listUrl} onChange={(next) => update("listUrl", next)} required />
      <Select label="采集模式" value={value.mode} values={["JSON", "HTML", "MANUAL"]} onChange={(next) => update("mode", next as FormValue["mode"])} />
      <Field label="适配器类型" value={value.adapterType} onChange={(next) => update("adapterType", next)} required />
      <Field label="企业分类" value={value.companyCategory} onChange={(next) => update("companyCategory", next)} required />
      <Select label="信任级别" value={value.trustLevel} values={["P1", "P2", "P3", "P4"]} onChange={(next) => update("trustLevel", next)} />
      <Field label="Cron" value={value.cron} onChange={(next) => update("cron", next)} />
      <Field label="时区" value={value.timezone} onChange={(next) => update("timezone", next)} />
      <Field label="最小间隔（秒）" type="number" value={String(value.minIntervalSeconds)} onChange={(next) => update("minIntervalSeconds", Number(next))} />
      <Field label="最大页面数" type="number" value={String(value.maxPages)} onChange={(next) => update("maxPages", Number(next))} />
      <Field label="最大条目数" type="number" value={String(value.maxItems)} onChange={(next) => update("maxItems", Number(next))} />
      <Field label="超时秒数" type="number" value={String(value.timeoutSeconds)} onChange={(next) => update("timeoutSeconds", Number(next))} />
      <Field label="robots URL" type="url" value={value.robotsUrl ?? ""} onChange={(next) => update("robotsUrl", next)} />
      <Field label="条款 URL" type="url" value={value.termsUrl ?? ""} onChange={(next) => update("termsUrl", next)} />
      <Field label="条款确认时间" type="datetime-local" value={value.termsConfirmedAt} onChange={(next) => update("termsConfirmedAt", next)} />
      <Select label="合规状态" value={value.complianceStatus} values={["UNKNOWN", "ALLOWED", "REVIEW_REQUIRED", "FORBIDDEN", "LOGIN_REQUIRED", "CAPTCHA_REQUIRED"]} onChange={(next) => update("complianceStatus", next)} />
      <label className="check-label"><input aria-label="启用来源" type="checkbox" checked={value.enabled} disabled={!canEnable && !value.enabled}
        onChange={(event) => update("enabled", event.target.checked)} />启用来源</label>
    </div>
    {value.mode !== "MANUAL" ? <><JsonField label="Selectors" value={value.selectors ?? "{}"} onChange={(next) => update("selectors", next)} />
      <JsonField label="字段映射" value={value.fieldMapping ?? "{}"} onChange={(next) => update("fieldMapping", next)} /></> : null}
    <div className="form-grid"><label className="check-label"><input aria-label="启用 AI" type="checkbox" checked={value.aiEnabled} onChange={(event) => update("aiEnabled", event.target.checked)} />启用 AI 分类/摘要</label>
      <Field label="AI Provider" value={value.aiProvider ?? ""} onChange={(next) => update("aiProvider", next)} />
      <Field label="AI Model" value={value.aiModel ?? ""} onChange={(next) => update("aiModel", next)} />
      <Field label="密钥环境变量引用" value={value.secretEnvRef ?? ""} onChange={(next) => update("secretEnvRef", next)} />
    </div><div className="form-actions"><button disabled={saving} type="submit">{saving ? "保存中" : "保存来源"}</button><button className="secondary-button" disabled={saving} type="button" onClick={onCancel}>取消</button></div>
    {error ? <p className="form-error" role="alert">{error}</p> : null}</form>
  </section>;
}

function fromSource(source: Source): FormValue {
  const { id, etag, lastModified, lastRunAt, lastStatus, configVersion, termsConfirmedAt, ...input } = source;
  return { ...input, termsConfirmedAt: toLocalDateTime(termsConfirmedAt), secretEnvRef: "" };
}

function toInput(value: FormValue): SourceInput {
  const { termsConfirmedAt, secretEnvRef, ...input } = value;
  return { ...input, termsConfirmedAt: termsConfirmedAt ? new Date(termsConfirmedAt).toISOString() : undefined,
    secretEnvRef: secretEnvRef?.trim() || undefined };
}

function validate(value: SourceInput, canEnable: boolean): string | undefined {
  if (!value.name || !isHttpUrl(value.baseUrl) || !isHttpUrl(value.listUrl)) return "请填写来源名称、官网 URL 和列表 URL。";
  if (value.enabled && !canEnable) return "只有已确认条款的 ALLOWED 来源可以启用。";
  if (value.mode !== "MANUAL" && (!isJsonObject(value.selectors) || !isJsonObject(value.fieldMapping))) return "Selectors 和字段映射必须是 JSON 对象。";
  if (value.secretEnvRef && !/^[A-Z][A-Z0-9_]*$/u.test(value.secretEnvRef)) return "密钥环境变量引用必须是大写环境变量名，不能填写明文 token。";
  return undefined;
}

function Field({ label, value, onChange, type = "text", required = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean }) {
  return <label>{label}<input aria-label={label} required={required} type={type} value={value} onChange={(event) => onChange(event.target.value)} /></label>;
}
function Select({ label, value, values, onChange }: { label: string; value: string; values: string[]; onChange: (value: string) => void }) {
  return <label>{label}<select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>{values.map((item) => <option key={item}>{item}</option>)}</select></label>;
}
function JsonField({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) { return <label>{label}<textarea aria-label={label} value={value} onChange={(event) => onChange(event.target.value)} rows={3} /></label>; }
function isHttpUrl(value: string): boolean { try { return ["http:", "https:"].includes(new URL(value).protocol); } catch { return false; } }
function isJsonObject(value?: string): boolean { try { const parsed = JSON.parse(value ?? "{}"); return Boolean(parsed) && !Array.isArray(parsed) && typeof parsed === "object"; } catch { return false; } }
function toLocalDateTime(value?: string): string { return value ? new Date(value).toISOString().slice(0, 16) : ""; }
function apiMessage(reason: unknown, fallback: string): string { return reason instanceof ApiError ? reason.message : fallback; }
