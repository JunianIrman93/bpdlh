import type { ReportContent, ReportRecord, ReportStatus } from '../domain/report.js';

export type { ReportRecord, ReportStatus } from '../domain/report.js';

export interface ReportListFilter {
  ownerId: string | null;
  status?: ReportStatus;
  updatedSince?: Date;
  after?: { updatedAt: Date; id: string };
  limit: number;
}

export interface ReportRepo {
  findById(id: string): Promise<ReportRecord | null>;

  list(filter: ReportListFilter): Promise<ReportRecord[]>;

  create(input: { id: string; userId: string; content: ReportContent }): Promise<ReportRecord>;

  resubmit(input: {
    id: string;
    expectedVersion: number;
    content: ReportContent;
  }): Promise<{ report: ReportRecord; removedStorageKeys: string[] } | null>;

  markAttachmentUploaded(attachmentId: string, at: Date): Promise<void>;

  submitIfComplete(reportId: string): Promise<ReportStatus>;

  review(input: {
    id: string;
    status: 'VERIFIED' | 'REJECTED';
    note: string | null;
    reviewerId: string;
    at: Date;
  }): Promise<ReportRecord | null>;
}

export interface InspectedFile {
  head: Buffer;
  tail: Buffer;
  sha256: string;
}

export type InspectFile = (path: string) => Promise<InspectedFile>;
