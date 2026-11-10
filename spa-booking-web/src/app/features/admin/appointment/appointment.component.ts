import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-appointment-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointment.component.html',
})
export class AppointmentManagementComponent implements OnInit {
  appointments: any[] = [];
  filteredAppointments: any[] = [];
  selectedStatus = 'ALL';
  isLoading = true;

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.appointmentService.getAllAppointments().subscribe({
      next: (data) => {
        this.appointments = data;
        this.filterAppointments();
        this.isLoading = false;
      },
      error: () => (this.isLoading = false),
    });
  }

  filterAppointments(): void {
    if (this.selectedStatus === 'ALL') {
      this.filteredAppointments = this.appointments;
    } else {
      this.filteredAppointments = this.appointments.filter((a) => a.status === this.selectedStatus);
    }
  }

  changeStatus(id: number, newStatus: string): void {
    this.appointmentService.updateStatus(id, newStatus).subscribe({
      next: () => this.loadAppointments(),
      error: (err) => alert(err.error?.message || 'Cập nhật trạng thái thất bại!'),
    });
  }
}
