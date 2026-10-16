export const ICON_REGISTRY = {
  login: '/icons/login-icon.svg',
  clinic: '/icons/clinic-icon.svg',
  arrowRight: '/icons/arrow-right-icon.svg'
} as const;

export type IconName = keyof typeof ICON_REGISTRY;
