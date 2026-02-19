import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { UserInfo } from '../../../../core/auth/models/auth.model';

/**
 * Customer Profile Component
 * Displays customer's profile information from Keycloak
 * Issue: #30 [US-028] Customer Profile Management
 */
@Component({
  selector: 'app-customer-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule
  ],
  template: `
    <div class="profile-container">
      <div class="header-section">
        <button mat-icon-button routerLink="/my-portal" class="back-btn">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <h1><mat-icon>person</mat-icon> My Profile</h1>
      </div>

      <div class="profile-grid">
        <!-- Profile Card -->
        <mat-card class="profile-card main-card">
          <mat-card-content>
            <div class="avatar-section">
              <div class="avatar">
                <mat-icon>person</mat-icon>
              </div>
              <h2>{{ user?.fullName || 'Customer' }}</h2>
              <p class="email">{{ user?.email }}</p>
              <mat-chip-set>
                @for (role of user?.roles; track role) {
                  <mat-chip color="primary" highlighted>{{ formatRole(role) }}</mat-chip>
                }
              </mat-chip-set>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Account Details Card -->
        <mat-card class="profile-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon">account_circle</mat-icon>
            <mat-card-title>Account Information</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <mat-list>
              <mat-list-item>
                <mat-icon matListItemIcon>badge</mat-icon>
                <span matListItemTitle>Full Name</span>
                <span matListItemLine>{{ user?.fullName || 'Not provided' }}</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>person</mat-icon>
                <span matListItemTitle>First Name</span>
                <span matListItemLine>{{ user?.firstName || 'Not provided' }}</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>person_outline</mat-icon>
                <span matListItemTitle>Last Name</span>
                <span matListItemLine>{{ user?.lastName || 'Not provided' }}</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>email</mat-icon>
                <span matListItemTitle>Email Address</span>
                <span matListItemLine>{{ user?.email || 'Not provided' }}</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>fingerprint</mat-icon>
                <span matListItemTitle>User ID</span>
                <span matListItemLine class="mono">{{ user?.id || 'Not available' }}</span>
              </mat-list-item>
            </mat-list>
          </mat-card-content>
        </mat-card>

        <!-- Security Card -->
        <mat-card class="profile-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon security-icon">security</mat-icon>
            <mat-card-title>Security Settings</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="security-info">
              <p class="info-text">
                <mat-icon>info</mat-icon>
                Your account is managed through our secure identity provider.
                To change your password or update security settings, you'll be
                redirected to the secure login portal.
              </p>
              <button mat-stroked-button color="primary" (click)="changePassword()">
                <mat-icon>lock</mat-icon> Change Password
              </button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Quick Actions Card -->
        <mat-card class="profile-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon actions-icon">flash_on</mat-icon>
            <mat-card-title>Quick Actions</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="quick-actions-grid">
              <button mat-stroked-button color="primary" routerLink="/my-portal/apply">
                <mat-icon>add_circle</mat-icon>
                <span>Apply for Loan</span>
              </button>
              <button mat-stroked-button color="primary" routerLink="/my-portal/applications">
                <mat-icon>assignment</mat-icon>
                <span>My Applications</span>
              </button>
              <button mat-stroked-button color="primary" routerLink="/my-portal/documents">
                <mat-icon>folder_open</mat-icon>
                <span>My Documents</span>
              </button>
              <button mat-stroked-button color="primary" routerLink="/my-portal/documents/upload">
                <mat-icon>cloud_upload</mat-icon>
                <span>Upload Document</span>
              </button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Help Card -->
        <mat-card class="profile-card help-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon help-icon">help_outline</mat-icon>
            <mat-card-title>Need Help?</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <mat-list>
              <mat-list-item>
                <mat-icon matListItemIcon>email</mat-icon>
                <span matListItemTitle>Email Support</span>
                <span matListItemLine>support&#64;loanflow.com</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>phone</mat-icon>
                <span matListItemTitle>Phone Support</span>
                <span matListItemLine>1800-LOAN-FLOW (Toll Free)</span>
              </mat-list-item>
              <mat-divider></mat-divider>
              <mat-list-item>
                <mat-icon matListItemIcon>schedule</mat-icon>
                <span matListItemTitle>Business Hours</span>
                <span matListItemLine>Mon-Fri: 9:00 AM - 6:00 PM IST</span>
              </mat-list-item>
            </mat-list>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .profile-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .header-section { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
    .header-section h1 { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.75rem; color: #1976d2; }
    .back-btn { flex-shrink: 0; }
    .profile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 24px; }
    .profile-card { height: fit-content; }
    .main-card { grid-column: 1 / -1; }
    @media (min-width: 768px) { .main-card { grid-column: span 1; } }
    .avatar-section { text-align: center; padding: 24px; }
    .avatar { width: 100px; height: 100px; border-radius: 50%; background: linear-gradient(135deg, #1976d2, #42a5f5); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; }
    .avatar mat-icon { font-size: 56px; width: 56px; height: 56px; color: white; }
    .avatar-section h2 { margin: 0 0 8px; color: #333; }
    .avatar-section .email { margin: 0 0 16px; color: #666; }
    .card-icon { background: #1976d2; color: white; border-radius: 50%; padding: 8px; font-size: 24px; width: 40px; height: 40px; }
    .security-icon { background: #ff9800; }
    .actions-icon { background: #4caf50; }
    .help-icon { background: #9c27b0; }
    mat-list-item { height: auto !important; min-height: 56px; }
    .mono { font-family: monospace; font-size: 0.85rem; }
    .security-info { padding: 16px 0; }
    .info-text { display: flex; gap: 12px; padding: 16px; background: #e3f2fd; border-radius: 8px; color: #1565c0; margin-bottom: 16px; font-size: 0.9rem; line-height: 1.5; }
    .info-text mat-icon { flex-shrink: 0; }
    .quick-actions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 8px 0; }
    .quick-actions-grid button { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px; height: auto; }
    .quick-actions-grid button span { font-size: 0.85rem; }
    @media (max-width: 600px) { .profile-grid { grid-template-columns: 1fr; } .quick-actions-grid { grid-template-columns: 1fr; } }
  `]
})
export class CustomerProfileComponent implements OnInit {
  private authService = inject(AuthService);
  user: UserInfo | null = null;

  ngOnInit(): void {
    this.user = this.authService.getCurrentUserSync();
  }

  formatRole(role: string): string {
    return role.replace(/_/g, ' ').replace(/ROLE_/i, '');
  }

  changePassword(): void {
    // Redirect to Keycloak account console for password change
    // In production, this would be the Keycloak account URL
    const keycloakAccountUrl = '/auth/realms/loanflow/account/password';
    window.open(keycloakAccountUrl, '_blank');
  }
}
