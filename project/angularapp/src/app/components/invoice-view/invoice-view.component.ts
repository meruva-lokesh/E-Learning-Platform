import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Invoice } from '../../models/billing.model';
import { InvoiceService } from '../../services/invoice.service';

/**
 * One invoice on screen. Reached as /invoice/:id (from My invoices) or /invoice/order/:orderId
 * (right after paying, or from an order). The PDF button downloads the same invoice as a file.
 */
@Component({
  selector: 'app-invoice-view',
  templateUrl: './invoice-view.component.html',
  styleUrls: ['./invoice-view.component.css']
})
export class InvoiceViewComponent implements OnInit {
  invoice: Invoice | null = null;
  loaded = false;
  notFound = false;
  loadError = '';
  downloadError = '';
  busy = false;

  constructor(private route: ActivatedRoute, private api: InvoiceService) {}

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('orderId');
    const id = this.route.snapshot.paramMap.get('id');
    const call = orderId ? this.api.byOrder(Number(orderId)) : this.api.byId(Number(id));
    call.subscribe({
      next: inv => { this.invoice = inv; this.loaded = true; },
      error: err => {
        this.loaded = true;
        if (err.status === 404) { this.notFound = true; }
        else { this.loadError = 'Could not load this invoice. Please try again.'; }
      }
    });
  }

  get addressLines(): string[] {
    return (this.invoice?.sellerAddress || '').split('\n').map(s => s.trim()).filter(s => !!s);
  }

  download(): void {
    if (!this.invoice) { return; }
    const inv = this.invoice;
    this.busy = true;
    this.downloadError = '';
    this.api.downloadPdf(inv).subscribe({
      next: blob => { this.api.saveBlob(blob, 'invoice-' + inv.invoiceNumber + '.pdf'); this.busy = false; },
      error: () => { this.busy = false; this.downloadError = 'The PDF could not be downloaded. Please try again.'; }
    });
  }
}
