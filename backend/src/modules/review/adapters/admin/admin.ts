import AdminJSExpress from '@adminjs/express';
import { Database, getModelByName, Resource } from '@adminjs/prisma';
import type { PrismaClient } from '@prisma/client';
import AdminJS, { type ActionContext, type ActionRequest, type CurrentAdmin, type ResourceOptions } from 'adminjs';
import type { Router } from 'express';
import session from 'express-session';
import { AppError } from '../../../../shared/errors.js';
import type { Role } from '../../../../shared/principal.js';
import type { ReviewReport } from '../../application/review-report.js';
import type { Decision } from '../../domain/review.js';
import { Components, componentLoader } from './components.js';

AdminJS.registerAdapter({ Database, Resource });

export const ADMIN_ROOT = '/admin';

export type AuthenticateReviewer = (
  email: string,
  password: string,
) => Promise<{ id: string; email: string; role: Role } | null>;

const readOnly: ResourceOptions['actions'] = {
  new: { isAccessible: false },
  edit: { isAccessible: false },
  delete: { isAccessible: false },
  bulkDelete: { isAccessible: false },
};

export function createAdmin(deps: {
  prisma: PrismaClient;
  reviewReport: ReviewReport;
  authenticateReviewer: AuthenticateReviewer;
  cookieSecret: string;
}): { admin: AdminJS; router: Router } {
  const model = (name: string) => ({ model: getModelByName(name), client: deps.prisma });

  const admin = new AdminJS({
    rootPath: ADMIN_ROOT,
    componentLoader,
    branding: { companyName: 'LaporLH', withMadeWithLove: false },
    resources: [
      {
        resource: model('Report'),
        options: {
          navigation: { name: 'Laporan', icon: 'FileText' },
          sort: { sortBy: 'updatedAt', direction: 'desc' },
          listProperties: ['title', 'category', 'status', 'activityDate', 'user', 'updatedAt'],
          filterProperties: ['status', 'category', 'activityDate', 'user'],
          showProperties: [
            'id',
            'title',
            'description',
            'category',
            'activityDate',
            'status',
            'reviewNote',
            'reviewedBy',
            'reviewedAt',
            'latitude',
            'longitude',
            'accuracyM',
            'locationCapturedAt',
            'isMockLocation',
            'version',
            'user',
            'clientCreatedAt',
            'createdAt',
            'updatedAt',
          ],
          actions: {
            ...readOnly,
            verify: reviewAction('VERIFIED', 'Verifikasi', 'Check'),
            reject: reviewAction('REJECTED', 'Tolak', 'X'),
          },
        },
      },
      {
        resource: model('Attachment'),
        options: {
          navigation: { name: 'Laporan', icon: 'FileText' },
          listProperties: ['fileName', 'kind', 'mimeType', 'sizeBytes', 'state', 'report'],
          filterProperties: ['report', 'kind', 'state'],
          properties: { storageKey: { isVisible: false } },
          actions: readOnly,
        },
      },
      {
        resource: model('User'),
        options: {
          navigation: { name: 'Pengguna', icon: 'User' },
          listProperties: ['fullName', 'email', 'role', 'createdAt'],
          properties: { passwordHash: { isVisible: false } },
          actions: readOnly,
        },
      },
    ],
  });

  function reviewAction(decision: Decision, label: string, icon: string) {
    return {
      actionType: 'record' as const,
      icon,
      label,
      component: Components.ReviewAction,
      isVisible: ({ record }: ActionContext) => record?.params.status === 'SUBMITTED',
      isAccessible: ({ currentAdmin }: ActionContext) => currentAdmin?.role === 'REVIEWER',
      handler: async (request: ActionRequest, _response: unknown, context: ActionContext) => {
        const { record, currentAdmin, h, resource } = context;
        if (!record || !currentAdmin) throw new AppError('NOT_FOUND', 'Laporan tidak ditemukan');
        if (request.method === 'get') return { record: record.toJSON(currentAdmin) };

        const note = typeof request.payload?.note === 'string' ? request.payload.note : null;
        try {
          await deps.reviewReport({
            actor: { userId: String(currentAdmin.id), role: currentAdmin.role as Role },
            reportId: String(record.id()),
            decision,
            note,
          });
        } catch (err) {
          if (!(err instanceof AppError)) throw err;
          return {
            record: record.toJSON(currentAdmin),
            notice: { message: err.details?.[0]?.message ?? err.message, type: 'error' as const },
          };
        }
        const fresh = await resource.findOne(String(record.id()));
        return {
          record: (fresh ?? record).toJSON(currentAdmin),
          redirectUrl: h.recordActionUrl({
            resourceId: resource._decorated?.id() ?? resource.id(),
            recordId: String(record.id()),
            actionName: 'show',
          }),
          notice: {
            message: decision === 'VERIFIED' ? 'Laporan diverifikasi' : 'Laporan ditolak',
            type: 'success' as const,
          },
        };
      },
    };
  }

  const router = AdminJSExpress.buildAuthenticatedRouter(
    admin,
    {
      authenticate: async (email: string, password: string): Promise<CurrentAdmin | null> =>
        deps.authenticateReviewer(email, password),
      cookieName: 'laporlh_admin',
      cookiePassword: deps.cookieSecret,
    },
    null,
    {
      secret: deps.cookieSecret,
      store: new session.MemoryStore(),
      resave: false,
      saveUninitialized: false,
      cookie: { httpOnly: true, sameSite: 'lax', secure: 'auto' },
    },
  );

  return { admin, router };
}
