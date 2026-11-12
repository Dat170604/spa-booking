import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { SpaServiceService } from '../../../core/services/spa-service.service';

@Component({
  selector: 'app-service-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './service.component.html',
})
export class ServiceManagementComponent implements OnInit, OnDestroy {
  services: any[] = [];
  isLoading = true;
  searchKeyword = '';
  selectedCategory = '';
  selectedFile: File | null = null;
  imagePreview: string | null = null;
  isUploading = false;

  // Danh mục dịch vụ Spa mẫu (Có thể đồng bộ với Enum ở Backend)
  categories = [
    { value: 'MASSAGE', label: 'Massage Trị Liệu' },
    { value: 'SKINCARE', label: 'Chăm Sóc Da Mặt' },
    { value: 'BODY_CARE', label: 'Chăm Sóc Toàn Thân' },
    { value: 'HAIR_NAIL', label: 'Gội Đầu Dưỡng Sinh & Nail' },
    { value: 'COMBO', label: 'Gói Combo Thư Giãn' },
  ];

  // Modal Control
  isModalOpen = false;
  isEditMode = false;
  selectedServiceId: number | null = null;
  serviceForm!: FormGroup;

  // Real-time search
  private searchSubject = new Subject<string>();
  private searchSubscription!: Subscription;

  constructor(
    private spaService: SpaServiceService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.initForm();

    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((keyword) => {
        this.searchKeyword = keyword;
        this.loadServices();
      });

    this.loadServices();
  }

  private initForm(): void {
    this.serviceForm = this.fb.group({
      name: ['', Validators.required],
      category: ['MASSAGE', Validators.required],
      price: [0, [Validators.required, Validators.min(1000)]],
      durationMinutes: [60, [Validators.required, Validators.min(15)]],
      imageUrl: [''],
      description: [''],
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onCategoryChange(event: Event): void {
    this.selectedCategory = (event.target as HTMLSelectElement).value;
    this.loadServices();
  }

  loadServices(): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    this.spaService.getServices(this.searchKeyword, this.selectedCategory, 0, 50).subscribe({
      next: (res) => {
        this.services = res?.content || (Array.isArray(res) ? res : []);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải danh sách dịch vụ:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];

      // Tạo ảnh xem trước ngay lập tức trên trình duyệt
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result as string;
        this.cdr.detectChanges();
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  // Cập nhật lại openCreateModal & openEditModal để reset/hiển thị ảnh
  openCreateModal(): void {
    this.isEditMode = false;
    this.selectedServiceId = null;
    this.selectedFile = null;
    this.imagePreview = null;
    this.serviceForm.reset({
      category: 'MASSAGE',
      price: 200000,
      durationMinutes: 60,
      imageUrl: '',
    });
    this.isModalOpen = true;
  }

  openEditModal(service: any): void {
    this.isEditMode = true;
    this.selectedServiceId = service.id;
    this.selectedFile = null;
    this.imagePreview = service.imageUrl || null;
    this.serviceForm.patchValue({
      name: service.name,
      category: service.category || 'MASSAGE',
      price: service.price,
      durationMinutes: service.durationMinutes || service.duration,
      imageUrl: service.imageUrl || '',
      description: service.description || '',
    });
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  submitForm(): void {
    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      return;
    }

    if (this.selectedFile) {
      this.isUploading = true;
      this.spaService.uploadImage(this.selectedFile).subscribe({
        next: (res) => {
          this.serviceForm.patchValue({ imageUrl: res.imageUrl });
          this.isUploading = false;
          this.saveServiceData();
        },
        error: (err) => {
          this.isUploading = false;
          alert(err.error?.message || 'Upload ảnh thất bại!');
        },
      });
    } else {
      this.saveServiceData();
    }
  }

  private saveServiceData(): void {
    const payload = this.serviceForm.value;
    if (this.isEditMode && this.selectedServiceId) {
      this.spaService.updateService(this.selectedServiceId, payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadServices();
        },
        error: (err) => alert(err.error?.message || 'Cập nhật dịch vụ thất bại!'),
      });
    } else {
      this.spaService.createService(payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadServices();
        },
        error: (err) => alert(err.error?.message || 'Thêm dịch vụ thất bại!'),
      });
    }
  }

  toggleStatus(id: number): void {
    this.spaService.toggleServiceStatus(id).subscribe({
      next: () => this.loadServices(),
      error: (err) => alert(err.error?.message || 'Thao tác thất bại!'),
    });
  }

  deleteService(id: number, name: string): void {
    if (confirm(`Bạn có chắc chắn muốn xóa dịch vụ "${name}" không?`)) {
      this.spaService.deleteService(id).subscribe({
        next: () => this.loadServices(),
        error: (err) => alert(err.error?.message || 'Không thể xóa dịch vụ đang có lịch đặt!'),
      });
    }
  }

  getCategoryLabel(val: string): string {
    const found = this.categories.find((c) => c.value === val);
    return found ? found.label : val || 'Khác';
  }

  get f() {
    return this.serviceForm.controls;
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }
}
