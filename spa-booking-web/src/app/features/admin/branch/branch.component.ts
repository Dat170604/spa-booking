import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BranchService } from '../../../core/services/branch.service';

@Component({
  selector: 'app-branch-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './branch.component.html',
})
export class BranchManagementComponent implements OnInit {
  branches: any[] = [];
  branchForm!: FormGroup;
  isModalOpen = false;
  isEdit = false;
  selectedBranchId: number | null = null;
  isLoading = false;

  constructor(
    private branchService: BranchService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef, // 1. Inject ChangeDetectorRef vào constructor
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadBranches();
  }

  private initForm(): void {
    this.branchForm = this.fb.group({
      name: ['', Validators.required],
      address: ['', Validators.required],
      phone: ['', Validators.required],
      email: [''],
      description: [''],
      openTime: ['08:00', Validators.required],
      closeTime: ['21:00', Validators.required],
      active: [true],
    });
  }

  loadBranches(): void {
    this.isLoading = true;

    // Gọi API tìm kiếm / lấy danh sách chi nhánh
    this.branchService.searchBranches('', 0, 10).subscribe({
      next: (res) => {
        // 2. Kiểm tra nếu kết quả là đối tượng Page của Spring Boot thì lấy res.content, nếu là Array thì lấy trực tiếp res
        if (res && res.content) {
          this.branches = res.content;
        } else if (Array.isArray(res)) {
          this.branches = res;
        } else {
          this.branches = [];
        }

        this.isLoading = false;

        // 3. BẮT BUỘC: Ép Angular cập nhật UI ngay lập tức mà không cần tương tác Modal
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải danh sách chi nhánh:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openModal(branch?: any): void {
    this.isModalOpen = true;
    if (branch) {
      this.isEdit = true;
      this.selectedBranchId = branch.id;
      this.branchForm.patchValue(branch);
    } else {
      this.isEdit = false;
      this.selectedBranchId = null;
      this.branchForm.reset({ openTime: '08:00', closeTime: '21:00', active: true });
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  saveBranch(): void {
    if (this.branchForm.invalid) return;

    const body = this.branchForm.value;

    if (this.isEdit && this.selectedBranchId) {
      this.branchService.updateBranch(this.selectedBranchId, body).subscribe({
        next: () => {
          this.closeModal();
          this.loadBranches();
        },
      });
    } else {
      this.branchService.createBranch(body).subscribe({
        next: () => {
          this.closeModal();
          this.loadBranches();
        },
      });
    }
  }

  toggleStatus(id: number): void {
    this.branchService.toggleBranchStatus(id).subscribe({
      next: () => this.loadBranches(),
    });
  }
}
