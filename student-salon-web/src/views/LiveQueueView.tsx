import React, { useState, useEffect, useMemo } from 'react';
import {
  ShopConfig,
  QueueTicket,
  SyncStatus,
  SALON_SERVICES
} from '../types/salon';
import {
  subscribeToShopConfig,
  subscribeToTodayTickets
} from '../services/salonService';
import {
  calculateTicketInfo,
  getTodayDateString
} from '../utils/timingEngine';
import { SyncStatusIndicator } from '../components/SyncStatusIndicator';
import { ShopStatusBadge } from '../components/ShopStatusBadge';

export const LiveQueueView: React.FC = () => {
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

  const [currentTime, setCurrentTime] = useState<number>(Date.now());

  // 1-second live ticker for clock & countdowns
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(Date.now());
    }, 1000);
    return () => clearInterval(timer);
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

  const servingTicket = useMemo(
    () => todayTickets.find((t) => t.statusName === 'SERVING') || null,
    [todayTickets]
  );

  const waitingTickets = useMemo(
    () =>
      todayTickets
        .filter((t) => t.statusName === 'WAITING')
        .sort((a, b) => {
          if (a.isRejoinedPriority !== b.isRejoinedPriority) {
            return a.isRejoinedPriority ? -1 : 1;
          }
          return a.queueNumber - b.queueNumber;
        }),
    [todayTickets]
  );

  const formattedClock = useMemo(() => {
    const d = new Date(currentTime);
    return d.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }, [currentTime]);

  const servingInfo = useMemo(() => {
    if (!servingTicket) return null;
    return calculateTicketInfo(servingTicket, todayTickets, currentTime);
  }, [servingTicket, todayTickets, currentTime]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between selection:bg-amber-400 selection:text-slate-950">
      {/* Top TV Header */}
      <header className="bg-slate-900/90 border-b border-slate-800/80 px-6 py-4 sticky top-0 z-30 shadow-xl backdrop-blur-md">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-2xl shadow-inner">
              💈
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-xl sm:text-2xl font-black text-amber-400 tracking-tight">
                  {shopConfig.shopName || 'Student Salon 2'}
                </h1>
                <span className="text-[11px] font-extrabold uppercase bg-amber-500/20 text-amber-300 px-2.5 py-0.5 rounded-full border border-amber-500/30">
                  Live Queue Board
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-400">
                {shopConfig.location || 'Telo, Bokaro'} • {shopConfig.openingHours || '08:00 AM - 09:00 PM'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {/* Live Clock Display */}
            <div className="bg-slate-950 px-4 py-2 rounded-2xl border border-slate-800 text-center shadow-inner">
              <span className="text-[10px] uppercase font-bold text-slate-500 block">
                CURRENT TIME
              </span>
              <span className="text-lg sm:text-xl font-black text-white font-mono tracking-wider">
                {formattedClock}
              </span>
            </div>

            <ShopStatusBadge isOpen={shopConfig.isOpen} />
          </div>
        </div>
      </header>

      {/* Announcement Marquee Bar */}
      {shopConfig.announcement && (
        <div className="bg-amber-400 text-slate-950 py-2 px-6 font-bold text-xs sm:text-sm flex items-center justify-center gap-2 overflow-hidden shadow-inner">
          <span>📢</span>
          <span className="truncate">{shopConfig.announcement}</span>
        </div>
      )}

      {/* Main Board Content (Split layout on wide screens) */}
      <main className="max-w-7xl mx-auto w-full px-4 sm:px-6 py-6 flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT / HERO: NOW SERVING */}
        <section className="lg:col-span-5 flex flex-col">
          <div className="h-full bg-slate-900 border-2 border-emerald-500/50 rounded-3xl p-6 sm:p-8 shadow-2xl flex flex-col justify-between relative overflow-hidden">
            <div className="absolute top-0 right-0 w-48 h-48 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

            <div>
              {/* Header Badge */}
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2.5">
                  <span className="relative flex h-4 w-4">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-500"></span>
                  </span>
                  <h2 className="text-sm font-black text-emerald-400 tracking-widest uppercase">
                    NOW IN CHAIR
                  </h2>
                </div>

                {servingTicket && (
                  <span className="px-3 py-1 rounded-full text-xs font-black bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 animate-pulse">
                    ACTIVE SERVICE
                  </span>
                )}
              </div>

              {servingTicket ? (
                <div className="space-y-6">
                  {/* Big Number & Customer Name */}
                  <div className="text-center sm:text-left">
                    <div className="inline-block text-6xl sm:text-7xl lg:text-8xl font-black text-emerald-400 tracking-tight drop-shadow-md">
                      #{servingTicket.queueNumber}
                    </div>
                    <h3 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white mt-1">
                      {servingTicket.customerName}
                    </h3>
                    <div className="flex items-center justify-center sm:justify-start gap-2 text-base sm:text-lg font-bold text-amber-300 mt-2">
                      <span>{SALON_SERVICES[servingTicket.serviceName]?.iconEmoji || '✂️'}</span>
                      <span>{SALON_SERVICES[servingTicket.serviceName]?.title}</span>
                    </div>
                  </div>

                  {/* Remaining Time Banner */}
                  {servingInfo && (() => {
                    const totalMin = SALON_SERVICES[servingTicket.serviceName]?.durationMinutes || 20;
                    const totalMs = totalMin * 60_000;
                    const elapsedMs = servingTicket.startedAt ? Math.max(0, currentTime - servingTicket.startedAt) : 0;
                    const progress = Math.min(100, Math.round((elapsedMs / totalMs) * 100));

                    return (
                      <div className="bg-slate-950/80 border border-emerald-500/30 rounded-2xl p-5 shadow-inner">
                        <div className="flex items-center justify-between text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                          <span>ESTIMATED TIME REMAINING</span>
                          <span className="text-emerald-400">{progress}% done</span>
                        </div>
                        <div className="text-3xl sm:text-4xl font-black text-emerald-400 font-mono tracking-tight">
                          {servingInfo.remainingServingFormatted}
                        </div>
                        <div className="w-full bg-slate-800 h-2.5 rounded-full overflow-hidden mt-3">
                          <div
                            className="bg-emerald-400 h-full rounded-full transition-all duration-1000"
                            style={{ width: `${progress}%` }}
                          />
                        </div>
                      </div>
                    );
                  })()}
                </div>
              ) : (
                <div className="py-16 text-center space-y-4">
                  <div className="w-20 h-20 rounded-3xl bg-slate-800/80 border border-slate-700 flex items-center justify-center text-4xl mx-auto text-slate-500">
                    🪑
                  </div>
                  <div>
                    <h3 className="text-xl font-extrabold text-slate-300">
                      Chair is Currently Available
                    </h3>
                    <p className="text-xs text-slate-500 mt-1">
                      {waitingTickets.length > 0
                        ? `${waitingTickets.length} customer(s) waiting in queue.`
                        : 'No customers in queue. Join now!'}
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Bottom prompt */}
            <div className="pt-6 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
              <span>Next up will be called shortly</span>
              <span className="font-bold text-amber-400">Student Salon</span>
            </div>
          </div>
        </section>

        {/* RIGHT: UPCOMING WAITING QUEUE */}
        <section className="lg:col-span-7 flex flex-col">
          <div className="h-full bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center font-black text-sm">
                    {waitingTickets.length}
                  </div>
                  <h2 className="text-base sm:text-lg font-black text-white tracking-wide uppercase">
                    UPCOMING CUSTOMERS IN QUEUE
                  </h2>
                </div>
                <span className="text-xs text-slate-400">
                  Real-time order
                </span>
              </div>

              {waitingTickets.length === 0 ? (
                <div className="py-20 text-center space-y-3">
                  <div className="text-4xl">✨</div>
                  <h3 className="text-base font-bold text-slate-300">
                    Queue is currently clear!
                  </h3>
                  <p className="text-xs text-slate-500 max-w-xs mx-auto">
                    Be the first in line by joining the digital queue on your phone.
                  </p>
                </div>
              ) : (
                <div className="space-y-3 max-h-[580px] overflow-y-auto pr-1">
                  {waitingTickets.map((ticket, index) => {
                    const info = calculateTicketInfo(ticket, todayTickets, currentTime);
                    const duration = SALON_SERVICES[ticket.serviceName]?.durationMinutes || 20;

                    return (
                      <div
                        key={ticket.id}
                        className={`p-4 rounded-2xl border transition-all flex items-center justify-between gap-4 ${
                          ticket.isRejoinedPriority
                            ? 'bg-cyan-950/40 border-cyan-500/50 shadow-md'
                            : index === 0
                            ? 'bg-slate-950 border-amber-500/50 shadow-lg'
                            : 'bg-slate-950/70 border-slate-800'
                        }`}
                      >
                        <div className="flex items-center gap-4 min-w-0">
                          {/* Position Badge */}
                          <div
                            className={`w-12 h-12 rounded-2xl flex flex-col items-center justify-center font-black ${
                              ticket.isRejoinedPriority
                                ? 'bg-cyan-500 text-slate-950'
                                : index === 0
                                ? 'bg-amber-400 text-slate-950'
                                : 'bg-slate-900 border border-slate-800 text-slate-300'
                            }`}
                          >
                            <span className="text-[10px] uppercase font-extrabold leading-none opacity-80">
                              POS
                            </span>
                            <span className="text-base font-black">
                              #{index + 1}
                            </span>
                          </div>

                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-lg sm:text-xl font-black text-amber-400">
                                #{ticket.queueNumber}
                              </span>
                              <h4 className="text-base sm:text-lg font-bold text-white truncate">
                                {ticket.customerName}
                              </h4>
                              {ticket.isRejoinedPriority && (
                                <span className="text-[10px] font-extrabold bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full border border-cyan-500/40">
                                  ↩ Priority
                                </span>
                              )}
                            </div>

                            <p className="text-xs text-slate-400 truncate mt-0.5">
                              {SALON_SERVICES[ticket.serviceName]?.iconEmoji}{' '}
                              {SALON_SERVICES[ticket.serviceName]?.title} ({duration}m)
                            </p>
                          </div>
                        </div>

                        {/* Timing Column */}
                        <div className="text-right shrink-0">
                          <span className="text-[10px] uppercase font-bold text-slate-500 block">
                            EST. WAIT
                          </span>
                          <span className="text-sm sm:text-base font-black text-slate-200">
                            {info.estimatedWaitingFormatted}
                          </span>
                          <span className="text-[11px] text-amber-400/80 font-mono block">
                            Turn: {info.estimatedTurnTimeFormatted}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Bottom info banner */}
            <div className="pt-4 mt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
              <span>Customers are served in listed sequence</span>
              <span className="font-semibold text-slate-300">
                Total Today: {todayTickets.length}
              </span>
            </div>
          </div>
        </section>
      </main>

      {/* Public Footer */}
      <footer className="bg-slate-900/80 border-t border-slate-800 px-6 py-3 text-xs text-slate-400">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span>📱</span>
            <span>Join the queue on your mobile phone without standing in line</span>
          </div>
          <SyncStatusIndicator syncStatus={syncStatus} />
        </div>
      </footer>
    </div>
  );
};
