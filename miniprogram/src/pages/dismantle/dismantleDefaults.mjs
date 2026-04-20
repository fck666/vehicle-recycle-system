export const DEFAULT_RECYCLE_TYPES = ['steel', 'aluminum', 'copper', 'battery', 'plastic', 'rubber'];

export const DEFAULT_PART_NAMES = [
  '三元催化', '发动机', '变速箱', '轮毂', '电机', '空调压缩机', '发电机', '音响', '中控', '座椅',
  '电瓶', '方向盘', '转向机', '水箱', '水箱盖', '风扇', 'ABS', '录音机', '仪表', '雨刷', '暖风电机', '天窗', '油箱'
];

const normalizeTypes = (types) => {
  const normalized = Array.isArray(types)
    ? types
      .filter((item) => typeof item === 'string')
      .map((item) => item.trim())
      .filter(Boolean)
    : [];
  return normalized.length > 0 ? normalized : DEFAULT_RECYCLE_TYPES;
};

export function buildDynamicItems(types, labelMap = {}) {
  return normalizeTypes(types).map((type) => ({
    type,
    label: labelMap[type] || type,
    value: ''
  }));
}

export function buildDynamicColumns(types, labelMap = {}) {
  return normalizeTypes(types).map((type) => ({
    prop: `${type}Weight`,
    label: labelMap[type] || type
  }));
}
