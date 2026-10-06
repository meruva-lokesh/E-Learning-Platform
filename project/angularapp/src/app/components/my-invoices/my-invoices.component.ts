import { Component, OnInit } from '@angular/core';
import { Invoice } from '../../models/billing.model';
import { InvoiceService } from '../../services/invoice.service';

/** A customer's invoices, newest first, each with View and Download PDF. */
@Component({
  selector: 'app-my-invoices',
  templateUrl: './my-invoices.component.html',
  styleUrls: ['./my-invoices.component.css']
})
export class MyInvoicesComponent implements OnInit {
  invoices: Invoice[] = [];
  loaded = false;
  loadError = '';
  downloadError = '';
  busyId: number | null = null;

  constructor(private api: InvoiceService) {}

  ngOnInit(): void {
    this.api.mine().subscribe({
      next: list => { this.invoices = list; this.loaded = true; },
      error: err => {
        this.loaded = true;
        this.loadError = err.status === 404 ? 'Complete your profile first, then your invoices will appear here.' : 'Could not load your invoices. Please try again.';
      }
    });
  }

  download(i: Invoice): void {
    this.busyId = i.id;
    this.downloadError = '';
    this.api.downloadPdf(i).subscribe({
      next: blob => { this.api.saveBlob(blob, 'invoice-' + i.invoiceNumber + '.pdf'); this.busyId = null; },
      error: () => { this.busyId = null; this.downloadError = 'The PDF could not be downloaded. Please try again.'; }
    });
  }

  trackById(_: number, i: Invoice): number { return i.id; }
}
