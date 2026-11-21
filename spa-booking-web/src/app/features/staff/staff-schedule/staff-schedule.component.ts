import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-staff-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-schedule.component.html',
})
export class StaffScheduleComponent implements OnInit {
  schedules: any[] = [];
  selectedStatus = '';
  selectedDate = '';
  todayStr = '';
  isLoading = false;

  statusTabs = [
    { key: '', label: 'Tất cả ca làm' },
    { key: 'CONFIRMED', label: 'Sắp thực hiện (Đã xác nhận)' },
    { key: 'PENDING', label: 'Chờ duyệt' },
    { key: 'COMPLETED', label: 'Đã hoàn thành' },
    { key: 'CANCELLED', label: 'Đã hủy' },
  ];

  constructor(
    private appointmentService: AppointmentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.todayStr = today.toISOString().split('T')[0];
    this.loadSchedule();
  }

  loadSchedule(): void {
    this.isLoading = true;
    this.appointmentService
      .getMyStaffSchedule(this.selectedStatus, this.selectedDate || undefined, 0, 50)
      .subscribe({
        next: (res: any) => {
          this.schedules = res?.content || (Array.isArray(res) ? res : []);
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Lỗi tải lịch làm việc của nhân viên:', err);
          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  filterByStatus(status: string): void {
    this.selectedStatus = status;
    this.loadSchedule();
  }

  setTodayFilter(): void {
    this.selectedDate = this.todayStr;
    this.loadSchedule();
  }

  clearDateFilter(): void {
    this.selectedDate = '';
    this.loadSchedule();
  }

  completeAppointment(item: any): void {
    const confirmed = confirm(
      `Xác nhận bạn đã hoàn thành liệu trình cho khách "${item.customerName}"?`,
    );
    if (!confirmed) return;

    this.appointmentService.updateStatus(item.id, 'COMPLETED').subscribe({
      next: () => {
        this.loadSchedule();
      },
      error: (err) => {
        alert(err?.error?.message || 'Không thể cập nhật trạng thái!');
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
