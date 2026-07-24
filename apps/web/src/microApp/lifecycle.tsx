import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import type { Root } from "react-dom/client";
import { flushSync } from "react-dom";
import { App } from "../App";
import { isMicroAppEnvironment } from "./environment";
import type { MicroAppGlobals } from "./environment";

let root: Root | null = null;

function getRootElement(): HTMLElement {
  const rootElement = document.getElementById("root");

  if (!rootElement) {
    throw new Error("Root element not found");
  }

  return rootElement;
}

export function shouldAutoMount(target: MicroAppGlobals = window): boolean {
  return !isMicroAppEnvironment(target);
}

export function mount(container = getRootElement()): void {
  if (!root) {
    root = createRoot(container);
  }

  flushSync(() => root?.render(<StrictMode><App /></StrictMode>));
}

export function unmount(): void {
  root?.unmount();
  root = null;
}
