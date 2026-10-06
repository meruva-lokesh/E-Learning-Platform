/** Numbers shown on the dashboards (GET /api/dashboard/customer/{id} and /api/dashboard/admin). */
export interface CustomerStats {
  enrolledCourses?: number;
  ordersPlaced?: number;
  cartItems?: number;
  totalSpent?: number;
  reviewsGiven?: number;
}

export interface AdminStats {
  totalCourses?: number;
  totalCustomers?: number;
  totalOrders?: number;
  totalRevenue?: number;
  totalReviews?: number;
  averageRating?: number;
}
