import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { BranchService } from '../../../core/services/branch.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-appointment-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointment.component.html',
})
export class AppointmentManagementComponent implements OnInit {
  appointments: any[] = [];
  branches: any[] = [];
  staffList: any[] = [];

  // Cache danh sách KTV theo từng chi nhánh: { [branchId]: Staff[] }
  staffByBranchMap: { [branchId: number]: any[] } = {};

  // Bộ lọc tìm kiếm
  filterKeyword = '';
  filterStatus = '';
  filterBranchId: number | null = null;
  filterDate = '';

  // Phân trang
  currentPage = 0;
  pageSize = 15;
  totalElements = 0;
  totalPages = 0;

  isLoading = false;

  // Modal chi tiết & phân công KTV
  isDetailModalOpen = false;
  selectedAppointment: any = null;
  selectedStaffId: number | null = null;
  isAssigningStaff = false;

  statusOptions = [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'PENDING', label: 'Chờ xác nhận' },
    { value: 'CONFIRMED', label: 'Đã xác nhận' },
    { value: 'COMPLETED', label: 'Hoàn thành' },
    { value: 'CANCELLED', label: 'Đã hủy' },
  ];

  constructor(
    private appointmentService: AppointmentService,
    private branchService: BranchService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadBranches();
    this.loadAppointments();
  }

  loadBranches(): void {
    this.branchService.searchBranches('', 0, 50).subscribe({
      next: (res: any) => {
        this.branches = res?.content || (Array.isArray(res) ? res : []);
        this.branches.forEach((b) => this.fetchStaffForBranch(b.id));
        this.cdr.detectChanges();
      },
    });
  }

  fetchStaffForBranch(branchId: number): void {
    if (!branchId || this.staffByBranchMap[branchId]) return;
    this.userService.getStaffByBranch(branchId).subscribe({
      next: (res: any) => {
        this.staffByBranchMap[branchId] = res?.content || (Array.isArray(res) ? res : []);
        this.cdr.detectChanges();
      },
      error: () => {
        this.staffByBranchMap[branchId] = [];
      },
    });
  }

  loadAppointments(page: number = 0): void {
    this.isLoading = true;
    this.currentPage = page;

    this.appointmentService
      .searchAppointments(
        this.filterKeyword.trim(),
        this.filterStatus,
        this.filterBranchId || undefined,
        this.filterDate || undefined,
        this.currentPage,
        this.pageSize,
      )
      .subscribe({
        next: (res: any) => {
          if (res && res.content) {
            this.appointments = res.content;
            this.totalElements = res.totalElements || 0;
            this.totalPages = res.totalPages || 0;
          } else if (Array.isArray(res)) {
            this.appointments = res;
            this.totalElements = res.length;
            this.totalPages = 1;
          } else {
            this.appointments = [];
          }

          this.appointments.forEach((a) => {
            if (a.branchId) this.fetchStaffForBranch(a.branchId);
          });

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

  resetFilters(): void {
    this.filterKeyword = '';
    this.filterStatus = '';
    this.filterBranchId = null;
    this.filterDate = '';
    this.loadAppointments(0);
  }

  // Phân công nhanh KTV ngay trên bảng danh sách
  onQuickAssignStaff(appointment: any, staffId: number | null): void {
    if (!staffId) return;
    this.appointmentService.assignStaff(appointment.id, Number(staffId)).subscribe({
      next: (updated) => {
        appointment.staffId = updated.staffId;
        appointment.staffName = updated.staffName;
        this.cdr.detectChanges();
      },
      error: (err) => {
        alert(err?.error?.message || 'Kỹ thuật viên này bị trùng lịch vào khung giờ trên!');
        this.loadAppointments(this.currentPage);
      },
    });
  }

  changeStatus(appointment: any, newStatus: string): void {
    const actionLabel =
      newStatus === 'CONFIRMED' ? 'XÁC NHẬN' : newStatus === 'COMPLETED' ? 'HOÀN THÀNH' : 'HỦY';

    const confirmed = confirm(
      `Bạn muốn chuyển lịch hẹn #${appointment.id} của khách "${appointment.customerName}" sang trạng thái ${actionLabel}?`,
    );
    if (!confirmed) return;

    this.appointmentService.updateStatus(appointment.id, newStatus).subscribe({
      next: () => {
        this.loadAppointments(this.currentPage);
        if (this.selectedAppointment?.id === appointment.id) {
          this.selectedAppointment.status = newStatus;
        }
      },
      error: (err) => {
        alert(err?.error?.message || 'Cập nhật trạng thái thất bại!');
      },
    });
  }

  openDetailModal(appointment: any): void {
    this.selectedAppointment = appointment;
    this.selectedStaffId = appointment.staffId || null;
    this.isDetailModalOpen = true;

    if (appointment.branchId) {
      if (this.staffByBranchMap[appointment.branchId]) {
        this.staffList = this.staffByBranchMap[appointment.branchId];
      } else {
        this.userService.getStaffByBranch(appointment.branchId).subscribe({
          next: (res: any) => {
            const list = res?.content || (Array.isArray(res) ? res : []);
            this.staffByBranchMap[appointment.branchId] = list;
            this.staffList = list;
            this.cdr.detectChanges();
          },
        });
      }
    }
  }

  closeDetailModal(): void {
    this.isDetailModalOpen = false;
    this.selectedAppointment = null;
  }

  saveStaffAssignment(): void {
    if (!this.selectedAppointment || !this.selectedStaffId) return;

    this.isAssigningStaff = true;
    this.appointmentService
      .assignStaff(this.selectedAppointment.id, Number(this.selectedStaffId))
      .subscribe({
        next: (updated) => {
          this.isAssigningStaff = false;
          this.selectedAppointment = updated;
          this.loadAppointments(this.currentPage);
          alert('Phân công kỹ thuật viên thành công!');
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isAssigningStaff = false;
          alert(err?.error?.message || 'Kỹ thuật viên bị trùng lịch hoặc có lỗi xảy ra!');
          this.cdr.detectChanges();
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
