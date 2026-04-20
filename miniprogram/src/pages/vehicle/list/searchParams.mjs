const normalizeNumber = (value, fallback) => {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
};

export function buildVehicleSearchRequest(params = {}) {
  const data = {};
  const keyword = typeof params.q === 'string' ? params.q.trim() : '';
  const brand = typeof params.brand === 'string' ? params.brand.trim() : '';

  if (keyword) data.q = keyword;
  if (brand) data.brands = brand;
  if (params.hasDismantleRecord) data.hasDismantleRecord = true;

  data.page = normalizeNumber(params.page, 0);
  data.size = normalizeNumber(params.size, 20);

  if (typeof params.sort === 'string' && params.sort.trim()) {
    data.sort = params.sort.trim();
  }

  return {
    url: '/vehicles',
    data
  };
}
