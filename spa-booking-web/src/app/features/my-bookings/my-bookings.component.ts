import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppointmentService } from '../../core/services/appointment.service';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-bookings.component.html',
})
export class MyBookingsComponent implements OnInit {
  bookings: any[] = [];
  filteredBookings: any[] = [];
  selectedStatus = 'ALL';
  isLoading = true;

  statusTabs = [
    { key: 'ALL', label: 'Tất cả lịch hẹn' },
    { key: 'PENDING', label: 'Chờ xác nhận' },
    { key: 'CONFIRMED', label: 'Đã xác nhận' },
    { key: 'COMPLETED', label: 'Đã hoàn thành' },
    { key: 'CANCELLED', label: 'Đã hủy' },
  ];

  constructor(
    private appointmentService: AppointmentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadMyBookings();
  }

  loadMyBookings(): void {
    this.isLoading = true;
    this.appointmentService.getMyAppointments(0, 50).subscribe({
      next: (res) => {
        this.bookings = res?.content || (Array.isArray(res) ? res : []);
        this.filterByStatus(this.selectedStatus);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải lịch hẹn cá nhân:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  filterByStatus(status: string): void {
    this.selectedStatus = status;
    if (status === 'ALL') {
      this.filteredBookings = [...this.bookings];
    } else {
      this.filteredBookings = this.bookings.filter((b) => b.status === status);
    }
  }

  cancelBooking(booking: any): void {
    const serviceName = booking.serviceName || booking.service?.name || 'này';
    const confirmed = confirm(`Bạn có chắc chắn muốn hủy lịch hẹn dịch vụ "${serviceName}" không?`);
    if (!confirmed) return;

    this.appointmentService.cancelAppointment(booking.id).subscribe({
      next: () => {
        this.loadMyBookings();
      },
      error: (err) => {
        alert(err?.error?.message || 'Không thể hủy lịch hẹn này!');
      },
    });
  }

  getStatusBadge(status: string): { text: string; classes: string } {
    switch (status) {
      case 'PENDING':
        return {
          text: 'Chờ xác nhận',
          classes: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
        };
      case 'CONFIRMED':
        return { text: 'Đã xác nhận', classes: 'bg-sky-500/15 text-sky-400 border-sky-500/30' };
      case 'COMPLETED':
        return {
          text: 'Hoàn thành',
          classes: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
        };
      case 'CANCELLED':
        return { text: 'Đã hủy', classes: 'bg-rose-500/15 text-rose-400 border-rose-500/30' };
      default:
        return { text: status, classes: 'bg-slate-700 text-slate-300 border-slate-600' };
    }
  }
}
