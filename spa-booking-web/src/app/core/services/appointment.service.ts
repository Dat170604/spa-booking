import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AppointmentRequest {
  branchId: number;
  appointmentDate: string; // YYYY-MM-DD
  startTime: string; // HH:mm:ss hoặc HH:mm
  notes?: string;
  details: {
    serviceId: number;
    staffId?: number | null;
  }[];
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly API_URL = `${environment.apiUrl}/appointments`;

  constructor(private http: HttpClient) {}

  createAppointment(request: AppointmentRequest): Observable<any> {
    return this.http.post<any>(this.API_URL, request);
  }

  getMyAppointments(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/my-appointments`);
  }
}
