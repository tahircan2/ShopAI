import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss'
})
export class ContactComponent {
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly sent = signal(false);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    subject: ['', Validators.required],
    message: ['', [Validators.required, Validators.minLength(20)]]
  });

  readonly contactInfo = [
    { icon: '📍', label: 'Adres',   value: 'Maslak Mahallesi, Büyükdere Cad. No:255, 34398 Sarıyer/İstanbul' },
    { icon: '📞', label: 'Telefon', value: '+90 (212) 123 45 67' },
    { icon: '🕐', label: 'Çalışma Saatleri', value: 'Pzt–Cum: 09:00–18:00' }
  ];

  showError(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && (c?.dirty || c?.touched));
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    // Simüle edilmiş gönderim (backend endpoint hazır değil)
    setTimeout(() => {
      this.loading.set(false);
      this.sent.set(true);
      this.toast.success('Mesajınız gönderildi! En kısa sürede dönüş yapacağız.');
      this.form.reset();
    }, 1200);
  }
}
