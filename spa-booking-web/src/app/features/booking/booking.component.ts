import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BranchService } from '../../core/services/branch.service';
import { SpaServiceService } from '../../core/services/spa-service.service';
import { AppointmentService } from '../../core/services/appointment.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './booking.component.html',
})
export class BookingComponent implements OnInit {
  bookingForm!: FormGroup;
  branches: any[] = [];
  services: any[] = [];
  staffList: any[] = [];

  selectedServiceIds: number[] = [];
  busyRanges: { startTime: string; endTime: string }[] = [];
  timeSlots: string[] = [];

  minDate: string = '';
  isSubmitting = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private branchService: BranchService,
    private spaService: SpaServiceService,
    private appointmentService: AppointmentService,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.minDate = today.toISOString().split('T')[0];

    this.initForm();
    this.generateTimeSlots(); // Sinh khung giờ mặc định ban đầu
    this.loadInitialData();
  }

  private initForm(): void {
    this.bookingForm = this.fb.group({
      branchId: [null, Validators.required],
      staffId: [null],
      appointmentDate: [this.minDate, Validators.required],
      startTime: ['', Validators.required],
      notes: [''],
    });
  }

  private loadInitialData(): void {
    this.branchService.getActiveBranches().subscribe({
      next: (res: any) => {
        this.branches = res?.content || (Array.isArray(res) ? res : []);
        this.applyQueryParams();
        this.generateTimeSlots();
        this.cdr.detectChanges();
      },
    });

    this.spaService.getServices('', '', 0, 100).subscribe({
      next: (res: any) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        this.services = list.filter((s: any) => s.status === 'ACTIVE' || s.active === true);
        this.applyQueryParams();
        this.generateTimeSlots();
        this.cdr.detectChanges();
      },
    });
  }

  private applyQueryParams(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['branchId']) {
        const bId = Number(params['branchId']);
        this.bookingForm.patchValue({ branchId: bId });
        this.loadStaffByBranch(bId);
        this.generateTimeSlots();
      }
      if (params['serviceId']) {
        const sId = Number(params['serviceId']);
        if (!this.selectedServiceIds.includes(sId)) {
          this.selectedServiceIds = [sId];
          this.generateTimeSlots();
        }
      }
    });
  }

  // Sinh danh sách khung giờ cách nhau 30 phút dựa theo giờ mở cửa & đóng cửa của chi nhánh
  generateTimeSlots(): void {
    const branch = this.selectedBranch;
    const openStr = branch?.openTime ? branch.openTime.slice(0, 5) : '08:00';
    const closeStr = branch?.closeTime ? branch.closeTime.slice(0, 5) : '21:00';

    const startMinutes = this.toMinutes(openStr);
    const closeMinutes = this.toMinutes(closeStr);

    // Thời lượng dịch vụ (nếu chưa chọn thì mặc định 30 phút để hiển thị các khung giờ hợp lệ)
    const serviceDuration = this.totalDuration > 0 ? this.totalDuration : 30;
    const lastPossibleStart = closeMinutes - serviceDuration;

    const slots: string[] = [];
    for (let m = startMinutes; m <= lastPossibleStart; m += 30) {
      slots.push(this.fromMinutes(m));
    }

    this.timeSlots = slots;
    this.validateSelectedSlot();
  }

  // Chuyển chuỗi "HH:mm" thành tổng số phút tính từ 00:00
  private toMinutes(timeStr: string): number {
    const [h, m] = timeStr.slice(0, 5).split(':').map(Number);
    return h * 60 + m;
  }

  // Chuyển tổng số phút thành chuỗi "HH:mm"
  private fromMinutes(totalMinutes: number): string {
    const hours = Math.floor(totalMinutes / 60);
    const mins = totalMinutes % 60;
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
  }

  toggleService(serviceId: number): void {
    const index = this.selectedServiceIds.indexOf(serviceId);
    if (index > -1) {
      this.selectedServiceIds.splice(index, 1);
    } else {
      this.selectedServiceIds.push(serviceId);
    }
    // Cập nhật lại danh sách khung giờ vì tổng thời lượng dịch vụ vừa thay đổi
    this.generateTimeSlots();
  }

  isServiceSelected(serviceId: number): boolean {
    return this.selectedServiceIds.includes(serviceId);
  }

  onBranchChange(): void {
    const branchId = this.bookingForm.get('branchId')?.value;
    this.bookingForm.patchValue({ staffId: null, startTime: '' });
    this.busyRanges = [];
    this.generateTimeSlots();

    if (branchId) {
      this.loadStaffByBranch(Number(branchId));
    } else {
      this.staffList = [];
    }
  }

  onStaffOrDateChange(): void {
    const staffId = this.bookingForm.get('staffId')?.value;
    const date = this.bookingForm.get('appointmentDate')?.value;

    if (!staffId || !date) {
      this.busyRanges = [];
      return;
    }

    this.appointmentService.getStaffBusySlots(Number(staffId), date).subscribe({
      next: (ranges) => {
        this.busyRanges = ranges || [];
        this.validateSelectedSlot();
        this.cdr.detectChanges();
      },
      error: () => {
        this.busyRanges = [];
      },
    });
  }

  // Kiểm tra khung giờ này có bị trùng với lịch bận của KTV không
  isSlotBusy(slot: string): boolean {
    if (!this.bookingForm.get('staffId')?.value || this.busyRanges.length === 0) {
      return false;
    }

    const duration = this.totalDuration > 0 ? this.totalDuration : 30;
    const slotStart = this.toMinutes(slot);
    const slotEnd = slotStart + duration;

    return this.busyRanges.some((range) => {
      const busyStart = this.toMinutes(range.startTime);
      const busyEnd = this.toMinutes(range.endTime);
      return slotStart < busyEnd && slotEnd > busyStart;
    });
  }

  // Nếu giờ đang chọn không còn nằm trong timeSlots hoặc bị trùng lịch KTV thì tự động bỏ chọn
  private validateSelectedSlot(): void {
    const currentSlot = this.bookingForm.get('startTime')?.value;
    if (currentSlot && (!this.timeSlots.includes(currentSlot) || this.isSlotBusy(currentSlot))) {
      this.bookingForm.patchValue({ startTime: '' });
    }
  }

  loadStaffByBranch(branchId: number): void {
    this.userService.getStaffByBranch(branchId).subscribe({
      next: (res: any) => {
        this.staffList = res?.content || (Array.isArray(res) ? res : []);
        this.cdr.detectChanges();
      },
      error: () => {
        this.staffList = [];
      },
    });
  }

  selectTimeSlot(time: string): void {
    if (this.isSlotBusy(time)) return;
    this.bookingForm.patchValue({ startTime: time });
  }

  get selectedBranch(): any {
    const id = Number(this.bookingForm.get('branchId')?.value);
    return this.branches.find((b) => b.id === id);
  }

  get selectedServicesList(): any[] {
    return this.services.filter((s) => this.selectedServiceIds.includes(s.id));
  }

  get totalDuration(): number {
    return this.selectedServicesList.reduce((sum, s) => sum + (s.durationMinutes || 0), 0);
  }

  get totalAmount(): number {
    return this.selectedServicesList.reduce((sum, s) => sum + (s.price || 0), 0);
  }

  submitBooking(): void {
    if (this.bookingForm.invalid || this.selectedServiceIds.length === 0) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const formVal = this.bookingForm.value;
    const payload: any = {
      branchId: Number(formVal.branchId),
      serviceIds: this.selectedServiceIds,
      staffId: formVal.staffId ? Number(formVal.staffId) : null,
      appointmentDate: formVal.appointmentDate,
      startTime: formVal.startTime.length === 5 ? `${formVal.startTime}:00` : formVal.startTime,
      notes: formVal.notes,
    };

    this.appointmentService.createAppointment(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('Đặt lịch thành công! Lotus Spa sẽ liên hệ xác nhận với bạn trong ít phút.');
        this.router.navigate(['/my-bookings']);
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err?.error?.message || '';
        if (msg.toLowerCase().includes('lỗi hệ thống') || err.status === 500) {
          this.errorMessage =
            'Kỹ thuật viên đã có lịch trong khung giờ này hoặc thời gian không hợp lệ. Vui lòng chọn khung giờ khác!';
        } else {
          this.errorMessage =
            msg || 'Kỹ thuật viên đã có lịch hẹn trong khung giờ này. Vui lòng chọn giờ khác!';
        }
        this.cdr.detectChanges();
      },
    });
  }
}
