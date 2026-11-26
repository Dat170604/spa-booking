import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLog, AuditLogService } from '../../../core/services/audit-log.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.component.html',
})
export class AuditLogsComponent implements OnInit {
  private auditLogService = inject(AuditLogService);
  private cdr = inject(ChangeDetectorRef);

  logs: AuditLog[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 15;
  isLoading = false;

  selectedLog: AuditLog | null = null;

  filters = {
    keyword: '',
    module: '',
    status: '',
    fromDate: '',
    toDate: '',
  };

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page: number = 0): void {
    this.currentPage = page;
    this.isLoading = true;
    this.cdr.detectChanges();

    this.auditLogService
      .getLogs({
        ...this.filters,
        page: this.currentPage,
        size: this.pageSize,
      })
      .subscribe({
        next: (res) => {
          this.logs = res.content || [];
          this.totalElements = res.totalElements || 0;
          this.totalPages = res.totalPages || 0;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  resetFilters(): void {
    this.filters = { keyword: '', module: '', status: '', fromDate: '', toDate: '' };
    this.loadLogs(0);
  }
}
