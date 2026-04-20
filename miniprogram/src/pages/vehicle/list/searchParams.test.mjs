import test from 'node:test';
import assert from 'node:assert/strict';

import { buildVehicleSearchRequest } from './searchParams.mjs';

test('buildVehicleSearchRequest uses admin-style filter semantics with request data', () => {
  const request = buildVehicleSearchRequest({
    q: '  Model Y  ',
    brand: 'Tesla',
    hasDismantleRecord: true,
    page: 2,
    size: 20,
    sort: 'id,desc'
  });

  assert.deepEqual(request, {
    url: '/vehicles',
    data: {
      q: 'Model Y',
      brands: 'Tesla',
      hasDismantleRecord: true,
      page: 2,
      size: 20,
      sort: 'id,desc'
    }
  });
});

test('buildVehicleSearchRequest omits empty filters and keeps default paging', () => {
  const request = buildVehicleSearchRequest({
    q: '   ',
    brand: '',
    page: undefined,
    size: undefined
  });

  assert.deepEqual(request, {
    url: '/vehicles',
    data: {
      page: 0,
      size: 20
    }
  });
});
