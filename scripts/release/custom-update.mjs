import { statSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export function createUpdate({ repository, versionCode, versionName, apkName, fileSize, notes }) {
  if (repository !== 'Kimger/gkd') throw new Error('Unexpected release repository');
  if (!Number.isSafeInteger(versionCode) || versionCode < 1 || versionCode > 2100000000) {
    throw new Error('Invalid version code');
  }
  if (!/^custom-\d+$/.test(versionName) || versionName !== `custom-${versionCode}`) {
    throw new Error('Version name/code mismatch');
  }
  if (apkName !== `gkd-${versionName}.apk`) throw new Error('Unexpected APK name');
  if (!Number.isSafeInteger(fileSize) || fileSize <= 0) throw new Error('Empty APK');
  if (typeof notes !== 'string' || !notes.trim()) throw new Error('Missing release notes');
  return {
    versionCode, versionName,
    downloadUrl: `https://github.com/${repository}/releases/download/custom-${versionCode}/${apkName}`,
    fileSize,
    versionLogs: [{ name: versionName, code: versionCode, desc: notes }],
  };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const directory = '.local/custom-release';
  const apkName = process.env.RELEASE_APK;
  const update = createUpdate({
    repository: process.env.GITHUB_REPOSITORY,
    versionCode: Number(process.env.GKD_VERSION_CODE),
    versionName: process.env.GKD_VERSION_NAME,
    apkName,
    fileSize: statSync(resolve(directory, apkName)).size,
    notes: process.env.RELEASE_NOTES,
  });
  writeFileSync(resolve(directory, 'update.json'), JSON.stringify(update, null, 2) + '\n');
  writeFileSync(resolve(directory, 'notes.md'), `${update.versionLogs[0].desc}\n\nSource: ${process.env.RELEASE_COMMIT}\n`);
}
