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
  selectedStatus = '';
  isLoading = false;

  statusTabs = [
    { key: '', label: 'Tất cả' },
    { key: 'PENDING', label: 'Chờ xác nhận' },
    { key: 'CONFIRMED', label: 'Đã xác nhận' },
    { key: 'COMPLETED', label: 'Hoàn thành' },
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
      next: (res: any) => {
        this.bookings = res?.content || (Array.isArray(res) ? res : []);
        this.applyFilter();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải danh sách lịch hẹn:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  filterByStatus(status: string): void {
    this.selectedStatus = status;
    this.applyFilter();
  }

  private applyFilter(): void {
    if (!this.selectedStatus) {
      this.filteredBookings = [...this.bookings];
    } else {
      this.filteredBookings = this.bookings.filter((b) => b.status === this.selectedStatus);
    }
  }

  // Chỉ cho phép khách hàng hủy lịch trước thời điểm hẹn ít nhất 24 giờ
  canCancel(item: any): boolean {
    if (!['PENDING', 'CONFIRMED'].includes(item.status)) {
      return false;
    }
    if (!item.appointmentDate || !item.startTime) {
      return false;
    }

    const timePart = item.startTime.slice(0, 5); // "HH:mm"
    const appointmentDateTime = new Date(`${item.appointmentDate}T${timePart}:00`);
    const now = new Date();

    const diffInHours = (appointmentDateTime.getTime() - now.getTime()) / (1000 * 60 * 60);
    return diffInHours >= 24;
  }

  cancelBooking(item: any): void {
    if (!this.canCancel(item)) {
      alert('Bạn chỉ có thể hủy lịch hẹn trước giờ thực hiện ít nhất 24 giờ.');
      return;
    }

    const reason = prompt(`Nhập lý do hủy lịch hẹn #${item.id} (không bắt buộc):`);
    if (reason === null) return; // Người dùng bấm Cancel trên hộp thoại prompt

    this.appointmentService.cancelAppointment(item.id, reason.trim()).subscribe({
      next: () => {
        alert('Đã hủy lịch hẹn thành công!');
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
