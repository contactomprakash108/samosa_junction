
export type ComplaintCategory = 'DAMAGED' | 'MISSING_ITEM' | 'WRONG_ITEM' | 'LATE' | 'QUALITY' | 'OTHER'
export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type ComplaintImage = {
  id: string
  fileName: string
  contentType: string
  fileSize: number
  url: string
  uploadedAt: string
}

export type Complaint = {
  id: string
  userId: string
  orderId: string
  category: ComplaintCategory
  description: string
  status: ComplaintStatus
  priority: ComplaintPriority
  createdAt: string
  updatedAt: string
  images: ComplaintImage[]
}

export const COMPLAINT_CATEGORIES: ComplaintCategory[] = [
  'DAMAGED',
  'MISSING_ITEM',
  'WRONG_ITEM',
  'LATE',
  'QUALITY',
  'OTHER',
]
