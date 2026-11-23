import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createUpdate } from './custom-update.mjs';
import { nextVersion } from './custom-version.mjs';

const input = { repository: 'Kimger/gkd', versionCode: 95, versionName: '1.13.0-beta-2-r95',
  apkName: 'gkd-1.13.0-beta-2-r95.apk', fileSize: 6000000, notes: '自用更新\n第二行' };

test('release manifest follows the Android updater wire format and pins the APK version', () => {
  assert.deepEqual(createUpdate(input), {
    versionCode: 95, versionName: '1.13.0-beta-2-r95',
    downloadUrl: 'https://github.com/Kimger/gkd/releases/download/custom-95/gkd-1.13.0-beta-2-r95.apk',
    fileSize: 6000000,
    versionLogs: [{ name: '1.13.0-beta-2-r95', code: 95, desc: '自用更新\n第二行' }],
  });
});

test('custom versions start after upstream and increase from the last release', () => {
  const source = `extra["gkdVersionName"] = buildProperty("GKD_VERSION_NAME").orNull
    ?: ("1.13.0-beta-2" + project.gitInfo.versionNameSuffix.orEmpty())
extra["gkdVersionCode"] = buildProperty("GKD_VERSION_CODE").orNull?.let {
    requireNotNull(it.toIntOrNull()) { "Invalid version code" }
} ?: 94`;
  assert.deepEqual(nextVersion(source, 105), { versionCode: 107, versionName: '1.13.0-beta-2-r107' });
  assert.deepEqual(nextVersion(source, 107), { versionCode: 108, versionName: '1.13.0-beta-2-r108' });
  assert.deepEqual(nextVersion(source.replace('} ?: 94', '} ?: 110'), 107),
    { versionCode: 111, versionName: '1.13.0-beta-2-r111' });
  assert.throws(() => nextVersion('invalid source'));
  assert.throws(() => nextVersion(source, NaN));
});

test('invalid release identity, empty APK and inconsistent versions cannot be published', () => {
  for (const override of [{repository: 'gkd-kit/gkd'}, {versionCode: 0}, {versionCode: NaN}, {versionCode: 96},
    {versionName: 'custom-12'}, {apkName: '../other.apk'}, {fileSize: 0}, {notes: ''}]) {
    assert.throws(() => createUpdate({ ...input, ...override }));
  }
});
