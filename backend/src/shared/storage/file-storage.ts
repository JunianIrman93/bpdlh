import type { Readable } from 'node:stream';

export interface FileStorage {
  put(key: string, sourcePath: string, contentType: string): Promise<void>;
  get(key: string): Promise<Readable>;
  delete(key: string): Promise<void>;
}
