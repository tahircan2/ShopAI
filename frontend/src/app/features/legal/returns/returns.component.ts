import { Component } from '@angular/core';

@Component({
  selector: 'app-returns',
  standalone: true,
  template: `
    <div class="legal-page container">
      <h1>İade Koşulları</h1>
      <div class="content-block card">
        <h3>14 Günlük İade Hakkı</h3>
        <p class="text-muted">ShopAI'den aldığınız ürünleri, teslimat tarihinden itibaren 14 gün içinde hiçbir gerekçe göstermeksizin iade edebilirsiniz. İade edilecek ürünün kullanılmamış, etiketi koparılmamış ve tekrar satılabilir özelliğini yitirmemiş olması gerekmektedir.</p>
        
        <h3>İade Süreci</h3>
        <ul class="text-muted">
          <li>Hesabınızdan "Siparişlerim" sekmesine gidin ve ilgili siparişi seçip "İade Et" butonuna tıklayın.</li>
          <li>İade kodunuzu alın.</li>
          <li>Ürünü orijinal faturası ve ambalajıyla birlikte anlaşmalı kargo şubesine bu kod ile ücretsiz teslim edin.</li>
        </ul>
        
        <h3>Geri Ödeme</h3>
        <p class="text-muted">İade ettiğiniz ürün depomuza ulaşıp kalite kontrolünden geçtikten sonra, ücret iadeniz 3-5 iş günü içerisinde ödeme yaptığınız karta veya hesaba aktarılır.</p>
      </div>
    </div>
  `,
  styles: [`
    .legal-page { padding: 80px 20px; max-width: 800px; margin: 0 auto; }
    h1 { margin-bottom: 40px; font-size: clamp(2rem, 4vw, 2.8rem); text-align: center; }
    .content-block { padding: 40px; }
    h3 { margin-top: 30px; margin-bottom: 12px; font-size: 1.25rem; color: var(--clr-primary-light); }
    h3:first-child { margin-top: 0; }
    p, ul { line-height: 1.7; }
    ul { padding-left: 20px; margin-bottom: 16px; }
    li { margin-bottom: 8px; }
  `]
})
export class ReturnsComponent {}
