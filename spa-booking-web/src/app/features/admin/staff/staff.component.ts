import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { UserService } from '../../../core/services/user.service';
import { BranchService } from '../../../core/services/branch.service';

@Component({
  selector: 'app-staff',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './staff.component.html',
})
export class StaffManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private branchService = inject(BranchService);
  private cdr = inject(ChangeDetectorRef);

  staffList: any[] = [];
  branches: any[] = [];
  isLoading = false;

  isCreateModalOpen = false;
  isEditModalOpen = false;
  selectedStaff: any = null;

  createForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required]],
    branchId: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^(0[3|5|7|8|9])+([0-9]{8})$/)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  editForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required]],
    phone: ['', [Validators.pattern(/^(0[3|5|7|8|9])+([0-9]{8})$/)]],
    branchId: [''],
    role: ['ROLE_STAFF', [Validators.required]],
  });

  get cf() {
    return this.createForm.controls;
  }

  ngOnInit(): void {
    this.loadStaff();
    this.loadBranches();
  }

  loadStaff(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.userService.searchUsers('', 'ROLE_STAFF', 0, 50, 'id', 'desc').subscribe({
      next: (res) => {
        this.staffList = res.content || res || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadBranches(): void {
    this.branchService.getActiveBranches().subscribe({
      next: (res: any) => {
        this.branches = res.content || res || [];
        this.cdr.detectChanges();
      },
    });
  }

  openCreateModal(): void {
    this.createForm.reset({ branchId: '' });
    this.isCreateModalOpen = true;
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const payload = {
      ...this.createForm.value,
      branchId: Number(this.createForm.value.branchId),
    };

    this.userService.createStaff(payload).subscribe({
      next: () => {
        this.closeCreateModal();
        this.loadStaff();
      },
      error: (err) => alert(err.error?.message || 'Thêm nhân viên thất bại'),
    });
  }

  openEditModal(staff: any): void {
    this.selectedStaff = staff;
    const primaryRole = staff.roles?.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_STAFF';

    // Đối chiếu thêm theo branchName phòng trường hợp API danh sách chỉ trả về branchName mà branchId bị null
    const matchedBranch = this.branches.find((b) => b.name === staff.branchName);
    const currentBranchId = staff.branchId ?? staff.branch?.id ?? matchedBranch?.id ?? '';

    // Dùng reset() thay vì patchValue() để đặt lại trạng thái dirty = false cho tất cả các ô
    this.editForm.reset({
      fullName: staff.fullName || '',
      phone: staff.phone || '',
      branchId: currentBranchId,
      role: primaryRole,
    });

    this.isEditModalOpen = true;
  }

  closeEditModal(): void {
    this.isEditModalOpen = false;
    this.selectedStaff = null;
  }

  submitEdit(): void {
    if (!this.selectedStaff || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const formVal = this.editForm.value;
    const requests: Observable<any>[] = [];

    // 1. Chỉ gọi UPDATE_USER nếu người dùng thực sự sửa ô Họ tên hoặc SĐT
    const infoDirty = this.editForm.get('fullName')?.dirty || this.editForm.get('phone')?.dirty;
    if (infoDirty) {
      requests.push(
        this.userService.updateProfile(this.selectedStaff.id, {
          fullName: formVal.fullName?.trim(),
          phone: formVal.phone?.trim() || '',
        }),
      );
    }

    // 2. Chỉ gọi UPDATE_USER_BRANCH nếu người dùng thực sự chọn lại Chi nhánh
    if (this.editForm.get('branchId')?.dirty && formVal.branchId) {
      requests.push(
        this.userService.updateUserBranch(this.selectedStaff.id, Number(formVal.branchId)),
      );
    }

    // 3. Chỉ gọi UPDATE_USER_ROLES nếu người dùng thực sự chọn lại Quyền
    if (this.editForm.get('role')?.dirty && formVal.role) {
      const updatedRoles =
        formVal.role === 'ROLE_ADMIN'
          ? ['ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_CUSTOMER']
          : ['ROLE_STAFF', 'ROLE_CUSTOMER'];
      requests.push(this.userService.updateUserRoles(this.selectedStaff.id, updatedRoles));
    }

    if (requests.length === 0) {
      this.closeEditModal();
      return;
    }

    forkJoin(requests).subscribe({
      next: () => {
        this.closeEditModal();
        this.loadStaff();
      },
      error: (err) => alert(err.error?.message || 'Cập nhật nhân viên thất bại'),
    });
  }

  toggleStatus(id: number): void {
    this.userService.toggleUserStatus(id).subscribe({
      next: () => this.loadStaff(),
      error: (err) => alert(err.error?.message || 'Thao tác thất bại'),
    });
  }
}
