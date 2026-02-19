import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'customers',
    pathMatch: 'full'
  },
  {
    path: 'customers',
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
    children: [
      {
        path: '',
        redirectTo: 'upload',
        pathMatch: 'full'
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
  }
];
