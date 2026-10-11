import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { BrowserPendingImport } from "./BrowserPendingImport";
afterEach(() => vi.restoreAllMocks());
it("rejects malformed worker envelopes without a mutation", async () => {
  const fetcher=vi.spyOn(globalThis,"fetch").mockImplementation(async()=>new Response(JSON.stringify({items:[]})));
  render(<BrowserPendingImport />);
  await waitFor(()=>expect(fetcher).toHaveBeenCalledTimes(2));
  fireEvent.change(screen.getByLabelText("工作进程结果 JSON"),{target:{value:'{"rows":[]}'}});
  fireEvent.click(screen.getByRole("button",{name:"导入待审核结果"}));
  expect(await screen.findByRole("alert")).toHaveTextContent("任务、工作进程、租约和行证据");
  expect(fetcher.mock.calls.every(([,init])=>init?.method!=="POST")).toBe(true);
});
