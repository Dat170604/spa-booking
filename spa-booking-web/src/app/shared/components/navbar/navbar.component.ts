import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserResponse } from '../../../core/models/user.model';
import { AppNotification } from '../../../core/models/notification.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class NavbarComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  isLoggedIn = false;
  isAdmin = false;
  isStaff = false;
  currentUser: UserResponse | null = null;

  isDropdownOpen = false;
  isMobileMenuOpen = false;
  isNotifOpen = false;

  notifications$ = this.notificationService.notifications$;
  unreadCount$ = this.notificationService.unreadCount$;

  private subscriptions = new Subscription();

  ngOnInit(): void {
    // 1. Theo dõi trạng thái đăng nhập từ AuthService
    const authSub = this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
      this.isLoggedIn = !!user;

      if (user && user.roles) {
        this.isAdmin = user.roles.includes('ROLE_ADMIN');
        this.isStaff = user.roles.includes('ROLE_STAFF');
        this.notificationService.initRealtimeNotifications();
      } else {
        this.isAdmin = false;
        this.isStaff = false;
        this.notificationService.disconnect();
      }
      this.cdr.detectChanges();
    });

    // 2. Cập nhật tức thì khi người dùng đổi tên / email / avatar ở trang /profile
    const profileSub = this.userService.profileUpdated$.subscribe((updatedUser) => {
      this.currentUser = updatedUser;
      this.cdr.detectChanges();
    });

    this.subscriptions.add(authSub);
    this.subscriptions.add(profileSub);
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.isNotifOpen = false;
    }
  }

  toggleNotificationDropdown(): void {
    this.isNotifOpen = !this.isNotifOpen;
    if (this.isNotifOpen) {
      this.isDropdownOpen = false;
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  onSelectNotification(item: AppNotification): void {
    if (!item.read) {
      this.notificationService.localMarkAsRead(item.id);
      this.notificationService.markAsRead(item.id).subscribe();
    }
    this.isNotifOpen = false;
    if (item.targetUrl) {
      this.router.navigateByUrl(item.targetUrl);
    }
  }

  markAllRead(): void {
    this.notificationService.localMarkAllAsRead();
    this.notificationService.markAllAsRead().subscribe();
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.isNotifOpen = false;
    this.notificationService.disconnect();
    this.authService.logout();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
