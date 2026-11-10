import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-customer-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customer.component.html',
})
export class CustomerManagementComponent implements OnInit, OnDestroy {
  customers: any[] = [];
  isLoading = true;
  searchKeyword = '';

  private searchSubject = new Subject<string>();
  private searchSubscription!: Subscription;

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Tự động tìm kiếm sau khi ngừng gõ 300ms
    this.searchSubscription = this.searchSubject
      .pipe(
        debounceTime(100),
        distinctUntilChanged()
      )
      .subscribe((keyword) => {
        this.searchKeyword = keyword;
        this.loadCustomers();
      });

    this.loadCustomers();
  }

  // Sửa thành hàm nhận sự kiện gõ trực tiếp từ input
  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  loadCustomers(): void {
    this.isLoading = true;
    this.cdr.markForCheck(); // Thông báo Angular chuẩn bị re-render spinner

    this.userService.searchUsers(this.searchKeyword, 'ROLE_CUSTOMER', 0, 50).subscribe({
      next: (res) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        this.customers = list.filter((u: any) => u.roles?.includes('ROLE_CUSTOMER'));
        this.isLoading = false;

        // Cập nhật lại giao diện ngay lập tức mà không cần click chuột
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải danh sách khách hàng:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleStatus(id: number): void {
    this.userService.toggleUserStatus(id).subscribe({
      next: () => this.loadCustomers(),
      error: (err) => alert(err.error?.message || 'Thao tác thất bại!')
    });
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }
}
