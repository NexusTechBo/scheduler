/**
 * Central catalog of SVG icons served from public/icons/.
 * To add an icon: drop the .svg file into public/icons/ and add one entry here —
 * the key becomes the name used everywhere (`<app-icon name="...">`, `'...' | icon`).
 */
export const ICON_REGISTRY = {
  login: '/icons/login-icon.svg',
  clinic: '/icons/clinic-icon.svg',
  arrowRight: '/icons/arrow-right-icon.svg'
} as const;

export type IconName = keyof typeof ICON_REGISTRY;
