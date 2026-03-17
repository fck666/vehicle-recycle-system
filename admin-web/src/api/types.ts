export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface VehicleModel {
  id: number
  sourceType: 'CRAWLED' | 'MANUAL' | 'EDITED'
  sourceSite?: string | null
  brand: string
  model: string
  modelYear: number
  releaseDate?: string
  fuelType: string
  vehicleType: string
  curbWeight: number
  grossWeight?: number | null
  batteryKwh?: number | null
  productId?: string | null
  productNo?: string | null
  createdAt?: string | null
  images?: VehicleImage[]
  documents?: VehicleDocument[]
}

export interface VehicleImage {
  id: number
  imageUrl: string
  imageName?: string | null
  sortOrder?: number | null
  createdAt?: string | null
}

export interface VehicleDocument {
  id: number
  docType?: string | null
  docName?: string | null
  docUrl: string
  sha256?: string | null
  sourceUrl?: string | null
  fetchedAt?: string | null
  createdAt?: string | null
}

export interface VehicleDismantleRecord {
  id: number
  vehicleId: number
  steelWeight?: number | null
  aluminumWeight?: number | null
  copperWeight?: number | null
  batteryWeight?: number | null
  otherWeight?: number | null
  detailsJson?: string | null
  operatorName?: string | null
  operatorId?: string | null
  imagesJson?: string | null
  remark?: string | null
  createdAt?: string | null
}

export interface VehicleUpsertRequest {
  brand?: string
  model?: string
  modelYear?: number
  fuelType?: string
  vehicleType?: string
  curbWeight?: number
  grossWeight?: number | null
  batteryKwh?: number | null
  productId?: string | null
  productNo?: string | null
}

export interface SameSeriesCandidate {
  vehicleId: number
  brand: string
  model: string
  modelYear: number
  manufacturerName?: string | null
  vehicleType?: string | null
  fuelType?: string | null
  curbWeight?: number | null
  wheelbaseMm?: number | null
  seriesName?: string | null
  score: number
  confidenceLevel: 'HIGH' | 'MEDIUM' | 'LOW'
  matchReasons: string[]
}

export interface SameSeriesResponse {
  targetVehicleId: number
  targetSeriesName: string
  yearWindow: number
  candidateCount: number
  highConfidenceCount: number
  mediumConfidenceCount: number
  candidates: SameSeriesCandidate[]
}

export interface MaterialPrice {
  id: number
  type: string
  pricePerKg: number
  currency?: string | null
  unit?: string | null
  effectiveDate?: string | null
  fetchedAt?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  updateTime?: string | null
}

export interface MaterialTemplate {
  id: number
  vehicleType?: string | null
  scopeType: 'VEHICLE_TYPE' | 'VEHICLE'
  scopeValue: string
  recoveryRatio: number
  othersPricePerKgOverride?: number | null
  materials: MaterialRatioItem[]
  createdAt?: string | null
}

export interface MaterialRatioItem {
  materialType: string
  ratio: number
}

export interface MaterialSourceConfig {
  id: number
  type: string
  displayName: string
  sourceName: string
  sourceUrl: string
  parseKeyword: string
  enabled: boolean
}

export interface MaterialSourceSuggestResult {
  type: string
  displayName: string
  sourceName: string
  sourceUrl: string
  parseKeyword: string
}
