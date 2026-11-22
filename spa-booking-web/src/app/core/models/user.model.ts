export interface UserResponse {
  id: number;
  branchId?: number | null;
  branchName?: string | null;
  branchAddress?: string | null;
  avatarUrl?: string | null;
  fullName: string;
  email: string;
  phone: string;
  status: string;
  active?: boolean;
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

export interface UpdateMyProfileRequest {
  fullName: string;
  email: string;
  phone: string;
}

export interface UpdateMyProfileResponse {
  profile: UserResponse;
  emailChanged: boolean;
  accessToken?: string;
  refreshToken?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
