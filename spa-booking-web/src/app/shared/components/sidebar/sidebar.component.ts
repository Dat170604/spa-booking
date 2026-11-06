import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NavItem } from '../../../core/models/sidebar.model';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent implements OnInit {
  isCollapsed = false;
  userRoles: string[] = [];

  // Danh sách các menu quản trị
  menuItems: NavItem[] = [
    {
      label: 'Tổng quan',
      icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
      route: '/admin/dashboard',
      roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
    },
    {
      label: 'Quản lý Đặt lịch',
      icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
      route: '/admin/appointments',
      roles: ['ROLE_ADMIN', 'ROLE_STAFF'],
    },
    {
      label: 'Quản lý Chi nhánh',
      icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m3 0h1m-4-8h4m-4-4h4m6 0v12m-4-12v12',
      route: '/admin/branches',
      roles: ['ROLE_ADMIN'],
    },
    {
      label: 'Quản lý Dịch vụ',
      icon: 'M13 10V3L4 14h7v7l9-11h-7z',
      route: '/admin/services',
      roles: ['ROLE_ADMIN'],
    },
    {
      label: 'Quản lý Người dùng',
      icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
      route: '/admin/users',
      roles: ['ROLE_ADMIN'],
    },
  ];

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      if (user && user.roles) {
        this.userRoles = user.roles;
      } else {
        this.userRoles = [];
      }
    });
  }

  // Kiểm tra user có quyền xem menu hay không
  hasPermission(itemRoles?: string[]): boolean {
    if (!itemRoles || itemRoles.length === 0) return true;
    return itemRoles.some((role) => this.userRoles.includes(role));
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}
