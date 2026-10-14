import { AppError, type ErrorDetail } from '../../../shared/errors.js';

export const CATEGORIES = [
  'REHABILITASI_HUTAN_LAHAN',
  'MANGROVE_PESISIR',
  'PENGELOLAAN_SAMPAH',
  'ENERGI_TERBARUKAN',
  'PEMBERDAYAAN_MASYARAKAT',
  'LAINNYA',
] as const;
export type Category = (typeof CATEGORIES)[number];

export const ATTACHMENT_KINDS = ['PHOTO', 'IMAGE', 'DOCUMENT', 'FILE'] as const;
export type AttachmentKind = (typeof ATTACHMENT_KINDS)[number];

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export const MIME_TYPES_BY_KIND: Record<AttachmentKind, readonly string[]> = {
  PHOTO: IMAGE_TYPES,
  IMAGE: IMAGE_TYPES,
  DOCUMENT: [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ],
  FILE: ['text/plain', 'text/csv'],
};

export const ALLOWED_MIME_TYPES = [...new Set(Object.values(MIME_TYPES_BY_KIND).flat())];

export const REPORT_LIMITS = {
  maxAttachments: 10,
  maxTotalBytes: 30 * 1024 * 1024,
  maxActivityAgeDays: 90,
};

export const REPORT_STATUSES = ['UPLOADING', 'SUBMITTED', 'VERIFIED', 'REJECTED'] as const;
export type ReportStatus = (typeof REPORT_STATUSES)[number];
export type AttachmentState = 'EXPECTED' | 'UPLOADED';

export interface Location {
  latitude: number;
  longitude: number;
  accuracyM: number;
  capturedAt: Date;
  isMock: boolean;
  address: string | null;
}

export interface ManifestItem {
  id: string;
  kind: AttachmentKind;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  sha256: string;
}

export interface ReportContent {
  title: string;
  description: string;
  category: Category;
  activityDate: string;
  location: Location;
  clientCreatedAt: Date;
  attachments: ManifestItem[];
}

export interface AttachmentRecord extends ManifestItem {
  state: AttachmentState;
  storageKey: string;
}

export interface ReportRecord extends ReportContent {
  id: string;
  userId: string;
  status: ReportStatus;
  reviewNote: string | null;
  version: number;
  createdAt: Date;
  updatedAt: Date;
  attachments: AttachmentRecord[];
}

export class IdConflictError extends Error {
  constructor(readonly target: 'report' | 'attachment') {
    super(`${target} id already exists`);
    this.name = 'IdConflictError';
  }
}

export function storageKeyFor(reportId: string, attachmentId: string): string {
  return `reports/${reportId}/${attachmentId}`;
}

export function jakartaDate(at: Date): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Jakarta' }).format(at);
}

export function validateContent(
  content: ReportContent,
  context: { today: string; maxFileBytes: number },
): ErrorDetail[] {
  const details: ErrorDetail[] = [];

  const earliest = addDays(context.today, -REPORT_LIMITS.maxActivityAgeDays);
  if (content.activityDate > context.today) {
    details.push({ field: 'activity_date', issue: 'future', message: 'Tidak boleh melebihi hari ini' });
  } else if (content.activityDate < earliest) {
    details.push({ field: 'activity_date', issue: 'too_old', message: 'Tidak boleh lebih dari 90 hari ke belakang' });
  }

  const items = content.attachments;
  if (items.length > REPORT_LIMITS.maxAttachments) {
    details.push({ field: 'attachments', issue: 'max', message: 'Maksimal 10 lampiran' });
  }
  if (!items.some((a) => a.kind === 'PHOTO' || a.kind === 'IMAGE')) {
    details.push({ field: 'attachments', issue: 'photo_required', message: 'Minimal satu foto atau gambar' });
  }
  if (new Set(items.map((a) => a.id)).size !== items.length) {
    details.push({ field: 'attachments', issue: 'duplicate_id', message: 'ID lampiran tidak boleh kembar' });
  }
  const total = items.reduce((sum, a) => sum + a.sizeBytes, 0);
  if (total > REPORT_LIMITS.maxTotalBytes) {
    details.push({ field: 'attachments', issue: 'total_size', message: 'Total ukuran lampiran maksimal 30 MB' });
  }
  items.forEach((item, i) => {
    if (!MIME_TYPES_BY_KIND[item.kind].includes(item.mimeType)) {
      details.push({
        field: `attachments.${i}.mime_type`,
        issue: 'kind_mismatch',
        message: 'Tipe berkas tidak sesuai jenis lampiran',
      });
    }
    if (item.sizeBytes > context.maxFileBytes) {
      details.push({ field: `attachments.${i}.size_bytes`, issue: 'max', message: `Ukuran berkas maksimal ${context.maxFileBytes / 1_048_576} MB` });
    }
  });

  return details;
}

export function sameContent(a: ReportContent, b: ReportContent): boolean {
  return (
    a.title === b.title &&
    a.description === b.description &&
    a.category === b.category &&
    a.activityDate === b.activityDate &&
    a.clientCreatedAt.getTime() === b.clientCreatedAt.getTime() &&
    a.location.latitude === b.location.latitude &&
    a.location.longitude === b.location.longitude &&
    a.location.accuracyM === b.location.accuracyM &&
    a.location.capturedAt.getTime() === b.location.capturedAt.getTime() &&
    a.location.isMock === b.location.isMock &&
    a.location.address === b.location.address &&
    sameManifest(a.attachments, b.attachments)
  );
}

function sameManifest(a: ManifestItem[], b: ManifestItem[]): boolean {
  if (a.length !== b.length) return false;
  const byId = new Map(b.map((item) => [item.id, item]));
  return a.every((item) => {
    const other = byId.get(item.id);
    return (
      other !== undefined &&
      other.kind === item.kind &&
      other.fileName === item.fileName &&
      other.mimeType === item.mimeType &&
      other.sizeBytes === item.sizeBytes &&
      other.sha256 === item.sha256
    );
  });
}

export type UpsertDecision = 'create' | 'unchanged' | 'resubmit';

export function decideUpsert(existing: ReportRecord | null, actorId: string, incoming: ReportContent): UpsertDecision {
  if (!existing) return 'create';
  if (existing.userId !== actorId) throw new AppError('REPORT_NOT_FOUND', 'Laporan tidak ditemukan');
  if (sameContent(existing, incoming)) return 'unchanged';
  if (existing.status === 'REJECTED') return 'resubmit';
  throw new AppError('REPORT_LOCKED', 'Laporan sudah dikirim dan tidak dapat diubah');
}

function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}
