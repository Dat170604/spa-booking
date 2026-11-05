import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.isLoading = false;
        // Đăng nhập thành công -> chuyển về trang chủ (hoặc trang đặt lịch)
        this.router.navigate(['/']);
      },    
      error: (err) => {
        this.isLoading = false;
        // Lấy thông điệp lỗi từ Backend trả về (nếu có AppException/ErrorCode)
        this.errorMessage = err.error?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại Email và Mật khẩu!';
      }
    });
  }

  // Getter giúp gọi nhanh control trong HTML
  get f() {
    return this.loginForm.controls;
  }
}