import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { BranchService } from '../../../core/services/branch.service';
import { SpaServiceService } from '../../../core/services/spa-service.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  isLoading = true;
  todayStr = '';

  // Chỉ số KPI tổng quan
  totalRevenue = 0;
  estimatedRevenue = 0;
  totalAppointments = 0;
  pendingCount = 0;
  confirmedCount = 0;
  completedCount = 0;
  cancelledCount = 0;
  activeBranchesCount = 0;
  activeServicesCount = 0;

  // Danh sách hiển thị nhanh
  todayAppointments: any[] = [];
  pendingAppointments: any[] = [];
  topServices: { name: string; count: number; revenue: number }[] = [];

  // Dữ liệu biểu đồ 7 ngày gần nhất
  last7DaysStats: {
    label: string;
    date: string;
    count: number;
    revenue: number;
    heightPercent: number;
  }[] = [];

  constructor(
    private appointmentService: AppointmentService,
    private branchService: BranchService,
    private spaService: SpaServiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.todayStr = today.toISOString().split('T')[0];
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;

    forkJoin({
      appointmentsRes: this.appointmentService.searchAppointments(
        '',
        '',
        undefined,
        undefined,
        0,
        500,
      ),
      branchesRes: this.branchService.searchBranches('', 0, 100),
      servicesRes: this.spaService.getServices('', '', 0, 100),
    }).subscribe({
      next: ({ appointmentsRes, branchesRes, servicesRes }) => {
        const appointments: any[] =
          appointmentsRes?.content || (Array.isArray(appointmentsRes) ? appointmentsRes : []);
        const branches: any[] =
          branchesRes?.content || (Array.isArray(branchesRes) ? branchesRes : []);
        const services: any[] =
          servicesRes?.content || (Array.isArray(servicesRes) ? servicesRes : []);

        this.activeBranchesCount = branches.filter((b) => b.active).length;
        this.activeServicesCount = services.filter(
          (s) => s.status === 'ACTIVE' || s.active === true,
        ).length;

        this.calculateMetrics(appointments);
        this.buildLast7DaysChart(appointments);
        this.calculateTopServices(appointments);

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tải dữ liệu tổng quan:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private calculateMetrics(appointments: any[]): void {
    this.totalAppointments = appointments.length;

    this.pendingCount = appointments.filter((a) => a.status === 'PENDING').length;
    this.confirmedCount = appointments.filter((a) => a.status === 'CONFIRMED').length;
    this.completedCount = appointments.filter((a) => a.status === 'COMPLETED').length;
    this.cancelledCount = appointments.filter((a) => a.status === 'CANCELLED').length;

    // Doanh thu thực tế từ các đơn đã hoàn thành
    this.totalRevenue = appointments
      .filter((a) => a.status === 'COMPLETED')
      .reduce((sum, a) => sum + Number(a.totalAmount || a.totalPrice || 0), 0);

    // Doanh thu dự kiến từ các đơn đã xác nhận nhưng chưa hoàn thành
    this.estimatedRevenue = appointments
      .filter((a) => a.status === 'CONFIRMED')
      .reduce((sum, a) => sum + Number(a.totalAmount || a.totalPrice || 0), 0);

    // Lịch hẹn diễn ra trong hôm nay (sắp xếp theo giờ tăng dần)
    this.todayAppointments = appointments
      .filter((a) => a.appointmentDate === this.todayStr && a.status !== 'CANCELLED')
      .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
      .slice(0, 6);

    // Các đơn đang chờ duyệt mới nhất
    this.pendingAppointments = appointments.filter((a) => a.status === 'PENDING').slice(0, 5);
  }

  private buildLast7DaysChart(appointments: any[]): void {
    const days: {
      label: string;
      date: string;
      count: number;
      revenue: number;
      heightPercent: number;
    }[] = [];

    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const isoDate = d.toISOString().split('T')[0];
      const label = `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}`;

      const dayApps = appointments.filter(
        (a) => a.appointmentDate === isoDate && a.status !== 'CANCELLED',
      );
      const dayRev = dayApps.reduce(
        (sum, a) => sum + Number(a.totalAmount || a.totalPrice || 0),
        0,
      );

      days.push({
        label,
        date: isoDate,
        count: dayApps.length,
        revenue: dayRev,
        heightPercent: 0,
      });
    }

    const maxCount = Math.max(...days.map((d) => d.count), 1);
    days.forEach((d) => {
      d.heightPercent = d.count === 0 ? 8 : Math.max(18, Math.round((d.count / maxCount) * 100));
    });

    this.last7DaysStats = days;
  }

  private calculateTopServices(appointments: any[]): void {
    const serviceMap = new Map<string, { name: string; count: number; revenue: number }>();

    appointments
      .filter((a) => a.status !== 'CANCELLED')
      .forEach((a) => {
        if (Array.isArray(a.details) && a.details.length > 0) {
          a.details.forEach((d: any) => {
            const name = d.serviceName || 'Dịch vụ khác';
            const current = serviceMap.get(name) || { name, count: 0, revenue: 0 };
            current.count += 1;
            current.revenue += Number(d.price || 0);
            serviceMap.set(name, current);
          });
        } else if (a.serviceName) {
          const current = serviceMap.get(a.serviceName) || {
            name: a.serviceName,
            count: 0,
            revenue: 0,
          };
          current.count += 1;
          current.revenue += Number(a.totalAmount || a.totalPrice || 0);
          serviceMap.set(a.serviceName, current);
        }
      });

    this.topServices = Array.from(serviceMap.values())
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);
  }

  quickConfirm(appointment: any): void {
    this.appointmentService.updateStatus(appointment.id, 'CONFIRMED').subscribe({
      next: () => {
        this.loadDashboardData();
      },
      error: (err) => {
        alert(err?.error?.message || 'Không thể xác nhận lịch hẹn!');
      },
    });
  }

  getPercentage(count: number): number {
    if (this.totalAppointments === 0) return 0;
    return Math.round((count / this.totalAppointments) * 100);
  }

  getStatusBadge(status: string): { text: string; classes: string } {
    switch (status) {
      case 'PENDING':
        return {
          text: 'Chờ xác nhận',
          classes: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
        };
      case 'CONFIRMED':
        return { text: 'Đã xác nhận', classes: 'bg-sky-500/15 text-sky-400 border-sky-500/30' };
      case 'COMPLETED':
        return {
          text: 'Hoàn thành',
          classes: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
        };
      case 'CANCELLED':
        return { text: 'Đã hủy', classes: 'bg-rose-500/15 text-rose-400 border-rose-500/30' };
      default:
        return { text: status, classes: 'bg-slate-700 text-slate-300 border-slate-600' };
    }
  }
}
