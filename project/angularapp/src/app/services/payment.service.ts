import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API, API_URL } from '../constant';

/** What Razorpay Checkout gives the browser after a successful payment. */
export interface RazorpaySuccess {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

/** Why Checkout ended without a payment. */
export interface RazorpayStop {
  reason: 'dismissed' | 'failed';
  description?: string;
}

/**
 * Razorpay payment for the cart.
 *  1. createRazorpayOrder(): the SERVER prices the cart and creates the Razorpay order.
 *  2. openCheckout(): Razorpay's own pop-up (card, UPI, netbanking...). We never see card details.
 *  3. verifyRazorpayPayment(): the SERVER checks Razorpay's signature and only then saves the order.
 */
@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private static readonly SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js';
  private apiUrl = API_URL;
  private scriptLoaded: Promise<void> | null = null;

  constructor(private http: HttpClient) {}

  /** True when environment.razorpayEnabled is switched on. */
  get enabled(): boolean {
    return !!environment.razorpayEnabled;
  }

  createRazorpayOrder(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.PAYMENT_RAZORPAY_ORDER}`, {});
  }

  verifyRazorpayPayment(success: RazorpaySuccess): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.PAYMENT_RAZORPAY_VERIFY}`, {
      razorpayOrderId: success.razorpay_order_id,
      razorpayPaymentId: success.razorpay_payment_id,
      razorpaySignature: success.razorpay_signature
    });
  }

  private loadScript(): Promise<void> {
    if (!this.scriptLoaded) {
      this.scriptLoaded = new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.src = PaymentService.SCRIPT_URL;
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => {
          this.scriptLoaded = null;   // allow a retry
          reject(new Error('Could not load Razorpay Checkout'));
        };
        document.head.appendChild(script);
      });
    }
    return this.scriptLoaded;
  }

  /** Opens Razorpay Checkout. Resolves with the success data, or rejects with a RazorpayStop. */
  async openCheckout(options: any): Promise<RazorpaySuccess> {
    await this.loadScript();
    return new Promise<RazorpaySuccess>((resolve, reject) => {
      const Razorpay = (window as any).Razorpay;
      const checkout = new Razorpay({
        ...options,
        handler: (response: RazorpaySuccess) => resolve(response),
        modal: { ondismiss: () => reject({ reason: 'dismissed' } as RazorpayStop) }
      });
      checkout.on('payment.failed', (response: any) =>
        reject({ reason: 'failed', description: response?.error?.description } as RazorpayStop));
      checkout.open();
    });
  }
}
