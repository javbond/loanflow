import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Auth guard that checks if user is authenticated
 */
export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    // Store attempted URL for redirecting after login
    const returnUrl = route.url.map(segment => segment.path).join('/');
    router.navigate(['/login'], { queryParams: { returnUrl } });
    return false;
  }

  // Check for required roles
  const requiredRoles = route.data['roles'] as string[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    if (!authService.hasAnyRole(requiredRoles)) {
      // User doesn't have required role
      router.navigate(['/unauthorized']);
      return false;
    }
  }

  return true;
};

/**
 * Guest guard - only allow non-authenticated users (for login page)
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    router.navigate(['/']);
    return false;
  }

  return true;
};

/**
 * Role guard factory - creates guard for specific roles
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }

    if (!authService.hasAnyRole(allowedRoles)) {
      router.navigate(['/unauthorized']);
      return false;
    }

    return true;
  };
};
