import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';

import { AuthService } from './core/auth/services/auth.service';
import { map } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  title = 'LoanFlow';

  // Auth state
  isAuthenticated$ = this.authService.isAuthenticated$;
  currentUser$ = this.authService.currentUser$;

  // Role-based navigation
  isCustomer$ = this.currentUser$.pipe(
    map(user => user?.roles?.includes('CUSTOMER') ?? false)
  );
  isStaff$ = this.currentUser$.pipe(
    map(user => user?.roles?.some(r => ['ADMIN', 'LOAN_OFFICER', 'UNDERWRITER'].includes(r)) ?? false)
  );

  logout(): void {
    this.authService.logout();
  }

  isLoginPage(): boolean {
    return this.router.url === '/login' || this.router.url.startsWith('/login?');
  }
}
