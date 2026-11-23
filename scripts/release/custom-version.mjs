import { appendFileSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Highest versionCode already distributed outside GitHub Releases.
const localReleaseCode = 106;

export function nextVersion(source, previousCode = 0) {
  const baseName = source.match(/\?:\s*\("([^"]+)"\s*\+\s*project\.gitInfo\.versionNameSuffix/)?.[1];
  const baseCode = Number(source.match(/extra\["gkdVersionCode"\][\s\S]*?\}\s*\?:\s*(\d+)/)?.[1]);
  if (!baseName || !Number.isSafeInteger(baseCode) || baseCode < 1) {
    throw new Error('Cannot read upstream version from build.gradle.kts');
  }
  if (!Number.isSafeInteger(previousCode) || previousCode < 0) {
    throw new Error('Invalid previous release version code');
  }
  const versionCode = Math.max(baseCode, previousCode, localReleaseCode) + 1;
  return { versionCode, versionName: `${baseName}-r${versionCode}` };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const previousCode = Number(process.env.PREVIOUS_VERSION_CODE || 0);
  const version = nextVersion(readFileSync('build.gradle.kts', 'utf8'), previousCode);
  appendFileSync(process.env.GITHUB_ENV,
    `GKD_VERSION_CODE=${version.versionCode}\nGKD_VERSION_NAME=${version.versionName}\n`);
}
