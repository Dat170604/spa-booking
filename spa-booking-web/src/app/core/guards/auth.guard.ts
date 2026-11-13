import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

export const authGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const currentUser = tokenService.getUserInfo();

  if (currentUser) {
    return true;
  }

  // Chưa đăng nhập -> chuyển sang trang login và lưu lại URL định vào để quay lại sau
  router.navigate(['/login'], {
    queryParams: { returnUrl: state.url },
  });
  return false;
};
