import { Component, inject } from '@angular/core';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loading-bar',
  standalone: true,
  templateUrl: './loading-bar.component.html',
  styleUrl: './loading-bar.component.scss'
})
export class LoadingBarComponent {
  readonly loading = inject(LoadingService);
}
