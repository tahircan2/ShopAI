import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * SafeHtmlPipe — XSS Koruması için güvenli HTML rendering.
 *
 * Angular'ın [innerHTML] binding'i varsayılan olarak HTML'i sanitize eder
 * ama script tag'lerini ve olay işleyicileri (onclick, onload vb.) siler.
 * Bu pipe DomSanitizer'ı AÇIK olarak kullanır ve Angular'ın güvenlik modeline uyar.
 *
 * KULLANIM:
 *   {{ product.description | safeHtml }}
 *   <div [innerHTML]="product.description | safeHtml"></div>
 *
 * DİKKAT: Bu pipe bypass DEĞİL — Angular'ın sanitizer'ı çalışır.
 *   Eğer backend'den gelen HTML'in DOMPurify ile temizlendiğinden emin iseniz
 *   ve bypassSecurityTrustHtml kullanmak istemiyorsanız bu pipe yeterlidir.
 */
@Pipe({
  name: 'safeHtml',
  standalone: true
})
export class SafeHtmlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';
    // sanitize() ile Angular'ın XSS koruması aktif kalır
    return this.sanitizer.sanitize(1 /* SecurityContext.HTML */, value) ?? '';
  }
}
