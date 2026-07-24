import { useEffect, useState } from "react";
import { ApiError, getReviewItem, listReviewItems, reviewAction } from "../api/client";
import type { EntityId, ReviewDetail, ReviewItem } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { ReviewEvidence } from "../features/review/ReviewEvidence";
import { ReviewQueue } from "../features/review/ReviewQueue";
export function OpsReviewPage() {
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [selected, setSelected] = useState<ReviewDetail>();
  const [error, setError] = useState<string>();
  useEffect(() => {
    const controller = new AbortController();
    listReviewItems(controller.signal).then(setItems).catch((reason: unknown) => {
      if (!controller.signal.aborted) setError(message(reason, "无法加载复核队列。"));
    });
    return () => controller.abort();
  }, []);
  const select = (id: EntityId) => {
    getReviewItem(id).then(setSelected)
      .catch((reason: unknown) => setError(message(reason, "无法加载候选证据。")));
  };
  const act = async (id: EntityId, action: "approve" | "reject" | "block") => {
    try {
      const body = action === "block" ? { reason: "manual_follow_up" }
        : action === "reject" ? { reason: "operator_rejected" } : {};
      await reviewAction(id, action, body);
      setItems((current) => current.filter((item) => item.id !== id));
      if (selected?.id === id) setSelected(undefined);
    } catch (reason) { setError(message(reason, "复核操作失败。")); }
  };
  return <main className="workbench"><PageHeader title="岗位复核" description="核对来源、字段和岗位变更后再发布。" />
    {error ? <section className="panel state-panel" role="alert">{error}</section> : null}
    <div className="ops-grid"><ReviewQueue items={items} onAction={act} onSelect={select} />
      <ReviewEvidence item={selected} /></div></main>;
}
function message(reason: unknown, fallback: string): string {
  return reason instanceof ApiError ? reason.message : fallback;
}
