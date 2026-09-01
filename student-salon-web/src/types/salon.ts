export type ServiceType = 'HAIRCUT' | 'HAIR_BEARD' | 'HAIR_BEARD_FACIAL';

export interface ServiceDefinition {
  id: ServiceType;
  title: string;
  durationMinutes: number;
  description: string;
  iconEmoji: string;
}

export const SALON_SERVICES: Record<ServiceType, ServiceDefinition> = {
  HAIRCUT: {
    id: 'HAIRCUT',
    title: 'Haircut',
    durationMinutes: 20,
    description: 'Standard & modern haircut with styling',
    iconEmoji: '💇'
  },
  HAIR_BEARD: {
    id: 'HAIR_BEARD',
    title: 'Hair + Beard',
    durationMinutes: 35,
    description: 'Haircut & beard trimming, shaping & line-up',
    iconEmoji: '💇‍♂️🧔'
  },
  HAIR_BEARD_FACIAL: {
    id: 'HAIR_BEARD_FACIAL',
    title: 'Hair + Beard + Facial',
    durationMinutes: 45,
    description: 'Full grooming package with refreshing face cleanse & massage',
    iconEmoji: '✨'
  }
};

export type QueueStatus = 'WAITING' | 'SERVING' | 'COMPLETED' | 'SKIPPED' | 'CANCELLED' | 'REJOINED';

export interface QueueTicket {
  id: string;
  queueNumber: number;
  customerName: string;
  customerPhone: string;
  serviceName: ServiceType;
  statusName: QueueStatus;
  queueDate: string; // "YYYY-MM-DD"
  createdAt: number; // timestamp ms
  startedAt: number | null; // timestamp ms
  completedAt: number | null; // timestamp ms
  isRejoinedPriority: boolean;
  notes: string;
  creatorUid: string;
}

export interface ShopConfig {
  salonId: string;
  isOpen: boolean;
  shopName: string;
  location: string;
  openingHours: string;
  contactPhone: string;
  announcement: string;
  ownerPin: string;
  dailyCounterDate?: string;
  currentMaxQueueNumber?: number;
}

export interface CustomerTicketInfo {
  ticket: QueueTicket;
  customersAhead: number;
  estimatedWaitingMinutes: number;
  estimatedTurnTimeFormatted: string;
  remainingServingMinutes: number;
  estimatedWaitingSeconds: number;
  estimatedWaitingMillis: number;
  estimatedWaitingFormatted: string;
  remainingServingSeconds: number;
  remainingServingMillis: number;
  remainingServingFormatted: string;
}

export interface DailySummary {
  totalCustomers: number;
  waitingCount: number;
  servingCount: number;
  completedCount: number;
  skippedCount: number;
  cancelledCount: number;
}

export interface SyncStatus {
  isCloudConnected: boolean;
  isUsingLocalCache: boolean;
  statusMessage: string;
}
