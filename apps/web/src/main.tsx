import { mount as mountRecruitmentApp, shouldAutoMount, unmount as unmountRecruitmentApp } from "./microApp/lifecycle";
import "./styles/app.css";

if (shouldAutoMount(window)) {
  mountRecruitmentApp();
}

export const mount = mountRecruitmentApp;
export const unmount = unmountRecruitmentApp;
