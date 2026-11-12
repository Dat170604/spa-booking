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

  // Biến phục vụ upload & xem trước ảnh
  imagePreview: string | null = null;
  isUploadingImage = false;

  constructor(
    private branchService: BranchService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
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
      imageUrl: [''],
      description: [''],
      openTime: ['08:00', Validators.required],
      closeTime: ['21:00', Validators.required],
      active: [true],
    });
  }

  loadBranches(): void {
    this.isLoading = true;
    this.branchService.searchBranches('', 0, 50).subscribe({
      next: (res) => {
        if (res && res.content) {
          this.branches = res.content;
        } else if (Array.isArray(res)) {
          this.branches = res;
        } else {
          this.branches = [];
        }
        this.isLoading = false;
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
      this.imagePreview = branch.imageUrl || null;
      this.branchForm.patchValue(branch);
    } else {
      this.isEdit = false;
      this.selectedBranchId = null;
      this.imagePreview = null;
      this.branchForm.reset({ openTime: '08:00', closeTime: '21:00', active: true, imageUrl: '' });
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.imagePreview = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview = reader.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);

    this.isUploadingImage = true;
    this.branchService.uploadImage(file).subscribe({
      next: (res) => {
        this.branchForm.patchValue({ imageUrl: res.imageUrl });
        this.imagePreview = res.imageUrl;
        this.isUploadingImage = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi upload ảnh chi nhánh:', err);
        alert('Upload ảnh thất bại, vui lòng thử lại!');
        this.isUploadingImage = false;
        this.cdr.detectChanges();
      },
    });
  }

  removeImage(): void {
    this.imagePreview = null;
    this.branchForm.patchValue({ imageUrl: '' });
  }

  saveBranch(): void {
    if (this.branchForm.invalid || this.isUploadingImage) return;

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

  deleteBranch(branch: any): void {
    const confirmed = confirm(`Bạn có chắc chắn muốn xóa chi nhánh "${branch.name}" không?`);
    if (!confirmed) return;

    this.branchService.deleteBranch(branch.id).subscribe({
      next: () => {
        this.loadBranches();
      },
      error: (err) => {
        console.error('Lỗi khi xóa chi nhánh:', err);
        alert(
          'Không thể xóa chi nhánh này (có thể chi nhánh đang có lịch hẹn hoặc nhân viên trực thuộc).',
        );
      },
    });
  }
}
