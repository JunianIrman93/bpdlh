import type { ReportRecord } from '../../domain/report.js';

export function toSummaryDto(report: ReportRecord) {
  return {
    id: report.id,
    title: report.title,
    category: report.category,
    activity_date: report.activityDate,
    status: report.status,
    review_note: report.reviewNote,
    version: report.version,
    attachment_count: report.attachments.length,
    updated_at: report.updatedAt.toISOString(),
  };
}

export function toReceiptDto(report: ReportRecord, serverTime: Date) {
  return {
    id: report.id,
    status: report.status,
    version: report.version,
    attachments: report.attachments.map((a) => ({ id: a.id, state: a.state })),
    created_at: report.createdAt.toISOString(),
    updated_at: report.updatedAt.toISOString(),
    server_time: serverTime.toISOString(),
  };
}

export function toDetailDto(report: ReportRecord) {
  return {
    ...toSummaryDto(report),
    description: report.description,
    location: {
      latitude: report.location.latitude,
      longitude: report.location.longitude,
      accuracy_m: report.location.accuracyM,
      captured_at: report.location.capturedAt.toISOString(),
      is_mock: report.location.isMock,
    },
    client_created_at: report.clientCreatedAt.toISOString(),
    created_at: report.createdAt.toISOString(),
    attachments: report.attachments.map((a) => ({
      id: a.id,
      kind: a.kind,
      file_name: a.fileName,
      mime_type: a.mimeType,
      size_bytes: a.sizeBytes,
      sha256: a.sha256,
      state: a.state,
    })),
  };
}
