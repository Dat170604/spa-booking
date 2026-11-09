import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BranchService } from '../../core/services/branch.service';
import { SpaServiceService } from '../../core/services/spa-service.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit {
  branches: any[] = [];
  services: any[] = [];
  isLoading = true;

  constructor(
    private branchService: BranchService,
    private spaService: SpaServiceService,
  ) {}

  ngOnInit(): void {
    this.loadHomeData();
  }

  private loadHomeData(): void {
    this.branchService.getActiveBranches().subscribe({
      next: (data) => (this.branches = data.slice(0, 3)), // Lấy tối đa 3 chi nhánh tiêu biểu
      error: () => (this.branches = []),
    });

    this.spaService.getAllServices().subscribe({
      next: (data) => {
        this.services = data.slice(0, 4); // Lấy 4 dịch vụ nổi bật
        this.isLoading = false;
      },
      error: () => (this.isLoading = false),
    });
  }
}
