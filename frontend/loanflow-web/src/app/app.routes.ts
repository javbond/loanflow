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

  // Dashboard route - redirects based on role
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/components/customer-dashboard/customer-dashboard.component')
      .then(m => m.CustomerDashboardComponent)
  },

  // Customer portal - accessible by CUSTOMER role
  {
    path: 'my-portal',
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER'] },
    children: [
      {
        path: '',
        loadComponent: () => import('./features/dashboard/components/customer-dashboard/customer-dashboard.component')
          .then(m => m.CustomerDashboardComponent)
      },
      {
        path: 'apply',
        loadComponent: () => import('./features/customer-portal/components/loan-application-form/loan-application-form.component')
          .then(m => m.CustomerLoanApplicationFormComponent)
      },
      {
        path: 'applications',
        loadComponent: () => import('./features/customer-portal/components/my-applications/my-applications.component')
          .then(m => m.MyApplicationsComponent)
      },
      {
        path: 'applications/:id',
        loadComponent: () => import('./features/customer-portal/components/application-detail/application-detail.component')
          .then(m => m.ApplicationDetailComponent)
      },
      {
        path: 'documents',
        loadComponent: () => import('./features/customer-portal/components/my-documents/my-documents.component')
          .then(m => m.MyDocumentsComponent)
      },
      {
        path: 'documents/upload',
        loadComponent: () => import('./features/customer-portal/components/customer-document-upload/customer-document-upload.component')
          .then(m => m.CustomerDocumentUploadComponent)
      },
      {
        path: 'documents/upload/:appId',
        loadComponent: () => import('./features/customer-portal/components/customer-document-upload/customer-document-upload.component')
          .then(m => m.CustomerDocumentUploadComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/customer-portal/components/customer-profile/customer-profile.component')
          .then(m => m.CustomerProfileComponent)
      }
    ]
  },

  // Protected routes - Staff Only
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
    path: 'policies',
    canActivate: [authGuard],
    data: { roles: ['ADMIN', 'SUPERVISOR'] },
    children: [
      {
        path: '',
        loadComponent: () => import('./features/policy/components/policy-list/policy-list.component')
          .then(m => m.PolicyListComponent)
      },
      {
        path: 'new',
        loadComponent: () => import('./features/policy/components/policy-form/policy-form.component')
          .then(m => m.PolicyFormComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/policy/components/policy-detail/policy-detail.component')
          .then(m => m.PolicyDetailComponent)
      },
      {
        path: ':id/edit',
        loadComponent: () => import('./features/policy/components/policy-form/policy-form.component')
          .then(m => m.PolicyFormComponent)
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
