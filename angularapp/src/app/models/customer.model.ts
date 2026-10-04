import { User } from './user.model';

export interface Customer {
  customerId?: number;
  customerName?: string;
  information?: string;
  mobileNumber?: string;
  user?: User;
}
