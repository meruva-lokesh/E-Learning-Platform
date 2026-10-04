import { Course } from './course.model';
import { Customer } from './customer.model';

export interface Order {
  orderId?: number;
  orderPrice?: number;
  courses?: Course[];
  customer?: Customer;
}
