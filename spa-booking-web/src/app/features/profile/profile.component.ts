import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  profile: UserResponse | null = null;
  isLoading = false;
  isSavingInfo = false;
  isChangingPass = false;
  isUploadingAvatar = false;

  infoMessage = '';
  infoError = '';
  passMessage = '';
  passError = '';

  infoForm = {
    fullName: '',
    email: '',
    phone: '',
  };

  passForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  };

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.infoForm = {
          fullName: data.fullName || '',
          email: data.email || '',
          phone: data.phone || '',
        };
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.infoError = err.error?.message || 'Không thể tải thông tin cá nhân';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (file.size > 5 * 1024 * 1024) {
      this.infoError = 'Kích thước ảnh tối đa là 5MB!';
      this.cdr.detectChanges();
      return;
    }

    this.isUploadingAvatar = true;
    this.infoError = '';
    this.infoMessage = '';
    this.cdr.detectChanges();

    this.userService.uploadMyAvatar(file).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.infoMessage = 'Cập nhật ảnh đại diện thành công!';
        this.isUploadingAvatar = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.infoError = err.error?.message || 'Tải ảnh đại diện thất bại';
        this.isUploadingAvatar = false;
        this.cdr.detectChanges();
      },
    });
  }

  saveProfile(): void {
    this.infoError = '';
    this.infoMessage = '';
    this.isSavingInfo = true;
    this.cdr.detectChanges();

    this.userService.updateMyProfile(this.infoForm).subscribe({
      next: (res) => {
        this.profile = res.profile;
        this.infoMessage = 'Cập nhật thông tin cá nhân thành công!';
        this.isSavingInfo = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.infoError = err.error?.message || 'Cập nhật thất bại';
        this.isSavingInfo = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitChangePassword(): void {
    this.passError = '';
    this.passMessage = '';

    if (this.passForm.newPassword !== this.passForm.confirmPassword) {
      this.passError = 'Mật khẩu xác nhận không khớp!';
      this.cdr.detectChanges();
      return;
    }

    this.isChangingPass = true;
    this.cdr.detectChanges();

    this.userService
      .changePassword({
        currentPassword: this.passForm.currentPassword,
        newPassword: this.passForm.newPassword,
      })
      .subscribe({
        next: (res) => {
          this.passMessage = res.message || 'Đổi mật khẩu thành công!';
          this.passForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
          this.isChangingPass = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.passError = err.error?.message || 'Đổi mật khẩu thất bại';
          this.isChangingPass = false;
          this.cdr.detectChanges();
        },
      });
  }
}
