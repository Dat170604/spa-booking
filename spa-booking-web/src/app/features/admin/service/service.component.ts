import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SpaServiceService } from '../../../core/services/spa-service.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-service-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './service.component.html',
})
export class ServiceManagementComponent implements OnInit {
  services: any[] = [];
  serviceForm!: FormGroup;
  isModalOpen = false;
  isEdit = false;
  selectedId: number | null = null;

  constructor(
    private spaService: SpaServiceService,
    private fb: FormBuilder,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadServices();
  }

  private initForm(): void {
    this.serviceForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      price: [0, [Validators.required, Validators.min(0)]],
      durationMinutes: [60, [Validators.required, Validators.min(15)]],
      status: ['ACTIVE'],
    });
  }

  loadServices(): void {
    this.spaService.getAllServices().subscribe({
      next: (data) => (this.services = data),
    });
  }

  openModal(item?: any): void {
    this.isModalOpen = true;
    if (item) {
      this.isEdit = true;
      this.selectedId = item.id;
      this.serviceForm.patchValue(item);
    } else {
      this.isEdit = false;
      this.selectedId = null;
      this.serviceForm.reset({ price: 0, durationMinutes: 60, status: 'ACTIVE' });
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  saveService(): void {
    if (this.serviceForm.invalid) return;

    const body = this.serviceForm.value;
    const url = `${environment.apiUrl}/services`;

    if (this.isEdit && this.selectedId) {
      this.http.put(`${url}/${this.selectedId}`, body).subscribe({
        next: () => {
          this.closeModal();
          this.loadServices();
        },
      });
    } else {
      this.http.post(url, body).subscribe({
        next: () => {
          this.closeModal();
          this.loadServices();
        },
      });
    }
  }

  deleteService(id: number): void {
    if (confirm('Bạn có chắc chắn muốn xóa dịch vụ này?')) {
      this.http.delete(`${environment.apiUrl}/services/${id}`).subscribe({
        next: () => this.loadServices(),
      });
    }
  }
}
