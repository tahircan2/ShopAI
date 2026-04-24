import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { parse } from 'marked';
import DOMPurify from 'dompurify';

@Pipe({
  name: 'markdown',
  standalone: true
})
export class MarkdownPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';
    
    // Parse Markdown to HTML
    // parse returns a string or Promise<string>. Since we're not using async plugins, we can cast it
    const parsedHtml = parse(value) as string;
    
    // Sanitize HTML using DOMPurify
    const cleanHtml = DOMPurify.sanitize(parsedHtml);
    
    // Bypass Angular's built-in sanitizer since DOMPurify already cleaned it
    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml);
  }
}
