import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
  isLoading = true;
  errorMessage = '';

  constructor(private branchService: BranchService) {}

  ngOnInit(): void {
    this.loadActiveBranches();
  }

  loadActiveBranches(): void {
    this.isLoading = true;
    this.branchService.getActiveBranches().subscribe({
      next: (data) => {
        this.branches = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Không thể tải danh sách chi nhánh. Vui lòng thử lại sau!';
        this.isLoading = false;
      },
    });
  }
}
