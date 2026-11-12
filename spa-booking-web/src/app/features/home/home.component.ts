import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SpaServiceService } from '../../core/services/spa-service.service';
import { BranchService } from '../../core/services/branch.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit {
  featuredServices: any[] = [];
  branches: any[] = [];
  isLoading = true;

  constructor(
    private spaService: SpaServiceService,
    private branchService: BranchService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadFeaturedData();
  }

  loadFeaturedData(): void {
    this.isLoading = true;

    // Lấy 6 dịch vụ nổi bật (chỉ lấy dịch vụ đang ACTIVE)
    this.spaService.getServices('', '', 0, 10).subscribe({
      next: (res) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        this.featuredServices = list
          .filter((s: any) => s.status === 'ACTIVE' || s.active === true)
          .slice(0, 6);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });

    // Lấy tối đa 3 chi nhánh đang hoạt động qua hàm public
    this.branchService.getActiveBranches().subscribe({
      next: (res: any) => {
        const list = res?.content || (Array.isArray(res) ? res : []);
        this.branches = list.slice(0, 3);
        this.cdr.detectChanges();
      },
    });
  }

  onBookNow(serviceId?: number): void {
    const token = localStorage.getItem('access_token') || localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: '/booking', serviceId },
      });
    } else {
      this.router.navigate(['/booking'], {
        queryParams: serviceId ? { serviceId } : {},
      });
    }
  }
}
