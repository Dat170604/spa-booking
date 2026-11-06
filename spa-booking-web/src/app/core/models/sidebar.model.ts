export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[]; // Quyền được xem menu này
}
