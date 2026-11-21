import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StaffLeaveService } from '../../../core/services/staff-leave.service';
import { StaffLeaveRequest, StaffLeaveResponse } from '../../../core/models/staff-leave.model';

@Component({
  selector: 'app-admin-staff-leave',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-staff-leave.component.html',
  styleUrl: './admin-staff-leave.component.scss',
})
export class AdminStaffLeaveComponent implements OnInit {
  private staffLeaveService = inject(StaffLeaveService);
  private cdr = inject(ChangeDetectorRef);

  allLeaves: StaffLeaveResponse[] = [];
  statusFilter: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'PENDING';
  isLoading = false;
  showCreateForm = false;
  errorMessage = '';
  successMessage = '';

  minDate = new Date().toISOString().split('T')[0];

  adminForm: StaffLeaveRequest = {
    staffId: null,
    leaveDate: this.minDate,
    fullDay: true,
    startTime: '08:00',
    endTime: '17:00',
    reason: '',
  };

  get pendingCount(): number {
    return this.allLeaves.filter((l) => l.status === 'PENDING').length;
  }

  get filteredLeaves(): StaffLeaveResponse[] {
    if (this.statusFilter === 'ALL') return this.allLeaves;
    return this.allLeaves.filter((l) => l.status === this.statusFilter);
  }

  ngOnInit(): void {
    this.loadAllLeaves();
  }

  loadAllLeaves(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.staffLeaveService.getAllLeaves().subscribe({
      next: (data) => {
        this.allLeaves = data;
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

  changeStatus(id: number, status: 'APPROVED' | 'REJECTED'): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.staffLeaveService.updateStatus(id, status).subscribe({
      next: () => {
        this.successMessage =
          status === 'APPROVED' ? 'Đã phê duyệt đơn nghỉ phép!' : 'Đã từ chối đơn nghỉ phép.';
        this.cdr.detectChanges();
        this.loadAllLeaves();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Thao tác thất bại';
        this.cdr.detectChanges();
      },
    });
  }

  createByAdmin(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.adminForm.staffId || !this.adminForm.leaveDate) {
      this.errorMessage = 'Vui lòng nhập ID kỹ thuật viên và chọn ngày nghỉ!';
      this.cdr.detectChanges();
      return;
    }

    const payload: StaffLeaveRequest = {
      ...this.adminForm,
      startTime: this.adminForm.fullDay ? null : this.adminForm.startTime,
      endTime: this.adminForm.fullDay ? null : this.adminForm.endTime,
    };

    this.staffLeaveService.createLeave(payload).subscribe({
      next: () => {
        this.successMessage = 'Đã tạo và áp dụng lịch nghỉ cho kỹ thuật viên!';
        this.showCreateForm = false;
        this.adminForm.reason = '';
        this.cdr.detectChanges();
        this.loadAllLeaves();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Không thể tạo lịch nghỉ';
        this.cdr.detectChanges();
      },
    });
  }

  deleteLeave(id: number): void {
    if (!confirm('Xóa bản ghi nghỉ phép này khỏi hệ thống?')) return;

    this.staffLeaveService.deleteLeave(id).subscribe({
      next: () => {
        this.successMessage = 'Đã xóa bản ghi nghỉ phép.';
        this.cdr.detectChanges();
        this.loadAllLeaves();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Không thể xóa';
        this.cdr.detectChanges();
      },
    });
  }
}
