import type { ReactNode } from "react";

type PageHeaderProps = {
  actions?: ReactNode;
  description: string;
  title: string;
};

export function PageHeader({ actions, description, title }: PageHeaderProps) {
  return (
    <header className="topbar">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {actions ? (
        <div className="toolbar" aria-label={`${title}操作`}>
          {actions}
        </div>
      ) : null}
    </header>
  );
}
