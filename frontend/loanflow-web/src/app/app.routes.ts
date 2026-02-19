import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  // Public routes
  {
    path: 'login',
    loadComponent: () => import('./core/auth/components/login/login.component')
      .then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },

  // Protected routes
  {
    path: '',
    redirectTo: 'customers',
    pathMatch: 'full'
  },
  {
    path: 'customers',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'LOAN_OFFICER', 'UNDERWRITER'] },
    children: [
      {
        path: '',
        loadComponent: () => import('./features/customer/components/customer-list/customer-list.component')
          .then(m => m.CustomerListComponent)
      },
      {
        path: 'new',
        loadComponent: () => import('./features/customer/components/customer-form/customer-form.component')
          .then(m => m.CustomerFormComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/customer/components/customer-detail/customer-detail.component')
          .then(m => m.CustomerDetailComponent)
      },
      {
        path: ':id/edit',
        loadComponent: () => import('./features/customer/components/customer-form/customer-form.component')
          .then(m => m.CustomerFormComponent)
      }
    ]
  },
  {
    path: 'loans',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'LOAN_OFFICER', 'UNDERWRITER'] },
    children: [
      {
        path: '',
        loadComponent: () => import('./features/loan/components/loan-list/loan-list.component')
          .then(m => m.LoanListComponent)
      },
      {
        path: 'new',
        loadComponent: () => import('./features/loan/components/loan-form/loan-form.component')
          .then(m => m.LoanFormComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/loan/components/loan-detail/loan-detail.component')
          .then(m => m.LoanDetailComponent)
      },
      {
        path: ':id/edit',
        loadComponent: () => import('./features/loan/components/loan-form/loan-form.component')
          .then(m => m.LoanFormComponent)
      }
    ]
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'LOAN_OFFICER', 'UNDERWRITER'] },
    children: [
      {
        path: '',
        loadComponent: () => import('./features/document/components/document-list/document-list.component')
          .then(m => m.DocumentListComponent)
      },
      {
        path: 'upload',
        loadComponent: () => import('./features/document/components/document-upload/document-upload.component')
          .then(m => m.DocumentUploadComponent)
      },
      {
        path: 'upload/:appId',
        loadComponent: () => import('./features/document/components/document-upload/document-upload.component')
          .then(m => m.DocumentUploadComponent)
      },
      {
        path: 'application/:appId',
        loadComponent: () => import('./features/document/components/document-list/document-list.component')
          .then(m => m.DocumentListComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/document/components/document-viewer/document-viewer.component')
          .then(m => m.DocumentViewerComponent)
      }
    ]
  },

  // Unauthorized page
  {
    path: 'unauthorized',
    loadComponent: () => import('./shared/components/unauthorized/unauthorized.component')
      .then(m => m.UnauthorizedComponent)
  },

  // Catch all - redirect to login
  {
    path: '**',
    redirectTo: 'login'
  }
];
