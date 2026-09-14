import { apiClient } from '@/api/client'
import type { UpdateProfileRequest, User } from '@/types/user'


export const userApi = {
  me() {
    return apiClient.get<User>('/api/users/me').then((response) => response.data)
  },
  update(body: UpdateProfileRequest) {
    return apiClient.patch<User>('/api/users/me', body).then((response) => response.data)
  },
}
