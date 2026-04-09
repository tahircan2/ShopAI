import { Component } from '@angular/core';

@Component({
  selector: 'app-terms',
  standalone: true,
  template: `
    <div class="legal-page container">
      <h1>Kullanım Koşulları</h1>
      <div class="content-block card">
        <p class="text-muted">Son Güncelleme: 1 Ocak 2024</p>
        
        <h3>1. Taraflar</h3>
        <p class="text-muted">Bu sözleşme, ShopAI (bundan sonra "Platform" olarak anılacaktır) ile Platform'a üye olan kullanıcılar arasında düzenlenmiştir.</p>
        
        <h3>2. Hizmetlerin Kapsamı</h3>
        <p class="text-muted">ShopAI, satıcılar ile alıcıları bir araya getiren bir aracı hizmet sağlayıcıdır. Kullanıcılar, AI destekli asistan üzerinden arama yapabilir ve platform üzerinden alışveriş gerçekleştirebilir.</p>
        
        <h3>3. Kullanıcı Yükümlülükleri</h3>
        <p class="text-muted">Kullanıcı, platformu hukuka ve yasalara uygun bir şekilde kullanmayı, başkalarının haklarına tecavüz etmemeyi peşinen kabul eder. Hesabın güvenliğinden ve şifrenin korunmasından bizzat kullanıcı sorumludur.</p>

        <h3>4. Satıcı Yükümlülükleri</h3>
        <p class="text-muted">Satıcılar, listeledikleri ürünlerin yasal olduğunu, stoklarının bulunduğunu ve vaat edilen sürede kargoya vereceklerini kabul ederler.</p>
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
export class TermsComponent {}
