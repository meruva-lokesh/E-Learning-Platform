import { Course } from './course.model';
import { Customer } from './customer.model';

export interface Cart {
  cartId?: number;
  customer?: Customer;
  courses?: Course[];
  totalAmount?: number;
}
