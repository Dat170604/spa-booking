import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SpaServiceService } from '../../core/services/spa-service.service';

@Component({
  selector: 'app-services-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './services-list.component.html',
  styleUrls: ['./services-list.component.scss'],
})
export class ServicesListComponent implements OnInit {
  services: any[] = [];
  filteredServices: any[] = [];
  selectedCategory = 'ALL';
  isLoading = true;

  constructor(private spaService: SpaServiceService) {}

  ngOnInit(): void {
    this.loadServices();
  }

  loadServices(): void {
    this.isLoading = true;
    this.spaService.getAllServices().subscribe({
      next: (data) => {
        this.services = data;
        this.filteredServices = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  filterByCategory(category: string): void {
    this.selectedCategory = category;
    if (category === 'ALL') {
      this.filteredServices = this.services;
    } else {
      this.filteredServices = this.services.filter((s) => s.category === category);
    }
  }
}
