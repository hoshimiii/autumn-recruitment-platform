import type { ManualActionInput } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { ManualActionForm } from "../features/manual/ManualActionForm";
import { ManualRecordList } from "../features/manual/ManualRecordList";

type OpsAddPageProps = {
  manualRecords: ManualActionInput[];
  onManualSubmit: (record: ManualActionInput) => void;
};

export function OpsAddPage({ manualRecords, onManualSubmit }: OpsAddPageProps) {
  return (
    <main className="workbench">
      <PageHeader
        title="添加/导入"
        description="把外部渠道筛选后的岗位、登录阻塞和验证码阻塞统一沉淀为结构化记录。"
      />

      <div className="ops-grid">
        <section className="panel" aria-labelledby="import-contract-title">
          <div className="section-heading">
            <h2 id="import-contract-title">导入规范</h2>
            <span>结构化插槽</span>
          </div>
          <ul className="stack-list">
            <li>
              <div>
                <strong>岗位主信息</strong>
                <span>类型、公司、岗位、岗位类型、城市、开始/截止日期。</span>
              </div>
            </li>
            <li>
              <div>
                <strong>筛选信息</strong>
                <span>技术栈、证书/资质、优先级、来源可信度。</span>
              </div>
            </li>
            <li>
              <div>
                <strong>阻塞信息</strong>
                <span>登录、验证码、人工复核、投递方式和来源链接。</span>
              </div>
            </li>
          </ul>
        </section>

        <div className="secondary-column">
          <ManualActionForm onSubmit={onManualSubmit} />
          <ManualRecordList records={manualRecords} />
        </div>
      </div>
    </main>
  );
}
