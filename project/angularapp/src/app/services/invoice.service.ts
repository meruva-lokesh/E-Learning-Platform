import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { Invoice } from '../models/billing.model';

/** Invoices of paid orders: list, read, and download as a PDF file. */
@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  mine(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}${API.INVOICE}/my`);
  }

  byId(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}${API.INVOICE}/${id}`);
  }

  byOrder(orderId: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}${API.INVOICE}/by-order/${orderId}`);
  }

  /** The PDF travels with the login token (the interceptor adds it), so it is fetched as a blob and then saved. */
  downloadPdf(invoice: Invoice): Observable<Blob> {
    return this.http.get(`${this.apiUrl}${API.INVOICE}/${invoice.id}/pdf`, { responseType: 'blob' });
  }

  /** Saves a blob as a file with the given name. */
  saveBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
