import { Directive, ElementRef, EventEmitter, HostListener, Input, OnDestroy, Output, AfterViewInit } from '@angular/core';
import { Subject, debounceTime, takeUntil } from 'rxjs';

// ─── Click Outside Directive ──────────────────────────────────────────────────
@Directive({
  selector: '[clickOutside]',
  standalone: true
})
export class ClickOutsideDirective {
  @Output() clickOutside = new EventEmitter<void>();

  constructor(private el: ElementRef) {}

  @HostListener('document:click', ['$event'])
  onClick(event: Event): void {
    if (!this.el.nativeElement.contains(event.target)) {
      this.clickOutside.emit();
    }
  }
}

// ─── Debounce Click Directive ─────────────────────────────────────────────────
@Directive({
  selector: '[debounceClick]',
  standalone: true
})
export class DebounceClickDirective implements OnDestroy {
  @Input() debounceTime = 300;
  @Output() debounceClick = new EventEmitter<Event>();

  private clicks$ = new Subject<Event>();
  private destroy$ = new Subject<void>();

  constructor() {
    this.clicks$.pipe(
      debounceTime(this.debounceTime),
      takeUntil(this.destroy$)
    ).subscribe(e => this.debounceClick.emit(e));
  }

  @HostListener('click', ['$event'])
  clickEvent(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.clicks$.next(event);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

// ─── Lazy Load Image Directive ─────────────────────────────────────────────────
@Directive({
  selector: 'img[lazyLoad]',
  standalone: true
})
export class LazyLoadImageDirective implements AfterViewInit, OnDestroy {
  @Input() lazyLoad!: string;
  @Input() placeholder = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxIiBoZWlnaHQ9IjEiPjwvc3ZnPg==';

  private observer?: IntersectionObserver;

  constructor(private el: ElementRef<HTMLImageElement>) {}

  ngAfterViewInit(): void {
    this.el.nativeElement.src = this.placeholder;

    if ('IntersectionObserver' in window) {
      this.observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.el.nativeElement.src = this.lazyLoad;
            this.observer?.disconnect();
          }
        });
      }, { threshold: 0.1 });

      this.observer.observe(this.el.nativeElement);
    } else {
      this.el.nativeElement.src = this.lazyLoad;
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
