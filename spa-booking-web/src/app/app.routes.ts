import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

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
    loadComponent: () =>
      import('./features/admin/layout/layout.component').then((m) => m.LayoutComponent),
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
        path: 'staff',
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
        path: 'my-schedule',
        loadComponent: () =>
          import('./features/admin/staff-schedule/staff-schedule.component').then(
            (m) => m.StaffScheduleComponent,
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
