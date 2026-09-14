import type { User } from '@/types/user'


export function isStaffUser(user: User | undefined) {
  return Boolean(user?.roles.some((role) => role === 'STAFF' || role === 'ADMIN'))
}
