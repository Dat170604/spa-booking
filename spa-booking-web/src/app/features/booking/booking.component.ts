import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { BranchService } from '../../core/services/branch.service';
import { SpaServiceService } from '../../core/services/spa-service.service';
import { AppointmentService } from '../../core/services/appointment.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.scss'],
})
export class BookingComponent implements OnInit {
  bookingForm!: FormGroup;
  branches: any[] = [];
  services: any[] = [];
  selectedServiceIds: number[] = [];

  isLoading = false;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private branchService: BranchService,
    private spaService: SpaServiceService,
    private appointmentService: AppointmentService,
    public authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadData();
  }

  private initForm(): void {
    this.bookingForm = this.fb.group({
      branchId: ['', [Validators.required]],
      appointmentDate: ['', [Validators.required]],
      startTime: ['', [Validators.required]],
      notes: [''],
    });
  }

  private loadData(): void {
    this.isLoading = true;
    this.branchService.getActiveBranches().subscribe({
      next: (res) => (this.branches = res),
      error: () => (this.branches = []),
    });

    this.spaService.getAllServices().subscribe({
      next: (res) => {
        this.services = res;
        this.isLoading = false;
      },
      error: () => (this.isLoading = false),
    });
  }

  toggleService(serviceId: number): void {
    const index = this.selectedServiceIds.indexOf(serviceId);
    if (index > -1) {
      this.selectedServiceIds.splice(index, 1);
    } else {
      this.selectedServiceIds.push(serviceId);
    }
  }

  isServiceSelected(serviceId: number): boolean {
    return this.selectedServiceIds.includes(serviceId);
  }

  getTotalPrice(): number {
    return this.services
      .filter((s) => this.selectedServiceIds.includes(s.id))
      .reduce((sum, s) => sum + (s.price || 0), 0);
  }

  getTotalDuration(): number {
    return this.services
      .filter((s) => this.selectedServiceIds.includes(s.id))
      .reduce((sum, s) => sum + (s.durationMinutes || 0), 0);
  }

  onSubmit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    if (this.selectedServiceIds.length === 0) {
      this.errorMessage = 'Vui lòng chọn ít nhất 1 dịch vụ!';
      return;
    }

    if (this.bookingForm.invalid) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const formVal = this.bookingForm.value;
    const request = {
      branchId: Number(formVal.branchId),
      appointmentDate: formVal.appointmentDate,
      startTime: formVal.startTime.length === 5 ? `${formVal.startTime}:00` : formVal.startTime,
      notes: formVal.notes,
      details: this.selectedServiceIds.map((id) => ({ serviceId: id })),
    };

    this.appointmentService.createAppointment(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = 'Đặt lịch thành công! Lotus Spa sẽ liên hệ xác nhận sớm nhất.';
        setTimeout(() => this.router.navigate(['/']), 2500);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Đặt lịch thất bại. Vui lòng thử lại!';
      },
    });
  }
}
