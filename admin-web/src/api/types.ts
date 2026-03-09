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
  vehicleType: string
  recoveryRatio: number
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
