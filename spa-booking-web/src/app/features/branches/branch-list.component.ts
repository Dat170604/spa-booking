import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { BranchService } from '../../core/services/branch.service';

@Component({
  selector: 'app-branch-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './branch-list.component.html',
  styleUrls: ['./branch-list.component.scss'],
})
export class BranchListComponent implements OnInit {
  branches: any[] = [];
  filteredBranches: any[] = [];
  searchKeyword = '';
  isLoading = true;

  constructor(
    private branchService: BranchService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadBranches();
  }

  loadBranches(): void {
    this.isLoading = true;
    this.branchService.getActiveBranches().subscribe({
      next: (res: any) => {
        this.branches = res?.content || (Array.isArray(res) ? res : []);
        this.filteredBranches = [...this.branches];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSearch(event: Event): void {
    const keyword = (event.target as HTMLInputElement).value.toLowerCase().trim();
    this.searchKeyword = keyword;
    if (!keyword) {
      this.filteredBranches = [...this.branches];
      return;
    }
    this.filteredBranches = this.branches.filter(
      (b) => b.name?.toLowerCase().includes(keyword) || b.address?.toLowerCase().includes(keyword),
    );
  }

  bookAtBranch(branchId: number): void {
    const token = localStorage.getItem('access_token') || localStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: '/booking', branchId },
      });
    } else {
      this.router.navigate(['/booking'], { queryParams: { branchId } });
    }
  }
}
