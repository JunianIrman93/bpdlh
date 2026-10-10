type Detected =
  | 'image/jpeg'
  | 'image/png'
  | 'image/webp'
  | 'application/pdf'
  | 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  | 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  | 'text';

export const SIGNATURE_HEAD_BYTES = 4096;
export const SIGNATURE_TAIL_BYTES = 65536;

export function detectContentType(head: Buffer, tail: Buffer): Detected | null {
  if (startsWith(head, [0xff, 0xd8, 0xff])) return 'image/jpeg';
  if (startsWith(head, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) return 'image/png';
  if (ascii(head, 0, 4) === 'RIFF' && ascii(head, 8, 12) === 'WEBP') return 'image/webp';
  if (ascii(head, 0, 5) === '%PDF-') return 'application/pdf';
  if (startsWith(head, [0x50, 0x4b, 0x03, 0x04])) {
    const names = tail.toString('latin1');
    if (names.includes('word/document.xml')) {
      return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    }
    if (names.includes('xl/workbook.xml')) {
      return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    }
    return null;
  }
  return isUtf8Text(head) ? 'text' : null;
}

export function contentMatches(declaredMime: string, head: Buffer, tail: Buffer): boolean {
  const detected = detectContentType(head, tail);
  if (detected === 'text') return declaredMime === 'text/plain' || declaredMime === 'text/csv';
  return detected === declaredMime;
}

function isUtf8Text(head: Buffer): boolean {
  if (head.length === 0 || head.includes(0)) return false;
  try {
    new TextDecoder('utf-8', { fatal: true }).decode(head, { stream: true });
    return true;
  } catch {
    return false;
  }
}

function startsWith(buf: Buffer, bytes: number[]): boolean {
  return buf.length >= bytes.length && bytes.every((b, i) => buf[i] === b);
}

function ascii(buf: Buffer, start: number, end: number): string {
  return buf.subarray(start, end).toString('latin1');
}
