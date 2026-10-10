import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { emptyJobFilters, exportHref, listJobs, readJobFilters, writeJobFilters } from "../../api/client";
import { JobFilters } from "./JobFilters";

afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/"); });

it("round trips the exact department, title and cohort filters through the URL", () => {
  const filters = readJobFilters("?company=Acme&businessUnit=Platform&title=Engineer&recruitmentYear=2027");
  expect(filters).toMatchObject({ company: "Acme", businessUnit: "Platform", title: "Engineer", recruitmentYear: "2027" });
  writeJobFilters(filters);
  expect(readJobFilters()).toEqual(filters);
});

it("offers independently editable exact filter controls", () => {
  const onChange = vi.fn();
  render(<JobFilters jobs={[]} filters={emptyJobFilters} onChange={onChange} />);
  fireEvent.change(screen.getByLabelText("部门"), { target: { value: "Platform" } });
  expect(onChange).toHaveBeenLastCalledWith({ ...emptyJobFilters, businessUnit: "Platform" });
  fireEvent.change(screen.getByLabelText("岗位名称"), { target: { value: "Engineer" } });
  expect(onChange).toHaveBeenLastCalledWith({ ...emptyJobFilters, title: "Engineer" });
  fireEvent.change(screen.getByLabelText("毕业届别"), { target: { value: "2027" } });
  expect(onChange).toHaveBeenLastCalledWith({ ...emptyJobFilters, recruitmentYear: "2027" });
});

it("sends identical filters to listing and both exports and omits blank values", async () => {
  const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items: [], total: 0 }) });
  vi.stubGlobal("fetch", fetchMock);
  const filters = { ...emptyJobFilters, company: "Acme", businessUnit: "Platform", title: "Engineer", recruitmentYear: "2027" };
  await listJobs(filters, new AbortController().signal);
  const listing = new URL(fetchMock.mock.calls[0][0] as string, "http://localhost");
  for (const format of ["md", "xlsx"] as const) {
    const exporting = new URL(exportHref(format, filters), "http://localhost");
    for (const key of ["company", "businessUnit", "title", "recruitmentYear"]) expect(exporting.searchParams.get(key)).toEqual(listing.searchParams.get(key));
  }
  const blank = { ...filters, businessUnit: "  ", title: "", recruitmentYear: "  " };
  await listJobs(blank, new AbortController().signal);
  const second = new URL(fetchMock.mock.calls[1][0] as string, "http://localhost");
  expect(second.searchParams.has("businessUnit")).toBe(false);
  expect(second.searchParams.has("recruitmentYear")).toBe(false);
  expect(exportHref("md", blank)).not.toContain("businessUnit");
  writeJobFilters(blank);
  expect(window.location.search).not.toContain("recruitmentYear");
});
