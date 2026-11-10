import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { adminGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Trang chủ PUBLIC (Khách chưa đăng nhập hoặc đã đăng nhập đều vào được)
  {
    path: '',
    component: HomeComponent,
  },

  // Các trang Auth
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },

  // Trang đặt lịch của Khách hàng
  {
    path: 'booking',
    loadComponent: () =>
      import('./features/booking/booking.component').then((m) => m.BookingComponent),
  },
  {
    path: 'branches',
    loadComponent: () =>
      import('./features/branches/branch-list.component').then((m) => m.BranchListComponent),
  },
  {
    path: 'services',
    loadComponent: () =>
      import('./features/services-list/services-list.component').then(
        (m) => m.ServicesListComponent,
      ),
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
          import('./features/admin/staff/staff.component').then(
            (m) => m.StaffManagementComponent,
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
