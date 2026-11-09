import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  // Dữ liệu mẫu thống kê tổng quan
  stats = [
    { title: 'Tổng Đặt Lịch Hôm Nay', value: '24', icon: '📅', change: '+12%', isPositive: true },
    { title: 'Doanh Thu Ngày', value: '8,500,000đ', icon: '💰', change: '+8%', isPositive: true },
    { title: 'Khách Hàng Mới', value: '12', icon: '👥', change: '+15%', isPositive: true },
    { title: 'Chi Nhánh Hoạt Động', value: '4/4', icon: '🏢', change: '100%', isPositive: true },
  ];

  // Danh sách lịch hẹn gần nhất
  recentAppointments = [
    {
      id: 'BK-001',
      customer: 'Nguyễn Văn A',
      service: 'Massage Thụy Điển',
      time: '14:00 Today',
      status: 'CONFIRMED',
      staff: 'Trần Thị B',
    },
    {
      id: 'BK-002',
      customer: 'Lê Thị C',
      service: 'Chăm sóc da mặt chuyên sâu',
      time: '15:30 Today',
      status: 'PENDING',
      staff: 'Chưa gán',
    },
    {
      id: 'BK-003',
      customer: 'Phạm Văn D',
      service: 'Tắm trắng thảo dược',
      time: '16:00 Today',
      status: 'COMPLETED',
      staff: 'Nguyễn Thị E',
    },
  ];

  constructor() {}

  ngOnInit(): void {}
}
