import { useState, useEffect, useMemo } from 'react';
import {
  ShopConfig,
  QueueTicket,
  CustomerTicketInfo,
  ServiceType,
  SyncStatus
} from '../types/salon';
import {
  subscribeToShopConfig,
  subscribeToTodayTickets,
  joinQueue,
  cancelCustomerTicket
} from '../services/salonService';
import {
  calculateTicketInfo,
  getTodayDateString
} from '../utils/timingEngine';
import {
  getSavedTicketIds,
  getSelectedTicketId,
  setSelectedTicketId,
  removeSavedTicketId
} from '../utils/storage';
import { SyncStatusIndicator } from '../components/SyncStatusIndicator';
import { ShopStatusBadge } from '../components/ShopStatusBadge';
import { DigitalTicketCard } from '../components/DigitalTicketCard';
import { MultiTicketSwitcher } from '../components/MultiTicketSwitcher';
import { JoinQueueForm } from '../components/JoinQueueForm';

export const CustomerView: React.FC = () => {
  const [shopConfig, setShopConfig] = useState<ShopConfig>({
    salonId: 'student_salon_telo',
    isOpen: true,
    shopName: 'Student Salon 2',
    location: 'Telo, Chandrapura, Bokaro, Jharkhand',
    openingHours: '08:00 AM - 09:00 PM',
    contactPhone: '+91 91234 56789',
    announcement: 'Welcome to Student Salon! Digital queue is active.',
    ownerPin: '1234'
  });

  const [todayTickets, setTodayTickets] = useState<QueueTicket[]>([]);
  const [syncStatus, setSyncStatus] = useState<SyncStatus>({
    isCloudConnected: true,
    isUsingLocalCache: false,
    statusMessage: 'Connecting to Cloud...'
  });

  const [savedIds, setSavedIds] = useState<string[]>([]);
  const [activeTicketId, setActiveTicketId] = useState<string | null>(null);
  const [currentTime, setCurrentTime] = useState<number>(Date.now());

  // Real-time 1-second interval ticker for live countdowns
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Load saved ticket IDs on mount
  useEffect(() => {
    const ids = getSavedTicketIds();
    setSavedIds(ids);
    const selected = getSelectedTicketId();
    if (selected && ids.includes(selected)) {
      setActiveTicketId(selected);
    } else if (ids.length > 0) {
      setActiveTicketId(ids[0]);
    }
  }, []);

  // Subscribe to Shop Config
  useEffect(() => {
    const unsubscribe = subscribeToShopConfig(
      (config) => setShopConfig(config),
      (status) => setSyncStatus(status)
    );
    return () => unsubscribe();
  }, []);

  // Subscribe to Today's Tickets
  useEffect(() => {
    const today = getTodayDateString();
    const unsubscribe = subscribeToTodayTickets(
      today,
      (tickets) => setTodayTickets(tickets),
      (status) => setSyncStatus(status)
    );
    return () => unsubscribe();
  }, []);

  // Compute ticket info for all saved ticket IDs
  const myTicketsInfo: CustomerTicketInfo[] = useMemo(() => {
    return savedIds
      .map((id) => {
        const ticket = todayTickets.find((t) => t.id === id);
        if (!ticket) return null;
        return calculateTicketInfo(ticket, todayTickets, currentTime);
      })
      .filter((info): info is CustomerTicketInfo => info !== null);
  }, [savedIds, todayTickets, currentTime]);

  // Determine currently selected ticket info
  const selectedTicketInfo = useMemo(() => {
    if (myTicketsInfo.length === 0) return null;
    const found = myTicketsInfo.find((t) => t.ticket.id === activeTicketId);
    return found || myTicketsInfo[0];
  }, [myTicketsInfo, activeTicketId]);

  const handleSelectTicket = (ticketId: string) => {
    setActiveTicketId(ticketId);
    setSelectedTicketId(ticketId);
  };

  const handleJoinQueue = async (
    name: string,
    phone: string,
    service: ServiceType,
    notes: string
  ) => {
    const newTicket = await joinQueue(name, phone, service, notes);
    const updatedIds = getSavedTicketIds();
    setSavedIds(updatedIds);
    setActiveTicketId(newTicket.id);
  };

  const handleCancelTicket = async (ticketId: string) => {
    await cancelCustomerTicket(ticketId);
  };

  const handleDismissTicket = (ticketId: string) => {
    removeSavedTicketId(ticketId);
    const updatedIds = getSavedTicketIds();
    setSavedIds(updatedIds);
    if (activeTicketId === ticketId) {
      setActiveTicketId(updatedIds.length > 0 ? updatedIds[0] : null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between max-w-lg mx-auto border-x border-slate-800 shadow-2xl">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-4 py-3.5 space-y-2">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">💈</span>
              <h1 className="text-lg font-extrabold text-amber-400 tracking-tight">
                {shopConfig.shopName}
              </h1>
            </div>
            <p className="text-[11px] text-slate-400">
              {shopConfig.location}
            </p>
          </div>
          <SyncStatusIndicator syncStatus={syncStatus} />
        </div>

        {/* Shop Hours & Status */}
        <ShopStatusBadge
          isOpen={shopConfig.isOpen}
          openingHours={shopConfig.openingHours}
        />
      </header>

      {/* Main Content Area */}
      <main className="flex-1 p-4 space-y-5">
        {/* Announcement Banner */}
        {shopConfig.announcement && (
          <div className="bg-amber-950/40 border border-amber-800/60 rounded-2xl p-3.5 flex items-start gap-3 text-xs">
            <span className="text-base flex-shrink-0">📢</span>
            <div className="space-y-0.5">
              <span className="font-bold text-amber-400 uppercase tracking-wider block text-[10px]">
                Notice from Barber
              </span>
              <p className="text-slate-300 leading-relaxed">
                {shopConfig.announcement}
              </p>
            </div>
          </div>
        )}

        {/* Multi-Ticket Switcher */}
        {myTicketsInfo.length > 0 && (
          <MultiTicketSwitcher
            ticketsInfo={myTicketsInfo}
            selectedTicketId={selectedTicketInfo?.ticket.id || null}
            onSelectTicket={handleSelectTicket}
          />
        )}

        {/* Active Digital Ticket Card */}
        {selectedTicketInfo && (
          <section className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Your Digital Pass
              </span>
              <span className="text-[11px] text-slate-500 font-medium">
                Live 1s Real-time
              </span>
            </div>
            <DigitalTicketCard
              ticketInfo={selectedTicketInfo}
              onCancelTicket={handleCancelTicket}
              onDismissTicket={handleDismissTicket}
            />
          </section>
        )}

        {/* Join Queue Form (ALWAYS available when salon is open) */}
        <section className="space-y-2">
          <JoinQueueForm
            isOpen={shopConfig.isOpen}
            onJoin={handleJoinQueue}
          />
        </section>
      </main>

      {/* Footer */}
      <footer className="p-4 bg-slate-900 border-t border-slate-800 text-center space-y-1">
        <p className="text-xs font-semibold text-slate-400">
          Student Salon 2 • Digital Queue Web Experience
        </p>
        <p className="text-[10px] text-slate-500">
          Powered by Firebase Real-Time Firestore • Syncs directly with in-store counter
        </p>
      </footer>
    </div>
  );
};
