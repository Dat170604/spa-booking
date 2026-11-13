import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const currentUser = tokenService.getUserInfo();

  if (currentUser && currentUser.roles) {
    const hasRole = currentUser.roles.some(
      (role: string) => role === 'ROLE_ADMIN' || role === 'ROLE_STAFF',
    );
    if (hasRole) {
      return true;
    }
  }

  // Không có quyền thì chuyển hướng về trang chủ
  router.navigate(['/']);
  return false;
};
