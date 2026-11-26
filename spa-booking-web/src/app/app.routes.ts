import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import {ProfileComponent} from "./features/profile/profile.component";

export const routes: Routes = [
  // ==================== 1. CÁC TRANG PUBLIC (KHÁCH VÃNG LAI XEM ĐƯỢC) ====================
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'services',
    loadComponent: () =>
      import('./features/services/services-list.component').then((m) => m.ServicesListComponent),
  },
  {
    path: 'branches',
    loadComponent: () =>
      import('./features/branches/branch-list.component').then((m) => m.BranchListComponent),
  },

  // ==================== 2. XÁC THỰC (ĐĂNG NHẬP / ĐĂNG KÝ) ====================
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
  },

  // ==================== 3. CÁC TRANG KHÁCH HÀNG (YÊU CẦU ĐĂNG NHẬP) ====================
  {
    path: 'booking',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/booking/booking.component').then((m) => m.BookingComponent),
  },
  {
    path: 'my-bookings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-bookings/my-bookings.component').then((m) => m.MyBookingsComponent),
  },

  // Trang Quản trị ADMIN (Yêu cầu đăng nhập & Role ADMIN/STAFF)
  {
    path: 'admin',
    loadComponent: () => import('./shared/layout/layout.component').then((m) => m.LayoutComponent),
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/admin/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        path: 'appointments',
        loadComponent: () =>
          import('./features/admin/appointment/appointment.component').then(
            (m) => m.AppointmentManagementComponent,
          ),
      },
      {
        path: 'branches',
        loadComponent: () =>
          import('./features/admin/branch/branch.component').then(
            (m) => m.BranchManagementComponent,
          ),
      },
      {
        path: 'services',
        loadComponent: () =>
          import('./features/admin/service/service.component').then(
            (m) => m.ServiceManagementComponent,
          ),
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./features/admin/customer/customer.component').then(
            (m) => m.CustomerManagementComponent,
          ),
      },
      {
        path: 'staffs',
        loadComponent: () =>
          import('./features/admin/staff/staff.component').then((m) => m.StaffManagementComponent),
      },
      {
        path: 'services',
        loadComponent: () =>
          import('./features/admin/service/service.component').then(
            (m) => m.ServiceManagementComponent,
          ),
      },
      {
        path: 'staff-leaves', // Đường dẫn /admin/staff-leaves
        loadComponent: () =>
          import('./features/admin/admin-staff-leave/admin-staff-leave.component').then(
            (m) => m.AdminStaffLeaveComponent,
          ),
      },
      {
        path: 'audit-logs',
        loadComponent: () =>
          import('./features/admin/audit-logs/audit-logs.component').then(
            (m) => m.AuditLogsComponent,
          ),
      },
    ],
  },

  {
    path: 'staff',
    loadComponent: () => import('./shared/layout/layout.component').then((m) => m.LayoutComponent),
    canActivate: [adminGuard], // Giữ nguyên guard của bạn
    children: [
      {
        path: '',
        redirectTo: 'my-schedule',
        pathMatch: 'full',
      },
      {
        path: 'my-schedule', // Đường dẫn /staff/my-schedule
        loadComponent: () =>
          import('./features/staff/staff-schedule/staff-schedule.component').then(
            (m) => m.StaffScheduleComponent,
          ),
      },
      {
        path: 'leaves', // Đường dẫn /staff/leaves
        loadComponent: () =>
          import('./features/staff/staff-leave/staff-leave.component').then(
            (m) => m.StaffLeaveComponent,
          ),
      },
    ],
  },

  // Wildcard quay về trang chủ
  {
    path: '**',
    redirectTo: '',
  },
];
