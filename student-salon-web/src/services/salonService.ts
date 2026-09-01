import {
  collection,
  doc,
  getDocs,
  onSnapshot,
  query,
  runTransaction,
  updateDoc,
  where,
  writeBatch,
  DocumentSnapshot,
  QuerySnapshot
} from 'firebase/firestore';
import { db, auth, SALON_ID, ensureAnonymousAuth } from '../firebase/config';
import {
  QueueTicket,
  ShopConfig,
  ServiceType,
  SyncStatus
} from '../types/salon';
import { getTodayDateString } from '../utils/timingEngine';
import { saveTicketId } from '../utils/storage';

export function docToShopConfig(snapshot: DocumentSnapshot): ShopConfig {
  const data = snapshot.data();
  return {
    salonId: SALON_ID,
    isOpen: data?.isOpen ?? true,
    shopName: data?.shopName || 'Student Salon',
    location: data?.location || 'Telo, Chandrapura, Bokaro, Jharkhand',
    openingHours: data?.openingHours || '08:00 AM - 09:00 PM',
    contactPhone: data?.contactPhone || '+91 91234 56789',
    announcement:
      data?.announcement || 'Welcome to Student Salon! Digital queue is active.',
    ownerPin: data?.ownerPin || '1234',
    dailyCounterDate: data?.dailyCounterDate,
    currentMaxQueueNumber: data?.currentMaxQueueNumber
  };
}

export function docToQueueTicket(snapshot: DocumentSnapshot): QueueTicket | null {
  const data = snapshot.data();
  if (!data) return null;
  const queueNum = Number(data.queueNumber);
  if (isNaN(queueNum) || queueNum <= 0) return null;

  return {
    id: data.id || snapshot.id,
    queueNumber: queueNum,
    customerName: data.customerName || 'Customer',
    customerPhone: data.customerPhone || '',
    serviceName: (data.serviceName as ServiceType) || 'HAIRCUT',
    statusName: data.statusName || 'WAITING',
    queueDate: data.queueDate || getTodayDateString(),
    createdAt: Number(data.createdAt || Date.now()),
    startedAt: data.startedAt ? Number(data.startedAt) : null,
    completedAt: data.completedAt ? Number(data.completedAt) : null,
    isRejoinedPriority: Boolean(data.isRejoinedPriority),
    notes: data.notes || '',
    creatorUid: data.creatorUid || ''
  };
}

/**
 * Subscribes to the live Salon configuration document.
 */
export function subscribeToShopConfig(
  onConfig: (config: ShopConfig) => void,
  onSyncStatus?: (status: SyncStatus) => void
): () => void {
  const salonDocRef = doc(db, 'salons', SALON_ID);

  const unsubscribe = onSnapshot(
    salonDocRef,
    { includeMetadataChanges: true },
    (snapshot) => {
      const isFromCache = snapshot.metadata.fromCache;
      if (onSyncStatus) {
        onSyncStatus({
          isCloudConnected: !isFromCache,
          isUsingLocalCache: isFromCache,
          statusMessage: isFromCache
            ? 'Offline Mode • Showing Local Cache'
            : 'Live Cloud Synced'
        });
      }

      if (snapshot.exists()) {
        onConfig(docToShopConfig(snapshot));
      }
    },
    (error) => {
      console.warn('ShopConfig snapshot error:', error);
      if (onSyncStatus) {
        onSyncStatus({
          isCloudConnected: false,
          isUsingLocalCache: true,
          statusMessage: 'Offline Mode • Showing Local Cache'
        });
      }
    }
  );

  return unsubscribe;
}

/**
 * Subscribes to today's tickets in real-time.
 */
export function subscribeToTodayTickets(
  todayDate: string,
  onTickets: (tickets: QueueTicket[]) => void,
  onSyncStatus?: (status: SyncStatus) => void
): () => void {
  const ticketsColRef = collection(db, 'salons', SALON_ID, 'tickets');
  const q = query(ticketsColRef, where('queueDate', '==', todayDate));

  const unsubscribe = onSnapshot(
    q,
    { includeMetadataChanges: true },
    (snapshot: QuerySnapshot) => {
      const isFromCache = snapshot.metadata.fromCache;
      if (onSyncStatus) {
        onSyncStatus({
          isCloudConnected: !isFromCache,
          isUsingLocalCache: isFromCache,
          statusMessage: isFromCache
            ? 'Offline Mode • Showing Local Cache'
            : 'Live Cloud Synced'
        });
      }

      const tickets: QueueTicket[] = [];
      snapshot.forEach((docSnap) => {
        const ticket = docToQueueTicket(docSnap);
        if (ticket) {
          tickets.push(ticket);
        }
      });

      // Sort by rejoined priority first, then queueNumber ascending
      tickets.sort((a, b) => {
        if (a.isRejoinedPriority !== b.isRejoinedPriority) {
          return a.isRejoinedPriority ? -1 : 1;
        }
        return a.queueNumber - b.queueNumber;
      });

      onTickets(tickets);
    },
    (error) => {
      console.warn('TodayTickets snapshot error:', error);
      if (onSyncStatus) {
        onSyncStatus({
          isCloudConnected: false,
          isUsingLocalCache: true,
          statusMessage: 'Offline Mode • Showing Local Cache'
        });
      }
    }
  );

  return unsubscribe;
}

/**
 * Atomic customer queue joining with daily sequential numbering.
 */
export async function joinQueue(
  customerName: string,
  customerPhone: string,
  service: ServiceType,
  notes: string = ''
): Promise<QueueTicket> {
  const creatorUid = auth.currentUser?.uid || (await ensureAnonymousAuth());
  const today = getTodayDateString();

  const salonDocRef = doc(db, 'salons', SALON_ID);
  const ticketColRef = collection(db, 'salons', SALON_ID, 'tickets');
  const ticketDocRef = doc(ticketColRef);
  const ticketId = ticketDocRef.id;

  const now = Date.now();

  const assignedQueueNumber = await runTransaction(db, async (transaction) => {
    const salonDoc = await transaction.get(salonDocRef);
    if (!salonDoc.exists()) {
      throw new Error('Salon configuration not found.');
    }

    const salonData = salonDoc.data();
    if (salonData.isOpen === false) {
      throw new Error('Salon is currently closed.');
    }

    const counterDate = salonData.dailyCounterDate || '';
    let currentMax = Number(salonData.currentMaxQueueNumber || 0);

    if (counterDate !== today) {
      currentMax = 0;
    }

    const nextQueueNumber = currentMax + 1;

    // Update salon counter
    transaction.update(salonDocRef, {
      dailyCounterDate: today,
      currentMaxQueueNumber: nextQueueNumber
    });

    // Write new ticket document matching exact Android field names
    const ticketData = {
      id: ticketId,
      queueNumber: nextQueueNumber,
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      serviceName: service,
      statusName: 'WAITING',
      queueDate: today,
      createdAt: now,
      startedAt: null,
      completedAt: null,
      isRejoinedPriority: false,
      notes: notes.trim(),
      creatorUid: creatorUid
    };

    transaction.set(ticketDocRef, ticketData);
    return nextQueueNumber;
  });

  const createdTicket: QueueTicket = {
    id: ticketId,
    queueNumber: assignedQueueNumber,
    customerName: customerName.trim(),
    customerPhone: customerPhone.trim(),
    serviceName: service,
    statusName: 'WAITING',
    queueDate: today,
    createdAt: now,
    startedAt: null,
    completedAt: null,
    isRejoinedPriority: false,
    notes: notes.trim(),
    creatorUid: creatorUid
  };

  saveTicketId(ticketId);
  return createdTicket;
}

/**
 * Customer cancels their own ticket.
 */
export async function cancelCustomerTicket(ticketId: string): Promise<void> {
  const ticketDocRef = doc(db, 'salons', SALON_ID, 'tickets', ticketId);
  await updateDoc(ticketDocRef, {
    statusName: 'CANCELLED'
  });
}

/**
 * Start service for a customer.
 * If another customer is currently SERVING, automatically marks them as COMPLETED.
 * Preserves the exact Android SalonRepository behavior in a single Firestore batch.
 */
export async function startCustomer(ticketId: string): Promise<void> {
  await ensureAnonymousAuth();
  const today = getTodayDateString();
  const ticketsColRef = collection(db, 'salons', SALON_ID, 'tickets');

  // Query any currently serving customer today
  const servingQuery = query(
    ticketsColRef,
    where('queueDate', '==', today),
    where('statusName', '==', 'SERVING')
  );
  const servingSnap = await getDocs(servingQuery);

  const batch = writeBatch(db);
  const now = Date.now();

  // Complete any currently serving ticket that isn't the new target ticket
  servingSnap.forEach((docSnap) => {
    if (docSnap.id !== ticketId) {
      batch.update(docSnap.ref, {
        statusName: 'COMPLETED',
        completedAt: now
      });
    }
  });

  // Set target ticket to SERVING with current timestamp
  const targetDocRef = doc(db, 'salons', SALON_ID, 'tickets', ticketId);
  batch.update(targetDocRef, {
    statusName: 'SERVING',
    startedAt: now
  });

  await batch.commit();
}

/**
 * Complete service for the active customer.
 */
export async function completeService(ticketId: string): Promise<void> {
  await ensureAnonymousAuth();
  const now = Date.now();
  const ticketDocRef = doc(db, 'salons', SALON_ID, 'tickets', ticketId);
  await updateDoc(ticketDocRef, {
    statusName: 'COMPLETED',
    completedAt: now
  });
}

/**
 * Skip a customer who missed their turn.
 */
export async function skipCustomer(ticketId: string): Promise<void> {
  await ensureAnonymousAuth();
  const ticketDocRef = doc(db, 'salons', SALON_ID, 'tickets', ticketId);
  await updateDoc(ticketDocRef, {
    statusName: 'SKIPPED'
  });
}

/**
 * Rejoin a skipped customer with high priority.
 */
export async function rejoinCustomer(ticketId: string): Promise<void> {
  await ensureAnonymousAuth();
  const ticketDocRef = doc(db, 'salons', SALON_ID, 'tickets', ticketId);
  await updateDoc(ticketDocRef, {
    statusName: 'WAITING',
    isRejoinedPriority: true
  });
}

/**
 * Update shop open/closed status.
 */
export async function setShopOpen(isOpen: boolean): Promise<void> {
  await ensureAnonymousAuth();
  const salonDocRef = doc(db, 'salons', SALON_ID);
  await updateDoc(salonDocRef, {
    isOpen
  });
}

/**
 * Update salon announcement banner.
 */
export async function updateAnnouncement(announcement: string): Promise<void> {
  await ensureAnonymousAuth();
  const salonDocRef = doc(db, 'salons', SALON_ID);
  await updateDoc(salonDocRef, {
    announcement: announcement.trim()
  });
}

/**
 * Reset today's queue safely.
 * Deletes today's tickets and resets daily counter to 0 in a batch.
 */
export async function resetTodayQueue(): Promise<void> {
  await ensureAnonymousAuth();
  const today = getTodayDateString();
  const ticketsColRef = collection(db, 'salons', SALON_ID, 'tickets');
  const todayQuery = query(ticketsColRef, where('queueDate', '==', today));
  const todaySnap = await getDocs(todayQuery);

  const batch = writeBatch(db);
  todaySnap.forEach((docSnap) => {
    batch.delete(docSnap.ref);
  });

  const salonDocRef = doc(db, 'salons', SALON_ID);
  batch.update(salonDocRef, {
    currentMaxQueueNumber: 0
  });

  await batch.commit();
}

/**
 * Seeds sample queue demo data for today matching Android behavior.
 */
export async function seedDemoQueue(): Promise<void> {
  const creatorUid = auth.currentUser?.uid || (await ensureAnonymousAuth());
  const today = getTodayDateString();
  const now = Date.now();

  const samples = [
    {
      id: `demo_${Date.now()}_1`,
      queueNumber: 1,
      customerName: 'Yunus Ansari',
      customerPhone: '+91 98765 43210',
      serviceName: 'HAIRCUT' as ServiceType,
      statusName: 'SERVING',
      queueDate: today,
      createdAt: now - 12 * 60 * 1000,
      startedAt: now - 8 * 60 * 1000,
      completedAt: null,
      isRejoinedPriority: false,
      notes: 'Fade cut',
      creatorUid
    },
    {
      id: `demo_${Date.now()}_2`,
      queueNumber: 2,
      customerName: 'Rahul Verma',
      customerPhone: '+91 98765 01234',
      serviceName: 'HAIR_BEARD' as ServiceType,
      statusName: 'WAITING',
      queueDate: today,
      createdAt: now - 10 * 60 * 1000,
      startedAt: null,
      completedAt: null,
      isRejoinedPriority: false,
      notes: 'Trim beard short',
      creatorUid
    },
    {
      id: `demo_${Date.now()}_3`,
      queueNumber: 3,
      customerName: 'Aman Kumar',
      customerPhone: '+91 98765 12345',
      serviceName: 'HAIRCUT' as ServiceType,
      statusName: 'WAITING',
      queueDate: today,
      createdAt: now - 6 * 60 * 1000,
      startedAt: null,
      completedAt: null,
      isRejoinedPriority: false,
      notes: '',
      creatorUid
    },
    {
      id: `demo_${Date.now()}_4`,
      queueNumber: 4,
      customerName: 'Arjun Singh',
      customerPhone: '+91 98765 23456',
      serviceName: 'HAIR_BEARD_FACIAL' as ServiceType,
      statusName: 'WAITING',
      queueDate: today,
      createdAt: now - 3 * 60 * 1000,
      startedAt: null,
      completedAt: null,
      isRejoinedPriority: false,
      notes: 'VIP package',
      creatorUid
    },
    {
      id: `demo_${Date.now()}_5`,
      queueNumber: 5,
      customerName: 'Vikram Sharma',
      customerPhone: '+91 98765 34567',
      serviceName: 'HAIRCUT' as ServiceType,
      statusName: 'SKIPPED',
      queueDate: today,
      createdAt: now - 15 * 60 * 1000,
      startedAt: null,
      completedAt: null,
      isRejoinedPriority: false,
      notes: '',
      creatorUid
    }
  ];

  const batch = writeBatch(db);
  for (const item of samples) {
    const docRef = doc(db, 'salons', SALON_ID, 'tickets', item.id);
    batch.set(docRef, item);
  }

  const salonDocRef = doc(db, 'salons', SALON_ID);
  batch.update(salonDocRef, {
    dailyCounterDate: today,
    currentMaxQueueNumber: 5
  });

  await batch.commit();
}
