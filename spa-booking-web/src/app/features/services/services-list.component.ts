import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { SpaServiceService } from '../../core/services/spa-service.service';

@Component({
  selector: 'app-services-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './services-list.component.html',
  styleUrls: ['./services-list.component.scss'],
})
export class ServicesListComponent implements OnInit, OnDestroy {
  services: any[] = [];
  isLoading = true;
  searchKeyword = '';
  selectedCategory = '';

  categories = [
    { value: '', label: 'Tất cả' },
    { value: 'MASSAGE', label: 'Massage Trị Liệu' },
    { value: 'SKINCARE', label: 'Chăm Sóc Da Mặt' },
    { value: 'BODY_CARE', label: 'Chăm Sóc Toàn Thân' },
    { value: 'HAIR_NAIL', label: 'Gội Đầu Dưỡng Sinh & Nail' },
    { value: 'COMBO', label: 'Gói Combo Thư Giãn' },
  ];

  private searchSubject = new Subject<string>();
  private searchSubscription!: Subscription;

  constructor(
    private spaService: SpaServiceService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((keyword) => {
        this.searchKeyword = keyword;
        this.loadServices();
      });

    this.loadServices();
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  selectCategory(categoryValue: string): void {
    this.selectedCategory = categoryValue;
    this.loadServices();
  }

  loadServices(): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    this.spaService.getServices(this.searchKeyword, this.selectedCategory, 0, 50).subscribe({
      next: (res) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        // Khách hàng chỉ thấy các dịch vụ đang mở bán (ACTIVE)
        this.services = list.filter((s: any) => s.status === 'ACTIVE' || s.active === true);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getCategoryLabel(val: string): string {
    const found = this.categories.find((c) => c.value === val);
    return found ? found.label : 'Dịch vụ Spa';
  }

  bookService(serviceId: number): void {
    const token = localStorage.getItem('access_token') || localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: '/booking', serviceId },
      });
    } else {
      this.router.navigate(['/booking'], { queryParams: { serviceId } });
    }
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }
}
