import test from 'node:test';
import assert from 'node:assert/strict';

import { buildDynamicColumns, buildDynamicItems, DEFAULT_PART_NAMES, DEFAULT_RECYCLE_TYPES } from './dismantleDefaults.mjs';

test('buildDynamicItems falls back to default recycle types when api returns empty', () => {
  const items = buildDynamicItems([], {
    steel: '废钢',
    aluminum: '废铝'
  });

  assert.deepEqual(items.map((item) => item.type), DEFAULT_RECYCLE_TYPES);
  assert.equal(items[0].label, '废钢');
  assert.equal(items[1].label, '废铝');
});

test('buildDynamicColumns ignores invalid values and keeps api data when present', () => {
  const columns = buildDynamicColumns([' steel ', '', null, 'battery'], {
    steel: '废钢',
    battery: '电池'
  });

  assert.deepEqual(columns, [
    { prop: 'steelWeight', label: '废钢' },
    { prop: 'batteryWeight', label: '电池' }
  ]);
});

test('default part names contain expected fallback options', () => {
  assert.equal(DEFAULT_PART_NAMES[0], '三元催化');
  assert.ok(DEFAULT_PART_NAMES.includes('电瓶'));
  assert.equal(DEFAULT_PART_NAMES.at(-1), '油箱');
});
