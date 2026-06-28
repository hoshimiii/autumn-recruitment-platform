export function ManualActionForm() {
  return (
    <section className="panel" id="manual" aria-labelledby="manual-title">
      <div className="section-heading">
        <h2 id="manual-title">人工处理</h2>
        <span>登录与验证码</span>
      </div>
      <form className="manual-form">
        <label>
          来源名称
          <input name="sourceName" placeholder="例如：招聘平台 A" />
        </label>
        <label>
          账号备注
          <input name="accountNote" placeholder="账号、负责人或交接说明" />
        </label>
        <div className="check-grid">
          <label className="check-label">
            <input type="checkbox" name="needsLogin" />
            需要登录
          </label>
          <label className="check-label">
            <input type="checkbox" name="needsCaptcha" />
            需要验证码
          </label>
        </div>
        <label>
          处理说明
          <textarea name="notes" rows={4} placeholder="记录验证码、登录状态或阻塞原因" />
        </label>
        <div className="form-actions">
          <button type="button">保存处理记录</button>
          <button type="reset" className="secondary-button">清空</button>
        </div>
      </form>
    </section>
  );
}
