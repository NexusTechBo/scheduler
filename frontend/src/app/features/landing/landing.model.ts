import { UiIconName } from '../../shared/ui/ui-icon/ui-icon.component';

export interface LandingFeature {
  icon: UiIconName;
  title: string;
  description: string;
}

export interface LandingStep {
  title: string;
  description: string;
}

export interface LandingShot {
  src: string;
  alt: string;
}

export interface LandingShowcase {
  eyebrow: string;
  title: string;
  description: string;
  points: string[];
  shot: LandingShot;
}

export interface LandingRole {
  id: string;
  label: string;
  icon: UiIconName;
  title: string;
  description: string;
  shot: LandingShot;
}
