import { mount as mountRecruitmentApp, shouldAutoMount, unmount as unmountRecruitmentApp } from "./microApp/lifecycle";
import "./styles/app.css";

if (shouldAutoMount(window)) {
  document.body.classList.add("recruitment-standalone");
  mountRecruitmentApp();
}

export const mount = mountRecruitmentApp;
export function unmount(): void {
  unmountRecruitmentApp();
  document.body.classList.remove("recruitment-standalone");
}
