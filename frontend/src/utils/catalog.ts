import type { Product } from '@/types/product'


const PRODUCT_PHOTOS: Record<string, string> = {
  'Baked Samosa': '/images/products/baked-samosa.png',
  'Chole Samosa': '/images/products/chole-samosa.png',
  'Classic Samosa': '/images/products/classic-samosa.png',
  'Corn Samosa': '/images/products/corn-samosa.png',
  'High Protein Samosa': '/images/products/high-protein-samosa.png',
  'Millet Samosa': '/images/products/millet-samosa.png',
  'Paneer Samosa': '/images/products/paneer-samosa.png',
}

const CATEGORY_LABELS: Record<string, string> = {
  CLASSIC: 'Classic',
  HEALTHY: 'Healthy',
  PROTEIN: 'High Protein',
  BAKED: 'Baked',
}

export function productPhoto(product: Pick<Product, 'name' | 'imageUrl'>): string | null {
  return PRODUCT_PHOTOS[product.name] ?? product.imageUrl
}

export function categoryLabel(code: string): string {
  return CATEGORY_LABELS[code] ?? code
}

export function spiceLabel(level: string): string {
  return level.toLowerCase()
}

export function proteinLabel(protein: number | string): string {
  const value = typeof protein === 'number' ? protein : Number(protein)
  const rounded = Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, '')
  return `${rounded}g protein`
}
