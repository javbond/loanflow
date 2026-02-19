import { Routes } from '@angular/router';

export const documentRoutes: Routes = [
  {
    path: '',
    redirectTo: 'upload',
    pathMatch: 'full'
  },
  {
    path: 'upload',
    loadComponent: () => import('./components/document-upload/document-upload.component')
      .then(m => m.DocumentUploadComponent),
    title: 'Upload Document'
  },
  {
    path: 'upload/:appId',
    loadComponent: () => import('./components/document-upload/document-upload.component')
      .then(m => m.DocumentUploadComponent),
    title: 'Upload Document'
  },
  {
    path: 'application/:appId',
    loadComponent: () => import('./components/document-list/document-list.component')
      .then(m => m.DocumentListComponent),
    title: 'Application Documents'
  },
  {
    path: ':id',
    loadComponent: () => import('./components/document-viewer/document-viewer.component')
      .then(m => m.DocumentViewerComponent),
    title: 'View Document'
  }
];
