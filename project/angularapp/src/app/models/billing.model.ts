/** Money is always a whole number of paise (100 paise = 1 rupee). Divide by 100 to show rupees. */

export interface InvoiceLine { courseId: number; title: string; paise: number; }

export interface Invoice {
  id: number;
  invoiceNumber: string;
  paymentId: number;
  orderId: number | null;
  razorpayPaymentId: string | null;
  customerName: string;
  customerEmail: string;
  issuedAt: string;
  lines: InvoiceLine[];
  taxablePaise: number;
  taxPaise: number;
  totalPaise: number;
  gstPercent: number;
  sellerName: string;
  sellerAddress: string;
  sellerGstin: string;
}

export interface EarningRow {
  id: number;
  courseId: number;
  courseTitle: string;
  paymentId: number;
  grossPaise: number;
  feePaise: number;
  ownerPaise: number;
  status: 'PENDING' | 'PAID_OUT';
  createdAt: string;
}

export interface PayoutView {
  id: number;
  instructorUserId: number;
  instructorName: string;
  amountPaise: number;
  earningsCount: number;
  reference: string;
  paidByEmail: string;
  paidAt: string;
}

export interface EarningsSummary {
  totalPaise: number;
  pendingPaise: number;
  paidOutPaise: number;
  commissionPercent: number;
  earnings: EarningRow[];
  payouts: PayoutView[];
}

export interface PayableRow {
  instructorUserId: number;
  name: string;
  email: string;
  mobile: string;
  pendingPaise: number;
  pendingCount: number;
}
