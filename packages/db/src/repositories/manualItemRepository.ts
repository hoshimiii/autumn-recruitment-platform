import type { SqliteDatabase } from "../connection.js";

export interface CreateManualItemInput {
  platform: string;
  companyName: string;
  jobTitle: string;
  searchKeywords?: string[];
  sourceUrl?: string;
  techStackKeywords?: string[];
  certificateNotes?: string;
  requiredAction: string;
  requiresLogin?: boolean;
  requiresCaptcha?: boolean;
  applyMethod?: string;
  reminderAt?: string;
  priority?: number;
  status?: string;
  linkedJobId?: number;
  notes?: string;
}

export interface ManualItemRecord {
  id: number;
  platform: string;
  companyName: string;
  jobTitle: string;
  searchKeywords: string[];
  sourceUrl: string | null;
  techStackKeywords: string[];
  certificateNotes: string | null;
  requiredAction: string;
  requiresLogin: boolean;
  requiresCaptcha: boolean;
  applyMethod: string | null;
  reminderAt: string | null;
  priority: number;
  status: string;
  linkedJobId: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

interface ManualItemRow {
  id: number;
  platform: string;
  company_name: string;
  job_title: string;
  search_keywords: string;
  source_url: string | null;
  tech_stack_keywords: string;
  certificate_notes: string | null;
  required_action: string;
  requires_login: 0 | 1;
  requires_captcha: 0 | 1;
  apply_method: string | null;
  reminder_at: string | null;
  priority: number;
  status: string;
  linked_job_id: number | null;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export function createManualItemRepository(db: SqliteDatabase) {
  const insert = db.prepare(`
    INSERT INTO manual_items (
      platform,
      company_name,
      job_title,
      search_keywords,
      source_url,
      tech_stack_keywords,
      certificate_notes,
      required_action,
      requires_login,
      requires_captcha,
      apply_method,
      reminder_at,
      priority,
      status,
      linked_job_id,
      notes
    )
    VALUES (
      @platform,
      @companyName,
      @jobTitle,
      @searchKeywords,
      @sourceUrl,
      @techStackKeywords,
      @certificateNotes,
      @requiredAction,
      @requiresLogin,
      @requiresCaptcha,
      @applyMethod,
      @reminderAt,
      @priority,
      @status,
      @linkedJobId,
      @notes
    )
  `);

  const findById = db.prepare("SELECT * FROM manual_items WHERE id = ?");
  const listAll = db.prepare("SELECT * FROM manual_items ORDER BY created_at DESC, id DESC");

  return {
    create(input: CreateManualItemInput): ManualItemRecord {
      const result = insert.run({
        platform: input.platform,
        companyName: input.companyName,
        jobTitle: input.jobTitle,
        searchKeywords: JSON.stringify(input.searchKeywords ?? []),
        sourceUrl: input.sourceUrl ?? null,
        techStackKeywords: JSON.stringify(input.techStackKeywords ?? []),
        certificateNotes: input.certificateNotes ?? null,
        requiredAction: input.requiredAction,
        requiresLogin: input.requiresLogin ? 1 : 0,
        requiresCaptcha: input.requiresCaptcha ? 1 : 0,
        applyMethod: input.applyMethod ?? null,
        reminderAt: input.reminderAt ?? null,
        priority: input.priority ?? 0,
        status: input.status ?? "pending",
        linkedJobId: input.linkedJobId ?? null,
        notes: input.notes ?? null
      });

      const row = findById.get(result.lastInsertRowid) as ManualItemRow;
      return toManualItemRecord(row);
    },

    list(): ManualItemRecord[] {
      return (listAll.all() as ManualItemRow[]).map(toManualItemRecord);
    }
  };
}

function toManualItemRecord(row: ManualItemRow): ManualItemRecord {
  return {
    id: row.id,
    platform: row.platform,
    companyName: row.company_name,
    jobTitle: row.job_title,
    searchKeywords: parseStringArray(row.search_keywords),
    sourceUrl: row.source_url,
    techStackKeywords: parseStringArray(row.tech_stack_keywords),
    certificateNotes: row.certificate_notes,
    requiredAction: row.required_action,
    requiresLogin: Boolean(row.requires_login),
    requiresCaptcha: Boolean(row.requires_captcha),
    applyMethod: row.apply_method,
    reminderAt: row.reminder_at,
    priority: row.priority,
    status: row.status,
    linkedJobId: row.linked_job_id,
    notes: row.notes,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function parseStringArray(value: string): string[] {
  const parsed: unknown = JSON.parse(value);
  return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
}
