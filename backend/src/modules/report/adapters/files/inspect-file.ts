import { createHash } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { open } from 'node:fs/promises';
import { SIGNATURE_HEAD_BYTES, SIGNATURE_TAIL_BYTES } from '../../domain/file-signature.js';
import type { InspectedFile } from '../../ports/index.js';

export async function inspectFile(path: string): Promise<InspectedFile> {
  const hash = createHash('sha256');
  const headChunks: Buffer[] = [];
  let headLength = 0;
  for await (const chunk of createReadStream(path) as AsyncIterable<Buffer>) {
    hash.update(chunk);
    if (headLength < SIGNATURE_HEAD_BYTES) {
      const part = chunk.subarray(0, SIGNATURE_HEAD_BYTES - headLength);
      headChunks.push(part);
      headLength += part.length;
    }
  }

  const handle = await open(path);
  try {
    const { size } = await handle.stat();
    const length = Math.min(size, SIGNATURE_TAIL_BYTES);
    const tail = Buffer.alloc(length);
    await handle.read(tail, 0, length, size - length);
    return { head: Buffer.concat(headChunks), tail, sha256: hash.digest('hex') };
  } finally {
    await handle.close();
  }
}
