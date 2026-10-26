import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createUpdate } from './custom-update.mjs';

const input = { repository: 'Kimger/gkd', versionCode: 29800000, versionName: 'custom-29800000',
  apkName: 'gkd-custom-29800000.apk', fileSize: 6000000, notes: '自用更新\n第二行' };

test('release manifest follows the Android updater wire format and pins the APK version', () => {
  assert.deepEqual(createUpdate(input), {
    versionCode: 29800000, versionName: 'custom-29800000',
    downloadUrl: 'https://github.com/Kimger/gkd/releases/download/custom-29800000/gkd-custom-29800000.apk',
    fileSize: 6000000,
    versionLogs: [{ name: 'custom-29800000', code: 29800000, desc: '自用更新\n第二行' }],
  });
});

test('invalid release identity, empty APK and inconsistent versions cannot be published', () => {
  for (const override of [{repository: 'gkd-kit/gkd'}, {versionCode: 0}, {versionCode: NaN},
    {versionName: 'custom-12'}, {apkName: '../other.apk'}, {fileSize: 0}, {notes: ''}]) {
    assert.throws(() => createUpdate({ ...input, ...override }));
  }
});
