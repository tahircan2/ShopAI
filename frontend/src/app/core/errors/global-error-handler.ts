import { ErrorHandler, Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { ToastService } from '../services/toast.service';

/**
 * GlobalErrorHandler — Angular ErrorHandler Implementasyonu
 *
 * Angular'ın varsayılan davranışı, yakalanmamış hataları browser konsoluna yazar.
 * Bu, production'da hassas stack trace ve uygulama detaylarının
 * kullanıcının tarayıcısında görünmesine yol açabilir.
 *
 * Bu handler:
 * 1. Production'da stack trace'i gizler — kullanıcıya sadece jenerik mesaj gösterir
 * 2. Geliştirme sırasında tüm detayları konsola yazar
 * 3. Gelecekte Sentry/DataDog gibi error tracking servislerine entegre edilebilir
 *
 * KAYIT: app.config.ts içinde providers dizisine eklenmeli:
 *   { provide: ErrorHandler, useClass: GlobalErrorHandler }
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  // NOT: ToastService constructor injection yerine handleError içinden
  // alınmaz — ErrorHandler başka token'lardan önce oluşturulur.
  // Eğer toast göstermek istiyorsak, lazy injection kullanmalıyız.

  handleError(error: unknown): void {
    const err = error instanceof Error ? error : new Error(String(error));

    if (!environment.production) {
      // Geliştirme: tüm detayları göster
      console.error('[GlobalErrorHandler] Uncaught error:', err);
    } else {
      // Production: hassas bilgileri gizle — sadece özet logla
      console.error('[Error]', err.message ?? 'An unexpected error occurred.');
      // TODO: Sentry veya benzeri bir error tracking servisi entegre edilinceye kadar
      // bu noktada HTTP isteği ile backend'e hata raporu gönderilebilir.
    }
  }
}
