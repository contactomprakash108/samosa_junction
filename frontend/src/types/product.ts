
export type SpiceLevel = 'MILD' | 'MEDIUM' | 'HOT'

export type Category = {
  code: string
  name: string
}

export type Product = {
  id: string
  name: string
  description: string
  category: string
  price: number
  ingredients: string[]
  calories: number
  protein: number
  spiceLevel: SpiceLevel
  dietaryTags: string[]
  available: boolean
  stock: number
  createdAt: string
  updatedAt: string
  imageUrl: string | null
}

export type ProductListQuery = {
  search?: string
  category?: string
  spiceLevel?: SpiceLevel
  available?: boolean
  dietaryTag?: string
  page?: number
  size?: number
  sort?: string
}

export type ProductWrite = {
  name: string
  description: string
  category: string
  price: number
  ingredients: string[]
  calories: number
  protein: number
  spiceLevel: SpiceLevel
  dietaryTags: string[]
  available: boolean
}

export type RecommendationItem = {
  product: Product
  score: number
  reason: string
}

export type RecommendationResponse = {
  coldStart: boolean
  tasteTokens: string[]
  items: RecommendationItem[]
}
