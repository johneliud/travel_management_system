export interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles?: string[];
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Browse', route: '/travels', icon: 'compass' },
  { label: 'My Travels', route: '/travels/mine', icon: 'map', roles: ['TRAVEL_MANAGER', 'ADMIN'] },
  { label: 'Admin', route: '/admin', icon: 'shield', roles: ['ADMIN'] },
];
