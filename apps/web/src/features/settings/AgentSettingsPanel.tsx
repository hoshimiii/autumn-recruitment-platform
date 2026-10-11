import { useEffect, useState } from "react";
import { ApiError, checkAgentSettings, getAgentSettings, updateAgentSettings } from "../../api/client";
import type { AgentSettings, AgentSettingsCheck, AgentSettingsInput, ReasoningEffort } from "../../api/client";

const efforts: ReasoningEffort[] = ["low", "medium", "high", "xhigh"];

export function AgentSettingsPanel() {
  const [value, setValue] = useState<AgentSettings>();
  const [check, setCheck] = useState<AgentSettingsCheck>();
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [checking, setChecking] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    getAgentSettings(controller.signal).then(setValue).catch((reason: unknown) => {
      if (!controller.signal.aborted) setError(message(reason, "无法加载 Agent 设置。"));
    });
    return () => controller.abort();
  }, []);
  const update = <K extends keyof AgentSettingsInput>(key: K, next: AgentSettingsInput[K]) => setValue((current) => current ? { ...current, [key]: next } : current);
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!value) return;
    if (value.secretEnvRef && !/^[A-Z][A-Z0-9_]*$/u.test(value.secretEnvRef)) return setError("密钥环境变量名必须是大写环境变量名，不能填写明文 token。");
    setSaving(true); setError(undefined);
    try { setValue(await updateAgentSettings(toInput(value))); }
    catch (reason) { setError(message(reason, "保存 Agent 设置失败。")); }
    finally { setSaving(false); }
  };
  const runCheck = async () => {
    setChecking(true); setError(undefined);
    try { setCheck(await checkAgentSettings()); }
    catch (reason) { setError(message(reason, "无法检查 Agent 连接。")); }
    finally { setChecking(false); }
  };
  return <section className="panel" aria-labelledby="agent-settings-title">
    <div className="section-heading"><h2 id="agent-settings-title">Agent 工作流</h2><span>{value ? `版本 ${value.configVersion}` : "加载中"}</span></div>
    {value ? <form className="manual-form" onSubmit={save}>
      <div className="form-grid">
        <label className="check-label"><input aria-label="启用全局 Agent 工作流" type="checkbox" checked={value.enabled} onChange={(event) => update("enabled", event.target.checked)} />启用全局 Agent 工作流</label>
        <p className="settings-status">密钥状态：{value.secretConfigured ? "已配置" : "未配置"}</p>
        <Field label="Sub2API URL" value={value.baseUrl} onChange={(next) => update("baseUrl", next)} />
        <Field label="密钥环境变量名" value={value.secretEnvRef} onChange={(next) => update("secretEnvRef", next)} />
        <Field label="扫描模型" value={value.scanModel} onChange={(next) => update("scanModel", next)} />
        <Effort label="扫描推理等级" value={value.scanReasoningEffort} onChange={(next) => update("scanReasoningEffort", next)} />
        <Field label="处理模型" value={value.processModel} onChange={(next) => update("processModel", next)} />
        <Effort label="处理推理等级" value={value.processReasoningEffort} onChange={(next) => update("processReasoningEffort", next)} />
        <Field label="超时秒数" type="number" value={String(value.timeoutSeconds)} onChange={(next) => update("timeoutSeconds", Number(next))} />
      </div>
      <div className="form-actions"><button disabled={saving} type="submit">{saving ? "保存中" : "保存 Agent 设置"}</button><button className="secondary-button" disabled={checking} type="button" onClick={runCheck}>{checking ? "检查中" : "检查连接"}</button></div>
    </form> : <p className="empty-state">正在加载 Agent 设置。</p>}
    {check ? <div className="agent-check" role="status"><span>{check.message}</span><span>{check.secretConfigured ? "密钥已配置。" : "密钥未配置。"}</span><span className={check.scanModelAvailable ? "check-ok" : "check-warning"}>{check.scanModelAvailable ? "扫描模型可用。" : "扫描模型未出现在目录中。"}</span><span className={check.processModelAvailable ? "check-ok" : "check-warning"}>{check.processModelAvailable ? "处理模型可用。" : "处理模型未出现在目录中。"}</span><span className="model-catalog">已发现模型：{check.models.length ? check.models.join("、") : "未返回模型"}</span></div> : null}
    {error ? <p className="form-error" role="alert">{error}</p> : null}
  </section>;
}

function toInput(value: AgentSettings): AgentSettingsInput { const { enabled, baseUrl, secretEnvRef, scanModel, scanReasoningEffort, processModel, processReasoningEffort, timeoutSeconds } = value; return { enabled, baseUrl, secretEnvRef: secretEnvRef.trim(), scanModel, scanReasoningEffort, processModel, processReasoningEffort, timeoutSeconds }; }
function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) { return <label>{label}<input aria-label={label} type={type} value={value} onChange={(event) => onChange(event.target.value)} /></label>; }
function Effort({ label, value, onChange }: { label: string; value: ReasoningEffort; onChange: (value: ReasoningEffort) => void }) { return <label>{label}<select aria-label={label} value={value} onChange={(event) => onChange(event.target.value as ReasoningEffort)}>{efforts.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>; }
function message(reason: unknown, fallback: string): string { return reason instanceof ApiError ? reason.message : fallback; }
