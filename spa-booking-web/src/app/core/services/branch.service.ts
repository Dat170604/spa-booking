import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BranchService {
  private readonly API_URL = `${environment.apiUrl}/branches`;

  constructor(private http: HttpClient) {}

  // Lấy danh sách chi nhánh active (Public)
  getActiveBranches(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/active`);
  }

  // Xem chi tiết 1 chi nhánh
  getBranchById(id: number): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/${id}`);
  }

  // Tìm kiếm & phân trang (Admin)
  searchBranches(
    keyword: string = '',
    page = 0,
    size = 10,
    sortBy = 'id',
    sortDir = 'asc',
  ): Observable<any> {
    let params = new HttpParams()
      .set('keyword', keyword) // Truyền chuỗi rỗng thay vì để null
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<any>(this.API_URL, { params });
  }

  // Tạo chi nhánh mới (Admin)
  createBranch(data: any): Observable<any> {
    return this.http.post<any>(this.API_URL, data);
  }

  // Cập nhật chi nhánh (Admin)
  updateBranch(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/${id}`, data);
  }

  // Khóa / Kích hoạt chi nhánh (Admin)
  toggleBranchStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/toggle-status`, {});
  }

  // Xóa chi nhánh (Admin)
  deleteBranch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  uploadImage(file: File): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imageUrl: string }>(`${this.API_URL}/upload-image`, formData);
  }
}
