import { Injectable, signal } from '@angular/core';
import { EN_TRANSLATIONS } from './en';
import { ES_TRANSLATIONS } from './es';

export type SupportedLang = 'es' | 'en';

@Injectable({
  providedIn: 'root',
})
export class I18nService {
  private readonly defaultLang: SupportedLang = this.detectInitialLang();

  readonly currentLang = signal<SupportedLang>(this.defaultLang);

  private readonly translations: Record<SupportedLang, any> = {
    es: ES_TRANSLATIONS,
    en: EN_TRANSLATIONS,
  };

  private detectInitialLang(): SupportedLang {
    const saved = localStorage.getItem('lang');
    if (saved === 'es' || saved === 'en') {
      return saved;
    }
    const browserLang = navigator.language.slice(0, 2);
    return browserLang === 'es' ? 'es' : 'en';
  }

  setLanguage(lang: SupportedLang): void {
    if (this.currentLang() !== lang) {
      this.currentLang.set(lang);
      localStorage.setItem('lang', lang);
    }
  }

  translate(key: string): string {
    const lang = this.currentLang();
    const dictionary = this.translations[lang] || this.translations.en;
    const value = key.split('.').reduce((acc, part) => (acc ? acc[part] : undefined), dictionary);
    return typeof value === 'string' ? value : key;
  }
}
