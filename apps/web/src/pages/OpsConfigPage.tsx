import type { Source } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { SourceList } from "../features/sources/SourceList";

type OpsConfigPageProps = {
  sources: Source[];
};

export function OpsConfigPage({ sources }: OpsConfigPageProps) {
  return (
    <main className="workbench">
      <PageHeader
        title="来源配置"
        description="维护可扩展的信息源清单，后续外企、招聘平台和企业官网按同一结构接入。"
      />

      <div className="ops-grid single-column">
        <SourceList sources={sources} title="来源清单" />
      </div>
    </main>
  );
}
