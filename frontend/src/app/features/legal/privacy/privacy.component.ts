import { Component } from '@angular/core';

@Component({
  selector: 'app-privacy',
  standalone: true,
  template: `
    <div class="legal-page container">
      <h1>Gizlilik Politikası</h1>
      <div class="content-block card">
        <p class="text-muted">Bu Gizlilik Politikası, ShopAI platformunu kullanırken kişisel verilerinizin nasıl toplandığını, kullanıldığını ve korunduğunu açıklamaktadır.</p>
        
        <h3>Verilerin Toplanması</h3>
        <p class="text-muted">Platforma kayıt olurken paylaştığınız ad, soyad, e-posta adresi gibi bilgilerle, platform kullanımınız sırasında oluşan alışveriş ve gezinme verilerini toplamaktayız.</p>
        
        <h3>Verilerin Kullanımı</h3>
        <p class="text-muted">Toplanan veriler yalnızca hizmet kalitesini artırmak, siparişlerinizi yönetmek, AI destekli ürün önermeleri yapmak ve güvenlik amaçlarıyla kullanılmaktadır.</p>
        
        <h3>Güvenlik</h3>
        <p class="text-muted">Kişisel verileriniz, yetkisiz erişimlere karşı sektör standartlarındaki şifreleme yöntemleriyle (HTTPS/TLS) korunmaktadır. Ayrıca sistemimizde <strong>HttpOnly Cookie</strong> kullanılarak XSS saldırılarına karşı tam güvenlik sağlanır.</p>

        <h3>Üçüncü Taraflarla Paylaşım</h3>
        <p class="text-muted">Kişisel bilgileriniz, yasal zorunluluklar haricinde hiçbir üçüncü taraf şirket ile satış veya pazarlama amaçlı olarak paylaşılmaz.</p>
      </div>
    </div>
  `,
  styles: [`
    .legal-page { padding: 80px 20px; max-width: 800px; margin: 0 auto; }
    h1 { margin-bottom: 40px; font-size: clamp(2rem, 4vw, 2.8rem); text-align: center; }
    .content-block { padding: 40px; }
    h3 { margin-top: 30px; margin-bottom: 12px; font-size: 1.25rem; color: var(--clr-primary-light); }
    h3:first-child { margin-top: 0; }
    p { line-height: 1.7; margin-bottom: 16px; }
  `]
})
export class PrivacyComponent {}
