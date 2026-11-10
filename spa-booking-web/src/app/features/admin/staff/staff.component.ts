import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { BranchService } from '../../../core/services/branch.service';

@Component({
  selector: 'app-staff-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './staff.component.html',
})
export class StaffManagementComponent implements OnInit {
  staffList: any[] = [];
  branches: any[] = [];
  isLoading = true;

  // Modal Control
  isCreateModalOpen = false;
  isEditModalOpen = false;
  selectedStaffId: number | null = null;

  createForm!: FormGroup;
  editForm!: FormGroup;

  constructor(
    private userService: UserService,
    private branchService: BranchService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadStaff();
    this.loadBranches();
  }

  private initForms(): void {
    // Form Tạo mới Nhân viên
    this.createForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      branchId: ['', Validators.required],
    });

    // Form Chỉnh sửa Admin dành riêng cho Nhân viên (Info, Branch, Role)
    this.editForm = this.fb.group({
      fullName: ['', Validators.required],
      phone: ['', Validators.required],
      branchId: ['', Validators.required],
      role: ['ROLE_STAFF', Validators.required],
    });
  }

  loadStaff(): void {
    this.isLoading = true;
    this.userService.searchUsers('', '', 0, 50).subscribe({
      next: (res) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        // Lọc lấy tài khoản có Role là STAFF hoặc ADMIN
        this.staffList = list.filter(
          (u: any) => u.roles?.includes('ROLE_STAFF') || u.roles?.includes('ROLE_ADMIN'),
        );
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải danh sách nhân viên:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadBranches(): void {
    this.branchService.getActiveBranches().subscribe({
      next: (data) => (this.branches = data || []),
      error: (err) => console.error('Lỗi tải danh sách chi nhánh:', err),
    });
  }

  // --- Modal Thêm mới Nhân viên ---
  openCreateModal(): void {
    this.isCreateModalOpen = true;
    this.createForm.reset({ branchId: '' });
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.userService.createStaff(this.createForm.value).subscribe({
      next: () => {
        this.closeCreateModal();
        this.loadStaff();
      },
      error: (err) => alert(err.error?.message || 'Tạo nhân viên thất bại!'),
    });
  }

  // --- Modal Chỉnh sửa Nhân viên ---
  openEditModal(staff: any): void {
    this.selectedStaffId = staff.id;
    this.isEditModalOpen = true;

    // Lấy Role cao nhất hoặc ưu tiên ROLE_ADMIN nếu có
    const currentRole = staff.roles?.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_STAFF';

    this.editForm.patchValue({
      fullName: staff.fullName,
      phone: staff.phone,
      branchId: staff.branchId || staff.branch?.id || '',
      role: currentRole,
    });
  }

  closeEditModal(): void {
    this.isEditModalOpen = false;
    this.selectedStaffId = null;
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.selectedStaffId) {
      this.editForm.markAllAsTouched();
      return;
    }

    const { fullName, phone, branchId, role } = this.editForm.value;

    // 1. Cập nhật thông tin cơ bản
    this.userService.updateProfile(this.selectedStaffId, { fullName, phone }).subscribe({
      next: () => {
        // 2. Cập nhật Chi nhánh
        this.userService.updateUserBranch(this.selectedStaffId!, branchId).subscribe({
          next: () => {
            // 3. Cập nhật Role
            this.userService.updateUserRoles(this.selectedStaffId!, [role]).subscribe({
              next: () => {
                this.closeEditModal();
                this.loadStaff();
              },
              error: (err) => alert(err.error?.message || 'Lỗi cập nhật Vai trò!'),
            });
          },
          error: (err) => alert(err.error?.message || 'Lỗi cập nhật Chi nhánh!'),
        });
      },
      error: (err) => alert(err.error?.message || 'Lỗi cập nhật thông tin cá nhân!'),
    });
  }

  toggleStatus(id: number): void {
    this.userService.toggleUserStatus(id).subscribe({
      next: () => this.loadStaff(),
      error: (err) => alert(err.error?.message || 'Thao tác thất bại!'),
    });
  }

  get cf() {
    return this.createForm.controls;
  }
  get ef() {
    return this.editForm.controls;
  }
}
