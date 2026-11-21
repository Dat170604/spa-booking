import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StaffLeaveService } from '../../../core/services/staff-leave.service';
import { StaffLeaveRequest, StaffLeaveResponse } from '../../../core/models/staff-leave.model';

@Component({
  selector: 'app-staff-leave',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-leave.component.html',
  styleUrl: './staff-leave.component.scss',
})
export class StaffLeaveComponent implements OnInit {
  private staffLeaveService = inject(StaffLeaveService);
  private cdr = inject(ChangeDetectorRef);

  leaves: StaffLeaveResponse[] = [];
  isLoading = false;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  minDate = new Date().toISOString().split('T')[0];

  form: StaffLeaveRequest = {
    leaveDate: this.minDate,
    fullDay: true,
    startTime: '08:00',
    endTime: '12:00',
    reason: '',
  };

  ngOnInit(): void {
    this.loadMyLeaves();
  }

  loadMyLeaves(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.staffLeaveService.getMyLeaves().subscribe({
      next: (data) => {
        this.leaves = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Không thể tải danh sách nghỉ phép';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitLeave(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.form.leaveDate || !this.form.reason?.trim()) {
      this.errorMessage = 'Vui lòng chọn ngày nghỉ và nhập lý do!';
      this.cdr.detectChanges();
      return;
    }

    this.isSubmitting = true;
    this.cdr.detectChanges();

    const payload: StaffLeaveRequest = {
      leaveDate: this.form.leaveDate,
      fullDay: this.form.fullDay,
      startTime: this.form.fullDay ? null : this.form.startTime,
      endTime: this.form.fullDay ? null : this.form.endTime,
      reason: this.form.reason.trim(),
    };

    this.staffLeaveService.createLeave(payload).subscribe({
      next: () => {
        this.successMessage = 'Đã gửi đơn xin nghỉ phép thành công! Đang chờ quản lý phê duyệt.';
        this.form.reason = '';
        this.isSubmitting = false;
        this.cdr.detectChanges();
        this.loadMyLeaves();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi gửi đơn xin nghỉ';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelLeave(id: number): void {
    if (!confirm('Bạn có chắc chắn muốn hủy đơn xin nghỉ phép này?')) return;

    this.staffLeaveService.deleteLeave(id).subscribe({
      next: () => {
        this.successMessage = 'Đã hủy đơn xin nghỉ phép.';
        this.cdr.detectChanges();
        this.loadMyLeaves();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Không thể hủy đơn';
        this.cdr.detectChanges();
      },
    });
  }
}
