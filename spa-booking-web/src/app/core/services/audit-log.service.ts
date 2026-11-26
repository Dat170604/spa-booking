import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuditLog {
  id: number;
  actorEmail: string;
  actorName: string;
  actorRoles: string;
  module: string;
  action: string;
  description: string;
  targetId: string | null;
  httpMethod: string;
  endpoint: string;
  ipAddress: string;
  status: 'SUCCESS' | 'FAILED';
  errorMessage: string | null;
  executionTimeMs: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/admin/audit-logs`;

  getLogs(filters: {
    keyword?: string;
    module?: string;
    status?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Observable<any> {
    let params = new HttpParams().set('page', filters.page ?? 0).set('size', filters.size ?? 15);

    if (filters.keyword?.trim()) params = params.set('keyword', filters.keyword.trim());
    if (filters.module) params = params.set('module', filters.module);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);

    return this.http.get<any>(this.API_URL, { params });
  }
}
