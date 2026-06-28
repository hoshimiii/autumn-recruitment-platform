import type { SqliteDatabase } from "../connection.js";

export interface CreateJobInput {
  companyName: string;
  title: string;
  city?: string;
  jobType: string;
  techStack?: string[];
  certificateRequirements?: string[];
  status?: string;
}

export interface JobRecord {
  id: number;
  companyName: string;
  title: string;
  city: string | null;
  jobType: string;
  techStack: string[];
  certificateRequirements: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface JobRow {
  id: number;
  company_name: string;
  title: string;
  city: string | null;
  job_type: string;
  tech_stack: string;
  certificate_requirements: string;
  status: string;
  created_at: string;
  updated_at: string;
}

export function createJobRepository(db: SqliteDatabase) {
  const insert = db.prepare(`
    INSERT INTO jobs (
      company_name,
      title,
      city,
      job_type,
      tech_stack,
      certificate_requirements,
      status
    )
    VALUES (
      @companyName,
      @title,
      @city,
      @jobType,
      @techStack,
      @certificateRequirements,
      @status
    )
  `);

  const findById = db.prepare("SELECT * FROM jobs WHERE id = ?");
  const listAll = db.prepare("SELECT * FROM jobs ORDER BY created_at DESC, id DESC");
  const listByKeyword = db.prepare(`
    SELECT *
    FROM jobs
    WHERE company_name LIKE @keyword OR title LIKE @keyword OR city LIKE @keyword
    ORDER BY created_at DESC, id DESC
  `);

  return {
    create(input: CreateJobInput): JobRecord {
      const result = insert.run({
        companyName: input.companyName,
        title: input.title,
        city: input.city ?? null,
        jobType: input.jobType,
        techStack: JSON.stringify(input.techStack ?? []),
        certificateRequirements: JSON.stringify(input.certificateRequirements ?? []),
        status: input.status ?? "new"
      });

      const row = findById.get(result.lastInsertRowid) as JobRow;
      return toJobRecord(row);
    },

    list(filters: { keyword?: string } = {}): JobRecord[] {
      const keyword = filters.keyword?.trim();
      const rows = keyword
        ? (listByKeyword.all({ keyword: `%${keyword}%` }) as JobRow[])
        : (listAll.all() as JobRow[]);

      return rows.map(toJobRecord);
    }
  };
}

function toJobRecord(row: JobRow): JobRecord {
  return {
    id: row.id,
    companyName: row.company_name,
    title: row.title,
    city: row.city,
    jobType: row.job_type,
    techStack: parseStringArray(row.tech_stack),
    certificateRequirements: parseStringArray(row.certificate_requirements),
    status: row.status,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function parseStringArray(value: string): string[] {
  const parsed: unknown = JSON.parse(value);
  return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
}
