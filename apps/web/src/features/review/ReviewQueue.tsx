import type { ReviewItem } from "../../api/client";

type ReviewQueueProps = {
  items: ReviewItem[];
};

export function ReviewQueue({ items }: ReviewQueueProps) {
  return (
    <section className="panel" id="review" aria-labelledby="review-title">
      <div className="section-heading">
        <h2 id="review-title">复核队列</h2>
        <span>{items.length} 条待复核</span>
      </div>
      {items.length === 0 ? (
        <p className="empty-state">当前没有待复核岗位。</p>
      ) : (
        <ul className="stack-list">
          {items.map((item) => (
            <li key={item.id}>
              <div>
                <strong>{item.company} · {item.title}</strong>
                <span>{item.reason}</span>
              </div>
              <span className="priority">{item.priority}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
