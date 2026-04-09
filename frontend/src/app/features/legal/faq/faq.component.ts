import { Component } from '@angular/core';

@Component({
  selector: 'app-faq',
  standalone: true,
  template: `
    <div class="legal-page container">
      <h1>Sıkça Sorulan Sorular</h1>
      <p class="text-muted text-center mb-loose">Alışveriş sürecinizle ilgili en çok merak edilen soruların cevaplarını burada bulabilirsiniz.</p>
      
      <div class="faq-list">
        <div class="faq-item card card-hover">
          <h3>Siparişim ne zaman kargoya verilir?</h3>
          <p class="text-muted">Siparişleriniz onaylandıktan sonra stok durumuna göre genellikle 1-3 iş günü içerisinde kargoya teslim edilmektedir.</p>
        </div>
        <div class="faq-item card card-hover">
          <h3>İade süresi kaç gündür?</h3>
          <p class="text-muted">Yasal iade hakkınız olan 14 gün içerisinde, hiç kullanılmamış ve orijinal ambalajı bozulmamış ürünleri iade edebilirsiniz.</p>
        </div>
        <div class="faq-item card card-hover">
          <h3>Hangi ödeme yöntemlerini kullanabilirim?</h3>
          <p class="text-muted">Kredi kartı, banka kartı ve havale/EFT yöntemleriyle 256-bit SSL güvencesiyle ödeme yapabilirsiniz.</p>
        </div>
        <div class="faq-item card card-hover">
          <h3>Yurt dışına gönderim yapıyor musunuz?</h3>
          <p class="text-muted">Şu an için maalesef sadece Türkiye sınırları içerisine gönderim sağlamaktayız.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .legal-page { padding: 80px 20px; max-width: 800px; margin: 0 auto; }
    h1 { margin-bottom: 10px; font-size: clamp(2rem, 4vw, 2.8rem); text-align: center; }
    .mb-loose { margin-bottom: 40px; display: block; }
    .faq-list { display: flex; flex-direction: column; gap: 16px; }
    .faq-item h3 { margin-bottom: 12px; font-size: 1.15rem; color: var(--clr-primary-light); }
  `]
})
export class FaqComponent {}
