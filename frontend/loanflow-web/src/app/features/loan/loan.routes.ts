import { Routes } from '@angular/router';
import { LoanListComponent } from './components/loan-list/loan-list.component';
import { LoanFormComponent } from './components/loan-form/loan-form.component';
import { LoanDetailComponent } from './components/loan-detail/loan-detail.component';

export const LOAN_ROUTES: Routes = [
  {
    path: '',
    component: LoanListComponent
  },
  {
    path: 'new',
    component: LoanFormComponent
  },
  {
    path: ':id',
    component: LoanDetailComponent
  },
  {
    path: ':id/edit',
    component: LoanFormComponent
  }
];
