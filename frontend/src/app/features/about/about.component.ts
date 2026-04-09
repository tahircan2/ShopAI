import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss'
})
export class AboutComponent {
  readonly stats = [
    { value: '50K+', label: 'Mutlu Müşteri' },
    { value: '200K+', label: 'Ürün Çeşidi' },
    { value: '5Y+',  label: 'Sektör Deneyimi' },
    { value: '4.9',  label: 'Ortalama Puan' }
  ];

  readonly team = [
    { name: 'Ahmet Yılmaz',   role: 'CEO & Kurucu',         avatar: 'AY', color: '#7c6ff7' },
    { name: 'Elif Kaya',      role: 'Ürün Direktörü',       avatar: 'EK', color: '#f0a060' },
    { name: 'Mehmet Demir',   role: 'Baş Mühendis',         avatar: 'MD', color: '#4ade80' },
    { name: 'Selin Arslan',   role: 'AI Araştırmacısı',     avatar: 'SA', color: '#60a5fa' }
  ];

  readonly values = [
    { icon: '🤖', title: 'AI Odaklı',        desc: 'Yapay zeka teknolojisini alışveriş deneyiminin merkezine koyuyoruz.' },
    { icon: '🛡️', title: 'Güven',            desc: 'Müşteri verilerini en yüksek güvenlik standartlarıyla koruyoruz.' },
    { icon: '♻️', title: 'Sürdürülebilirlik', desc: 'Çevre dostu paketleme ve karbon nötr teslimat taahhüdü.' },
    { icon: '🤝', title: 'Topluluk',          desc: 'Satıcılarımız ve müşterilerimizle güçlü ilişkiler kuruyoruz.' }
  ];
}
