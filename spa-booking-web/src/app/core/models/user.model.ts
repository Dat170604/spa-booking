export interface UserResponse {
  id: number;
  branchId?: number;
  branchName?: string;
  fullName: string;
  email: string;
  phone: string;
  status: string;
  roles: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UserUpdateRequest {
  fullName: string;
  email: string;
  phone: string;
  password?: string;
  branchId?: number;
  status?: string;
}

export interface StaffCreateRequest {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  branchId: number;
}

export interface UpdateUserRolesRequest {
  roles: string[];
}
