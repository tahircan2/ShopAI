import { Pipe, PipeTransform } from '@angular/core';

// ─── Currency Format Pipe ─────────────────────────────────────────────────────
@Pipe({ name: 'currencyFormat', standalone: true })
export class CurrencyFormatPipe implements PipeTransform {
  transform(value: number | null | undefined, currency = '₺'): string {
    if (value == null) return `${currency}0.00`;
    return `${currency}${value.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}

// ─── Time Ago Pipe ────────────────────────────────────────────────────────────
@Pipe({ name: 'timeAgo', standalone: true })
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60) return `${diff} saniye önce`;
    if (diff < 3600) return `${Math.floor(diff / 60)} dakika önce`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} saat önce`;
    if (diff < 2592000) return `${Math.floor(diff / 86400)} gün önce`;
    if (diff < 31536000) return `${Math.floor(diff / 2592000)} ay önce`;
    return `${Math.floor(diff / 31536000)} yıl önce`;
  }
}

// ─── Truncate Pipe ────────────────────────────────────────────────────────────
@Pipe({ name: 'truncate', standalone: true })
export class TruncatePipe implements PipeTransform {
  transform(value: string | null | undefined, limit = 100, trail = '...'): string {
    if (!value) return '';
    return value.length > limit ? value.substring(0, limit) + trail : value;
  }
}
