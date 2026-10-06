import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CustomerViewCoursesComponent } from '../../components/customer-view-courses/customer-view-courses.component';
import { MyCartComponent } from '../../components/my-cart/my-cart.component';
import { PlaceOrderComponent } from '../../components/place-order/place-order.component';
import { MyOrdersComponent } from '../../components/my-orders/my-orders.component';
import { OrderDetailsComponent } from '../../components/order-details/order-details.component';
import { AddReviewComponent } from '../../components/add-review/add-review.component';
import { CustomerdashboardComponent } from '../../components/customerdashboard/customerdashboard.component';
import { CustomerProfileComponent } from '../../components/customer-profile/customer-profile.component';

const routes: Routes = [
  { path: 'customerdashboard', component: CustomerdashboardComponent },
  { path: 'customer-profile', component: CustomerProfileComponent },
  { path: 'customer-view-courses', component: CustomerViewCoursesComponent },
  { path: 'my-cart', component: MyCartComponent },
  { path: 'place-order', component: PlaceOrderComponent },
  { path: 'my-orders', component: MyOrdersComponent },
  { path: 'order-details/:id', component: OrderDetailsComponent },
  { path: 'add-review', component: AddReviewComponent }
];

/** Pages only a customer can open. Lazy loaded and protected by AuthGuard (role CUSTOMER) in app-routing. @author Meruva Lokesh */
@NgModule({
  declarations: [
    CustomerViewCoursesComponent, MyCartComponent, PlaceOrderComponent, MyOrdersComponent,
    OrderDetailsComponent, AddReviewComponent, CustomerdashboardComponent, CustomerProfileComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CustomerModule {}
