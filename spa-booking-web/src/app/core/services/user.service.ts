import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API_URL = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  searchUsers(
    keyword: string = '',
    role: string = '',
    page: number = 0,
    size: number = 10,
    sortBy: string = 'id',
    sortDir: string = 'asc',
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (keyword && keyword.trim() !== '') {
      params = params.set('keyword', keyword.trim());
    }
    if (role && role.trim() !== '') {
      params = params.set('role', role.trim());
    }

    return this.http.get<any>(this.API_URL, { params });
  }

  createStaff(request: any): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/staff`, request);
  }

  toggleUserStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/toggle-status`, {});
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  updateProfile(id: number, data: { fullName: string; phone: string }): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/${id}`, data);
  }

  updateUserRoles(id: number, roles: string[]): Observable<any> {
    return this.http.patch<any>(`${this.API_URL}/${id}/roles`, { roles });
  }

  updateUserBranch(id: number, branchId: number): Observable<any> {
    return this.http.patch<any>(`${this.API_URL}/${id}/branch`, { branchId });
  }
}
