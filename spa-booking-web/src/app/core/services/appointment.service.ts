import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AppointmentRequest {
  branchId: number;
  serviceId: number;
  staffId?: number | null;
  appointmentDate: string; // YYYY-MM-DD
  startTime: string; // HH:mm:ss
  notes?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AppointmentService {
  private apiUrl = `${environment.apiUrl}/appointments`;

  constructor(private http: HttpClient) {}

  // ==================== DÀNH CHO KHÁCH HÀNG ====================

  // 1. Tạo lịch hẹn mới
  createAppointment(data: AppointmentRequest): Observable<any> {
    return this.http.post<any>(this.apiUrl, data);
  }

  // 2. Lấy danh sách lịch hẹn của tài khoản đang đăng nhập
  getMyAppointments(page: number = 0, size: number = 50): Observable<any> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/my-appointments`, { params });
  }

  // 3. Khách hàng tự hủy lịch hẹn
  cancelAppointment(id: number, reason?: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/cancel`, reason ? { reason } : {});
  }

  // ==================== DÀNH CHO ADMIN / LỄ TÂN ====================

  // 5. Tìm kiếm & lọc toàn bộ lịch hẹn
  searchAppointments(
    keyword: string = '',
    status: string = '',
    branchId?: number,
    date?: string,
    page: number = 0,
    size: number = 20,
  ): Observable<any> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (keyword) params = params.set('keyword', keyword);
    if (status) params = params.set('status', status);
    if (branchId) params = params.set('branchId', branchId.toString());
    if (date) params = params.set('date', date);

    return this.http.get<any>(this.apiUrl, { params });
  }

  // 6. Xem chi tiết một lịch hẹn
  getAppointmentById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // 7. Cập nhật trạng thái lịch hẹn (CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)
  updateStatus(id: number, status: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/status`, { status });
  }

  // 8. Phân công kỹ thuật viên
  assignStaff(id: number, staffId: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/${id}/assign-staff`, { staffId });
  }

  // Lấy danh sách khoảng giờ bận của KTV trong ngày
  getStaffBusySlots(
    staffId: number,
    date: string,
  ): Observable<{ startTime: string; endTime: string }[]> {
    const params = new HttpParams().set('staffId', staffId.toString()).set('date', date);
    return this.http.get<{ startTime: string; endTime: string }[]>(
      `${this.apiUrl}/staff-busy-slots`,
      { params },
    );
  }

  // Lấy danh sách lịch làm việc được phân công cho nhân viên đang đăng nhập
  getMyStaffSchedule(status: string = '', date?: string, page: number = 0, size: number = 50,
  ): Observable<any> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (status) params = params.set('status', status);
    if (date) params = params.set('date', date);

    return this.http.get<any>(`${this.apiUrl}/my-schedule`, { params });
  }
}
