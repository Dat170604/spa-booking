export type StaffLeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface StaffLeaveRequest {
  staffId?: number | null;
  leaveDate: string; // Định dạng 'YYYY-MM-DD'
  fullDay: boolean;
  startTime?: string | null; // Định dạng 'HH:mm'
  endTime?: string | null; // Định dạng 'HH:mm'
  reason?: string;
}

export interface StaffLeaveResponse {
  id: number;
  staffId: number;
  staffName: string;
  branchName: string;
  leaveDate: string;
  fullDay: boolean;
  startTime: string | null;
  endTime: string | null;
  reason: string;
  status: StaffLeaveStatus;
}
