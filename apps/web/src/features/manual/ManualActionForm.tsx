import type { FormEvent } from "react";
import type { ManualActionInput, RequiredManualAction } from "../../api/client";

type ManualActionFormProps = {
  onSubmit?: (record: ManualActionInput) => Promise<void> | void;
};

const actionOptions: RequiredManualAction[] = [
  "manual_check",
  "login_required",
  "captcha_required",
  "manual_apply",
  "verify_official_source"
];

export function ManualActionForm({ onSubmit = () => undefined }: ManualActionFormProps) {
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);

    await onSubmit({
      platform: readText(data, "platform"),
      companyName: readText(data, "companyName"),
      jobTitle: readText(data, "jobTitle"),
      requiredAction: readText(data, "requiredAction") as RequiredManualAction,
      sourceUrl: optionalText(data, "sourceUrl"),
      techStackKeywords: splitList(readText(data, "techStackKeywords")),
      certificateNotes: optionalText(data, "certificateNotes"),
      priority: Number(readText(data, "priority") || 0),
      requiresLogin: data.get("requiresLogin") === "on",
      requiresCaptcha: data.get("requiresCaptcha") === "on",
      notes: optionalText(data, "notes"),
      status: "pending"
    });
  };

  return (
    <section className="panel" id="manual" aria-labelledby="manual-title">
      <div className="section-heading">
        <h2 id="manual-title">人工处理</h2>
        <span>登录、验证码、人工复核</span>
      </div>
      <form className="manual-form" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            平台
            <input name="platform" required placeholder="例如：BOSS直聘" />
          </label>
          <label>
            公司名称
            <input name="companyName" required placeholder="公司全称" />
          </label>
          <label>
            岗位名称
            <input name="jobTitle" required placeholder="岗位标题" />
          </label>
          <label>
            需处理动作
            <select name="requiredAction" defaultValue="manual_check">
              {actionOptions.map((action) => (
                <option key={action} value={action}>{action}</option>
              ))}
            </select>
          </label>
          <label>
            来源链接
            <input name="sourceUrl" type="url" placeholder="https://..." />
          </label>
          <label>
            技术栈关键词
            <input aria-label="技术栈" name="techStackKeywords" placeholder="Java, SQL" />
          </label>
          <label>
            证书/资质说明
            <input aria-label="证书/资质" name="certificateNotes" placeholder="英语四级、资格证" />
          </label>
          <label>
            优先级
            <input name="priority" type="number" min="0" step="1" defaultValue="0" />
          </label>
        </div>
        <div className="check-grid">
          <label className="check-label">
            <input type="checkbox" name="requiresLogin" />
            需要登录
          </label>
          <label className="check-label">
            <input type="checkbox" name="requiresCaptcha" />
            需要验证码
          </label>
        </div>
        <label>
          处理说明
          <textarea name="notes" rows={4} placeholder="记录阻塞原因、账号状态或处理结果" />
        </label>
        <div className="form-actions">
          <button type="submit">保存处理记录</button>
          <button type="reset" className="secondary-button">清空</button>
        </div>
      </form>
    </section>
  );
}

function readText(data: FormData, key: string): string {
  const value = data.get(key);
  return typeof value === "string" ? value.trim() : "";
}

function optionalText(data: FormData, key: string): string | undefined {
  const value = readText(data, key);
  return value || undefined;
}

function splitList(value: string): string[] {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}
