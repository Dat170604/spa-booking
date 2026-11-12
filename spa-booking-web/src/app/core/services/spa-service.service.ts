import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SpaServiceService {
  private readonly API_URL = `${environment.apiUrl}/services`;

  constructor(private http: HttpClient) {}

  // Lấy danh sách dịch vụ (Hỗ trợ tìm kiếm theo từ khóa, danh mục, phân trang)
  getServices(keyword: string = '', category: string = '', page = 0, size = 50): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (keyword && keyword.trim() !== '') {
      params = params.set('keyword', keyword.trim());
    }
    if (category && category.trim() !== '') {
      params = params.set('category', category.trim());
    }
    return this.http.get<any>(this.API_URL, { params });
  }

  // Tạo dịch vụ mới
  createService(data: any): Observable<any> {
    return this.http.post<any>(this.API_URL, data);
  }

  // Cập nhật thông tin dịch vụ
  updateService(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/${id}`, data);
  }

  // Bật / Tắt trạng thái kinh doanh dịch vụ
  toggleServiceStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/toggle-status`, {});
  }

  // Xóa dịch vụ
  deleteService(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  uploadImage(file: File): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imageUrl: string }>(`${this.API_URL}/upload-image`, formData);
  }
}
