import { Component } from '@angular/core';

@Component({
  selector: 'app-shipping',
  standalone: true,
  template: `
    <div class="legal-page container">
      <h1>Kargo ve Teslimat</h1>
      <div class="content-block card">
        <h3>Kargo Süreçleri</h3>
        <p class="text-muted">Siparişleriniz sisteme düştükten sonra özenle hazırlanır ve anlaşmalı olduğumuz kargo firmalarına (Yurtiçi, Aras, MNG) teslim edilir. İstanbul içi teslimatlar ortalama 1-2 iş günü, diğer iller ise 2-4 iş günü sürmektedir.</p>
        
        <h3>Kargo Ücretleri</h3>
        <p class="text-muted">500 TL ve üzeri siparişlerinizde kargo ücretsizdir! 500 TL altındaki siparişleriniz için standart kargo ücreti 39.90 TL'dir.</p>
        
        <h3>Sipariş Takibi</h3>
        <p class="text-muted">Siparişiniz kargoya verildiğinde size bir takip numarası SMS ve e-posta yoluyla iletilir. Ayrıca "Siparişlerim" sayfasından kargo durumunuzu anlık olarak takip edebilirsiniz.</p>
      </div>
    </div>
  `,
  styles: [`
    .legal-page { padding: 80px 20px; max-width: 800px; margin: 0 auto; }
    h1 { margin-bottom: 40px; font-size: clamp(2rem, 4vw, 2.8rem); text-align: center; }
    .content-block { padding: 40px; }
    h3 { margin-top: 30px; margin-bottom: 12px; font-size: 1.25rem; color: var(--clr-primary-light); }
    h3:first-child { margin-top: 0; }
    p { line-height: 1.7; }
  `]
})
export class ShippingComponent {}
