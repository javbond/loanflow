import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="unauthorized-container">
      <mat-card class="unauthorized-card">
        <mat-icon class="error-icon">lock</mat-icon>
        <h1>Access Denied</h1>
        <p>You don't have permission to access this page.</p>
        <p>Please contact your administrator if you believe this is an error.</p>
        <div class="actions">
          <a mat-raised-button color="primary" routerLink="/">Go to Home</a>
          <a mat-stroked-button routerLink="/login">Sign in with different account</a>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .unauthorized-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: #f5f5f5;
    }

    .unauthorized-card {
      text-align: center;
      padding: 48px;
      max-width: 500px;
    }

    .error-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #f44336;
    }

    h1 {
      margin: 24px 0 16px;
      color: #333;
    }

    p {
      color: #666;
      margin-bottom: 8px;
    }

    .actions {
      margin-top: 32px;
      display: flex;
      gap: 16px;
      justify-content: center;
    }
  `]
})
export class UnauthorizedComponent {}
