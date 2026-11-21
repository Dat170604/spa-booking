import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  StaffLeaveRequest,
  StaffLeaveResponse,
  StaffLeaveStatus,
} from '../models/staff-leave.model';

@Injectable({
  providedIn: 'root',
})
export class StaffLeaveService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/staff-leaves`;

  /**
   * Tạo đơn xin nghỉ phép mới
   * - Staff gửi: mặc định trạng thái PENDING
   * - Admin tạo cho Staff: mặc định trạng thái APPROVED
   */
  createLeave(payload: StaffLeaveRequest): Observable<StaffLeaveResponse> {
    return this.http.post<StaffLeaveResponse>(this.apiUrl, payload);
  }

  /**
   * Lấy danh sách đơn nghỉ phép của bản thân (Staff đang đăng nhập)
   */
  getMyLeaves(): Observable<StaffLeaveResponse[]> {
    return this.http.get<StaffLeaveResponse[]>(`${this.apiUrl}/my`);
  }

  /**
   * Admin lấy toàn bộ danh sách đơn xin nghỉ (có thể lọc theo branchId)
   */
  getAllLeaves(branchId?: number | null): Observable<StaffLeaveResponse[]> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId.toString());
    }
    return this.http.get<StaffLeaveResponse[]>(this.apiUrl, { params });
  }

  /**
   * Admin phê duyệt (APPROVED) hoặc từ chối (REJECTED) đơn xin nghỉ
   */
  updateStatus(
    id: number,
    status: Exclude<StaffLeaveStatus, 'PENDING'>,
  ): Observable<StaffLeaveResponse> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<StaffLeaveResponse>(`${this.apiUrl}/${id}/status`, null, { params });
  }

  /**
   * Hủy đơn (khi còn PENDING với Staff) hoặc xóa đơn (với Admin)
   */
  deleteLeave(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
