import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TokenService } from '../../../core/services/token.service';

export interface MenuItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent implements OnInit {
  private tokenService = inject(TokenService);

  isCollapsed = false;
  userRoles: string[] = [];

  // Nhóm menu dành riêng cho Quản trị viên (ROLE_ADMIN)
  adminMenuItems: MenuItem[] = [
    {
      label: 'Tổng quan & Lịch hẹn',
      route: '/admin/dashboard',
      icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
    },
    {
      label: 'Quản lý lịch hẹn',
      route: '/admin/appointments',
      icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    },
    {
      label: 'Quản lý khách hàng',
      route: '/admin/customers',
      icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
    },
    {
      label: 'Quản lý nhân viên',
      route: '/admin/staffs',
      icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
    },
    {
      label: 'Quản lý dịch vụ',
      route: '/admin/services',
      icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
    },
    {
      label: 'Quản lý chi nhánh',
      route: '/admin/branches',
      icon: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z',
    },
    {
      label: 'Duyệt nghỉ phép KTV',
      route: '/admin/staff-leaves',
      icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
    },
    {
      label: 'Nhật ký hệ thống',
      route: '/admin/audit-logs',
      icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    },
  ];

  // Nhóm menu dành riêng cho Kỹ thuật viên (ROLE_STAFF)
  staffMenuItems: MenuItem[] = [
    {
      label: 'Lịch làm việc của tôi',
      route: '/staff/my-schedule',
      icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    },
    {
      label: 'Đăng ký xin nghỉ',
      route: '/staff/leaves',
      icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
    },
  ];

  ngOnInit(): void {
    const userInfo = this.tokenService.getUserInfo();
    if (userInfo?.roles && Array.isArray(userInfo.roles)) {
      this.userRoles = userInfo.roles;
    } else if (userInfo?.role) {
      this.userRoles = [userInfo.role];
    } else {
      // Dự phòng giải mã trực tiếp từ JWT Access Token nếu user_info không chứa roles
      this.userRoles = this.extractRolesFromToken();
    }
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  hasRole(role: string): boolean {
    const cleanRole = role.replace('ROLE_', '');
    return this.userRoles.some((r) => r === role || r === cleanRole || `ROLE_${r}` === role);
  }

  private extractRolesFromToken(): string[] {
    const token = this.tokenService.getAccessToken();
    if (!token) return [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (Array.isArray(payload.roles)) return payload.roles;
      if (Array.isArray(payload.authorities)) return payload.authorities;
      if (typeof payload.role === 'string') return [payload.role];
      return [];
    } catch {
      return [];
    }
  }
}
