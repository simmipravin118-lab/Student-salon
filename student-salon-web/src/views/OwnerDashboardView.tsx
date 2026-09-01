import React, { useState, useEffect, useMemo } from 'react';
import {
  ShopConfig,
  QueueTicket,
  SyncStatus,
  SALON_SERVICES
} from '../types/salon';
import {
  subscribeToShopConfig,
  subscribeToTodayTickets,
  startCustomer,
  completeService,
  skipCustomer,
  rejoinCustomer,
  setShopOpen,
  updateAnnouncement,
  resetTodayQueue,
  seedDemoQueue,
  cancelCustomerTicket
} from '../services/salonService';
import {
  calculateDailySummary,
  calculateTicketInfo,
  getTodayDateString
} from '../utils/timingEngine';
import { SyncStatusIndicator } from '../components/SyncStatusIndicator';

type OwnerTab = 'WAITING' | 'SERVING' | 'SKIPPED' | 'COMPLETED';

export const OwnerDashboardView: React.FC = () => {
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

  // Authentication State
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return sessionStorage.getItem('owner_authenticated') === 'true';
  });
  const [pinInput, setPinInput] = useState<string>('');
  const [pinError, setPinError] = useState<string | null>(null);

  // UI State
  const [activeTab, setActiveTab] = useState<OwnerTab>('WAITING');
  const [currentTime, setCurrentTime] = useState<number>(Date.now());
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [showResetModal, setShowResetModal] = useState<boolean>(false);
  const [showAnnouncementModal, setShowAnnouncementModal] = useState<boolean>(false);
  const [announcementDraft, setAnnouncementDraft] = useState<string>('');
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);

  // 1-second live countdown interval
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Subscribe to Shop Config
  useEffect(() => {
    const unsubscribe = subscribeToShopConfig(
      (config) => {
        setShopConfig(config);
        setAnnouncementDraft(config.announcement);
      },
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

  const handleLogin = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const expectedPin = shopConfig.ownerPin || '1234';
    if (pinInput.trim() === expectedPin) {
      setIsAuthenticated(true);
      sessionStorage.setItem('owner_authenticated', 'true');
      setPinError(null);
      setPinInput('');
    } else {
      setPinError(`Incorrect PIN. (Default: 1234)`);
    }
  };

  const handleQuickDemoUnlock = () => {
    setIsAuthenticated(true);
    sessionStorage.setItem('owner_authenticated', 'true');
    setPinError(null);
    setPinInput('');
  };

  const handleLogout = () => {
    setIsAuthenticated(false);
    sessionStorage.removeItem('owner_authenticated');
  };

  const notifyAction = (msg: string) => {
    setActionSuccessMsg(msg);
    setTimeout(() => setActionSuccessMsg(null), 3000);
  };

  // Ticket Grouping
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

  const skippedTickets = useMemo(
    () => todayTickets.filter((t) => t.statusName === 'SKIPPED'),
    [todayTickets]
  );

  const completedTickets = useMemo(
    () => todayTickets.filter((t) => t.statusName === 'COMPLETED'),
    [todayTickets]
  );

  const dailySummary = useMemo(
    () => calculateDailySummary(todayTickets),
    [todayTickets]
  );

  const nextCustomerInLine = waitingTickets.length > 0 ? waitingTickets[0] : null;

  // Actions
  const handleStartCustomer = async (ticketId: string, customerName: string) => {
    try {
      setIsProcessing(true);
      await startCustomer(ticketId);
      notifyAction(`Started service for ${customerName}`);
    } catch (err: any) {
      alert(`Failed to start service: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCompleteService = async (ticketId: string, customerName: string) => {
    try {
      setIsProcessing(true);
      await completeService(ticketId);
      notifyAction(`Service completed for ${customerName}`);
    } catch (err: any) {
      alert(`Failed to complete service: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSkipCustomer = async (ticketId: string, customerName: string) => {
    try {
      setIsProcessing(true);
      await skipCustomer(ticketId);
      notifyAction(`Customer ${customerName} moved to Skipped list`);
    } catch (err: any) {
      alert(`Failed to skip customer: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRejoinCustomer = async (ticketId: string, customerName: string) => {
    try {
      setIsProcessing(true);
      await rejoinCustomer(ticketId);
      notifyAction(`Customer ${customerName} rejoined queue with Priority`);
    } catch (err: any) {
      alert(`Failed to rejoin customer: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleCancelTicket = async (ticketId: string, customerName: string) => {
    if (!window.confirm(`Are you sure you want to cancel ticket for ${customerName}?`)) {
      return;
    }
    try {
      setIsProcessing(true);
      await cancelCustomerTicket(ticketId);
      notifyAction(`Cancelled ticket for ${customerName}`);
    } catch (err: any) {
      alert(`Failed to cancel ticket: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleShopOpen = async () => {
    try {
      setIsProcessing(true);
      const newStatus = !shopConfig.isOpen;
      await setShopOpen(newStatus);
      notifyAction(newStatus ? 'Salon opened for queue intake' : 'Salon closed (Queue intake paused)');
    } catch (err: any) {
      alert(`Failed to update salon status: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSaveAnnouncement = async () => {
    try {
      setIsProcessing(true);
      await updateAnnouncement(announcementDraft);
      setShowAnnouncementModal(false);
      notifyAction('Salon announcement updated');
    } catch (err: any) {
      alert(`Failed to update announcement: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleResetQueue = async () => {
    try {
      setIsProcessing(true);
      await resetTodayQueue();
      setShowResetModal(false);
      notifyAction("Today's queue was successfully reset to #1");
    } catch (err: any) {
      alert(`Failed to reset queue: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSeedDemo = async () => {
    try {
      setIsProcessing(true);
      await seedDemoQueue();
      notifyAction('Loaded 5 demo queue tickets');
    } catch (err: any) {
      alert(`Failed to load demo data: ${err.message || err}`);
    } finally {
      setIsProcessing(false);
    }
  };

  // Render Login Screen if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-4">
        <div className="w-full max-w-md bg-slate-900 border border-amber-500/30 rounded-3xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
          <div className="absolute -top-24 -right-24 w-48 h-48 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
          
          <div className="flex flex-col items-center text-center mb-6">
            <div className="w-16 h-16 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-3xl mb-3 shadow-inner">
              🔐
            </div>
            <h1 className="text-xl font-extrabold text-amber-400 tracking-wider">
              STUDENT SALON 2
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Owner Management & Queue Control
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                Owner Security PIN
              </label>
              <input
                type="password"
                inputMode="numeric"
                maxLength={6}
                value={pinInput}
                onChange={(e) => {
                  setPinInput(e.target.value);
                  setPinError(null);
                }}
                placeholder="Enter PIN (Default: 1234)"
                className="w-full px-4 py-3.5 bg-slate-950 border border-slate-700 rounded-xl text-center text-xl tracking-widest font-mono text-amber-400 placeholder:text-slate-600 focus:outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/20"
                autoFocus
              />
              {pinError && (
                <p className="text-rose-400 text-xs mt-2 text-center font-medium">
                  {pinError}
                </p>
              )}
            </div>

            <button
              type="submit"
              className="w-full py-3.5 bg-amber-400 hover:bg-amber-300 active:scale-[0.98] text-slate-950 font-extrabold rounded-xl transition-all shadow-lg shadow-amber-400/20 flex items-center justify-center gap-2"
            >
              <span>🔓</span>
              <span>Unlock Dashboard</span>
            </button>

            <div className="pt-2 text-center">
              <button
                type="button"
                onClick={handleQuickDemoUnlock}
                className="text-xs text-amber-400/80 hover:text-amber-300 underline underline-offset-4 py-1"
              >
                Quick Demo Unlock (PIN: 1234)
              </button>
            </div>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-800 flex justify-center">
            <SyncStatusIndicator syncStatus={syncStatus} />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pb-16">
      {/* Action Notification Toast */}
      {actionSuccessMsg && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-500 text-slate-950 font-bold px-4 py-2.5 rounded-xl shadow-2xl border border-emerald-300 flex items-center gap-2 animate-bounce">
          <span>✓</span>
          <span>{actionSuccessMsg}</span>
        </div>
      )}

      {/* Top Owner Header */}
      <header className="bg-slate-900 border-b border-slate-800 sticky top-0 z-30 shadow-md">
        <div className="max-w-6xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-xl">
              💈
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-extrabold text-amber-400 tracking-tight">
                  {shopConfig.shopName || 'Student Salon 2'}
                </h1>
                <span className="text-[10px] uppercase font-bold bg-amber-500/20 text-amber-300 px-2 py-0.5 rounded-full border border-amber-500/30">
                  Owner Center
                </span>
              </div>
              <p className="text-xs text-slate-400">
                {shopConfig.location || 'Telo, Bokaro'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <SyncStatusIndicator syncStatus={syncStatus} />
            <button
              onClick={handleLogout}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 active:bg-slate-600 text-slate-300 hover:text-white rounded-lg text-xs font-semibold border border-slate-700 flex items-center gap-1.5 transition"
              title="Lock dashboard"
            >
              <span>🔒</span>
              <span className="hidden sm:inline">Lock / Exit</span>
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6">
        {/* Salon Status & Quick Controls Banner */}
        <section className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xl grid grid-cols-1 md:grid-cols-3 gap-4 items-center">
          {/* Shop Open/Closed Toggle */}
          <div className="md:col-span-2 flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-slate-950/70 border border-slate-800">
            <div className="flex items-center gap-3">
              <div
                className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl font-black ${
                  shopConfig.isOpen
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                    : 'bg-rose-500/20 text-rose-400 border border-rose-500/40'
                }`}
              >
                {shopConfig.isOpen ? '🟢' : '🔴'}
              </div>
              <div>
                <h2 className="text-sm font-black tracking-wide text-white uppercase flex items-center gap-2">
                  <span>{shopConfig.isOpen ? 'SALON IS OPEN' : 'SALON IS CLOSED'}</span>
                </h2>
                <p className="text-xs text-slate-400">
                  {shopConfig.isOpen
                    ? 'Customers can join the live digital queue'
                    : 'Queue intake paused • New customers cannot join'}
                </p>
              </div>
            </div>

            <button
              onClick={handleToggleShopOpen}
              disabled={isProcessing}
              className={`px-4 py-2.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all shadow-md active:scale-95 flex items-center justify-center gap-2 ${
                shopConfig.isOpen
                  ? 'bg-rose-600 hover:bg-rose-500 text-white'
                  : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950'
              }`}
            >
              <span>{shopConfig.isOpen ? 'Pause Intake / Close' : 'Open Salon Queue'}</span>
            </button>
          </div>

          {/* Announcement Box */}
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-1">
                <span className="text-[10px] uppercase font-bold text-amber-400 tracking-wider">
                  📢 Announcement
                </span>
                <button
                  onClick={() => setShowAnnouncementModal(true)}
                  className="text-xs text-amber-400 hover:text-amber-300 font-semibold underline underline-offset-2"
                >
                  Edit
                </button>
              </div>
              <p className="text-xs text-slate-300 line-clamp-2 italic">
                "{shopConfig.announcement || 'No announcement posted'}"
              </p>
            </div>
          </div>
        </section>

        {/* Hero: Active Serving Chair & Next In Line */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Serving Customer Card */}
          <div className="lg:col-span-2 bg-slate-900 border-2 border-emerald-500/40 rounded-3xl p-5 sm:p-6 shadow-2xl relative overflow-hidden">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
                </span>
                <h3 className="text-xs font-black text-emerald-400 uppercase tracking-widest">
                  NOW IN CHAIR (ACTIVE SERVICE)
                </h3>
              </div>
              {servingTicket && (
                <span className="px-2.5 py-0.5 rounded-full text-[11px] font-extrabold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  SERVING
                </span>
              )}
            </div>

            {servingTicket ? (
              (() => {
                const info = calculateTicketInfo(servingTicket, todayTickets, currentTime);
                const totalMinutes = SALON_SERVICES[servingTicket.serviceName]?.durationMinutes || 20;
                const totalMillis = totalMinutes * 60_000;
                const elapsedMillis = servingTicket.startedAt ? Math.max(0, currentTime - servingTicket.startedAt) : 0;
                const progressPct = Math.min(100, Math.round((elapsedMillis / totalMillis) * 100));

                return (
                  <div className="space-y-5">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div>
                        <div className="flex items-baseline gap-3">
                          <span className="text-4xl sm:text-5xl font-black text-emerald-400">
                            #{servingTicket.queueNumber}
                          </span>
                          <div>
                            <h4 className="text-xl sm:text-2xl font-black text-white">
                              {servingTicket.customerName}
                            </h4>
                            {servingTicket.customerPhone && (
                              <a
                                href={`tel:${servingTicket.customerPhone}`}
                                className="text-xs text-slate-400 hover:text-amber-400 flex items-center gap-1 font-mono mt-0.5"
                              >
                                <span>📞</span>
                                <span>{servingTicket.customerPhone}</span>
                              </a>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center gap-2 mt-2">
                          <span className="text-base">
                            {SALON_SERVICES[servingTicket.serviceName]?.iconEmoji || '✂️'}
                          </span>
                          <span className="text-sm font-bold text-amber-300">
                            {SALON_SERVICES[servingTicket.serviceName]?.title}
                          </span>
                          <span className="text-xs text-slate-400">
                            ({totalMinutes} min standard)
                          </span>
                        </div>
                        {servingTicket.notes && (
                          <p className="text-xs text-slate-300 mt-1.5 italic bg-slate-950/60 px-3 py-1 rounded-lg border border-slate-800 inline-block">
                            Note: {servingTicket.notes}
                          </p>
                        )}
                      </div>

                      {/* Remaining Time Ticker */}
                      <div className="bg-slate-950/80 border border-emerald-500/30 rounded-2xl p-4 text-center min-w-[140px]">
                        <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                          REMAINING TIME
                        </span>
                        <span className="text-2xl sm:text-3xl font-black text-emerald-400 font-mono block mt-0.5">
                          {info.remainingServingFormatted}
                        </span>
                        <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden mt-2">
                          <div
                            className="bg-emerald-500 h-full rounded-full transition-all duration-1000"
                            style={{ width: `${progressPct}%` }}
                          />
                        </div>
                      </div>
                    </div>

                    {/* Operational Action Buttons */}
                    <div className="pt-2 flex flex-wrap items-center gap-3">
                      <button
                        onClick={() => handleCompleteService(servingTicket.id, servingTicket.customerName)}
                        disabled={isProcessing}
                        className="flex-1 py-3 px-4 bg-emerald-500 hover:bg-emerald-400 active:scale-[0.98] text-slate-950 font-black rounded-xl text-sm transition shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
                      >
                        <span>✓</span>
                        <span>Complete Service</span>
                      </button>

                      <button
                        onClick={() => handleSkipCustomer(servingTicket.id, servingTicket.customerName)}
                        disabled={isProcessing}
                        className="py-3 px-4 bg-amber-500/20 hover:bg-amber-500/30 active:scale-[0.98] text-amber-400 border border-amber-500/40 font-bold rounded-xl text-sm transition flex items-center justify-center gap-2"
                      >
                        <span>⏭</span>
                        <span>Skip Customer</span>
                      </button>
                    </div>
                  </div>
                );
              })()
            ) : (
              <div className="py-8 text-center space-y-4">
                <div className="w-14 h-14 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-2xl mx-auto text-slate-500">
                  🪑
                </div>
                <div>
                  <h4 className="text-base font-bold text-slate-300">
                    Chair is currently empty
                  </h4>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {waitingTickets.length > 0
                      ? `${waitingTickets.length} customer(s) waiting in queue.`
                      : 'No customers currently waiting.'}
                  </p>
                </div>

                {nextCustomerInLine && (
                  <button
                    onClick={() => handleStartCustomer(nextCustomerInLine.id, nextCustomerInLine.customerName)}
                    disabled={isProcessing}
                    className="py-3 px-6 bg-amber-400 hover:bg-amber-300 active:scale-[0.98] text-slate-950 font-extrabold rounded-xl text-sm transition shadow-lg shadow-amber-400/20 inline-flex items-center gap-2"
                  >
                    <span>▶</span>
                    <span>Start Next Customer (#{nextCustomerInLine.queueNumber} - {nextCustomerInLine.customerName})</span>
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Up Next in Line Quick Preview Card */}
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-5 sm:p-6 shadow-xl flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-xs font-black text-cyan-400 uppercase tracking-widest">
                  NEXT IN LINE
                </h3>
                {nextCustomerInLine?.isRejoinedPriority && (
                  <span className="text-[10px] font-extrabold bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full border border-cyan-500/40">
                    ↩ Priority Rejoined
                  </span>
                )}
              </div>

              {nextCustomerInLine ? (
                <div className="space-y-3">
                  <div className="flex items-baseline gap-2">
                    <span className="text-3xl font-black text-white">
                      #{nextCustomerInLine.queueNumber}
                    </span>
                    <span className="text-lg font-bold text-amber-400">
                      {nextCustomerInLine.customerName}
                    </span>
                  </div>

                  <div className="p-3 bg-slate-950/70 rounded-xl border border-slate-800 space-y-1 text-xs">
                    <div className="flex justify-between text-slate-400">
                      <span>Service:</span>
                      <span className="font-semibold text-slate-200">
                        {SALON_SERVICES[nextCustomerInLine.serviceName]?.iconEmoji}{' '}
                        {SALON_SERVICES[nextCustomerInLine.serviceName]?.title}
                      </span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>Est. Duration:</span>
                      <span className="font-semibold text-slate-200">
                        {SALON_SERVICES[nextCustomerInLine.serviceName]?.durationMinutes} min
                      </span>
                    </div>
                    {nextCustomerInLine.customerPhone && (
                      <div className="flex justify-between text-slate-400 font-mono">
                        <span>Phone:</span>
                        <a
                          href={`tel:${nextCustomerInLine.customerPhone}`}
                          className="text-amber-400 hover:underline"
                        >
                          {nextCustomerInLine.customerPhone}
                        </a>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="py-6 text-center text-slate-500 text-xs">
                  No upcoming customer in line.
                </div>
              )}
            </div>

            {nextCustomerInLine && (
              <button
                onClick={() => handleStartCustomer(nextCustomerInLine.id, nextCustomerInLine.customerName)}
                disabled={isProcessing}
                className="mt-4 w-full py-2.5 bg-cyan-500 hover:bg-cyan-400 active:scale-[0.98] text-slate-950 font-black rounded-xl text-xs uppercase tracking-wider transition shadow-md flex items-center justify-center gap-1.5"
              >
                <span>▶ Start Service</span>
              </button>
            )}
          </div>
        </section>

        {/* Daily Summary & Analytics Metric Row */}
        <section className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 sm:p-5 shadow-lg">
          <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest mb-3">
            TODAY'S SUMMARY & ANALYTICS
          </h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-slate-500 block">TOTAL</span>
              <span className="text-xl font-black text-amber-400">{dailySummary.totalCustomers}</span>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-amber-500/80 block">WAITING</span>
              <span className="text-xl font-black text-amber-400">{dailySummary.waitingCount}</span>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-emerald-500/80 block">SERVING</span>
              <span className="text-xl font-black text-emerald-400">{dailySummary.servingCount}</span>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-sky-500/80 block">COMPLETED</span>
              <span className="text-xl font-black text-sky-400">{dailySummary.completedCount}</span>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-orange-500/80 block">SKIPPED</span>
              <span className="text-xl font-black text-orange-400">{dailySummary.skippedCount}</span>
            </div>
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-center">
              <span className="text-[10px] uppercase font-bold text-rose-500/80 block">CANCELLED</span>
              <span className="text-xl font-black text-rose-400">{dailySummary.cancelledCount}</span>
            </div>
          </div>
        </section>

        {/* Queue Management Tabbed Section */}
        <section className="bg-slate-900 border border-slate-800 rounded-3xl p-5 sm:p-6 shadow-2xl">
          {/* Tab Navigation */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-4 mb-5">
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              <button
                onClick={() => setActiveTab('WAITING')}
                className={`px-4 py-2 rounded-xl text-xs font-extrabold uppercase tracking-wider transition-all flex items-center gap-2 ${
                  activeTab === 'WAITING'
                    ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                    : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <span>Waiting</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] bg-slate-950/40 text-current">
                  {waitingTickets.length}
                </span>
              </button>

              <button
                onClick={() => setActiveTab('SERVING')}
                className={`px-4 py-2 rounded-xl text-xs font-extrabold uppercase tracking-wider transition-all flex items-center gap-2 ${
                  activeTab === 'SERVING'
                    ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20'
                    : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <span>Serving</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] bg-slate-950/40 text-current">
                  {servingTicket ? 1 : 0}
                </span>
              </button>

              <button
                onClick={() => setActiveTab('SKIPPED')}
                className={`px-4 py-2 rounded-xl text-xs font-extrabold uppercase tracking-wider transition-all flex items-center gap-2 ${
                  activeTab === 'SKIPPED'
                    ? 'bg-orange-500 text-slate-950 shadow-md shadow-orange-500/20'
                    : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <span>Skipped (Rejoin)</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] bg-slate-950/40 text-current">
                  {skippedTickets.length}
                </span>
              </button>

              <button
                onClick={() => setActiveTab('COMPLETED')}
                className={`px-4 py-2 rounded-xl text-xs font-extrabold uppercase tracking-wider transition-all flex items-center gap-2 ${
                  activeTab === 'COMPLETED'
                    ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20'
                    : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                <span>Completed</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] bg-slate-950/40 text-current">
                  {completedTickets.length}
                </span>
              </button>
            </div>

            {/* Quick Action in Header */}
            {activeTab === 'WAITING' && waitingTickets.length > 0 && (
              <button
                onClick={() => handleStartCustomer(waitingTickets[0].id, waitingTickets[0].customerName)}
                disabled={isProcessing}
                className="px-3.5 py-1.5 bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold rounded-xl text-xs flex items-center gap-1.5 transition shadow"
              >
                <span>▶</span>
                <span>Start Next (# {waitingTickets[0].queueNumber})</span>
              </button>
            )}
          </div>

          {/* TAB 1: WAITING QUEUE */}
          {activeTab === 'WAITING' && (
            <div className="space-y-3">
              {waitingTickets.length === 0 ? (
                <div className="py-12 text-center text-slate-400 text-xs">
                  No customers waiting in queue.
                </div>
              ) : (
                waitingTickets.map((ticket, index) => {
                  const info = calculateTicketInfo(ticket, todayTickets, currentTime);
                  const duration = SALON_SERVICES[ticket.serviceName]?.durationMinutes || 20;

                  return (
                    <div
                      key={ticket.id}
                      className={`p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                        ticket.isRejoinedPriority
                          ? 'bg-cyan-950/30 border-cyan-500/40'
                          : 'bg-slate-950/80 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center gap-3 sm:gap-4">
                        <div
                          className={`w-10 h-10 rounded-xl flex items-center justify-center font-black text-sm ${
                            ticket.isRejoinedPriority
                              ? 'bg-cyan-500 text-slate-950'
                              : 'bg-slate-900 border border-slate-800 text-amber-400'
                          }`}
                        >
                          #{index + 1}
                        </div>

                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-extrabold text-white text-sm sm:text-base">
                              #{ticket.queueNumber} - {ticket.customerName}
                            </span>
                            {ticket.isRejoinedPriority && (
                              <span className="text-[10px] font-extrabold bg-cyan-500/20 text-cyan-300 px-2 py-0.5 rounded-full border border-cyan-500/40">
                                ↩ Rejoined
                              </span>
                            )}
                          </div>

                          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400 mt-1">
                            <span>
                              {SALON_SERVICES[ticket.serviceName]?.iconEmoji}{' '}
                              {SALON_SERVICES[ticket.serviceName]?.title} ({duration}m)
                            </span>
                            {ticket.customerPhone && (
                              <a
                                href={`tel:${ticket.customerPhone}`}
                                className="text-amber-400 hover:underline font-mono"
                              >
                                {ticket.customerPhone}
                              </a>
                            )}
                            <span className="text-slate-500">
                              Est. Wait: <strong className="text-slate-300">{info.estimatedWaitingFormatted}</strong>
                            </span>
                          </div>

                          {ticket.notes && (
                            <p className="text-[11px] text-slate-400 italic mt-1">
                              Note: {ticket.notes}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Ticket Actions */}
                      <div className="flex items-center gap-2 self-end sm:self-center">
                        <button
                          onClick={() => handleSkipCustomer(ticket.id, ticket.customerName)}
                          disabled={isProcessing}
                          className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-orange-400 border border-orange-500/30 rounded-xl text-xs font-semibold transition"
                          title="Skip turn"
                        >
                          ⏭ Skip
                        </button>
                        <button
                          onClick={() => handleStartCustomer(ticket.id, ticket.customerName)}
                          disabled={isProcessing}
                          className="px-4 py-1.5 bg-emerald-500 hover:bg-emerald-400 active:scale-95 text-slate-950 font-extrabold rounded-xl text-xs transition shadow-sm"
                        >
                          ▶ Start
                        </button>
                        <button
                          onClick={() => handleCancelTicket(ticket.id, ticket.customerName)}
                          disabled={isProcessing}
                          className="p-1.5 text-slate-500 hover:text-rose-400 rounded-lg transition text-xs"
                          title="Cancel ticket"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {/* TAB 2: SERVING QUEUE */}
          {activeTab === 'SERVING' && (
            <div>
              {servingTicket ? (
                <div className="p-4 rounded-2xl bg-emerald-950/20 border border-emerald-500/40 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-black text-emerald-400">
                        #{servingTicket.queueNumber} - {servingTicket.customerName}
                      </span>
                      <span className="text-[10px] font-extrabold bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-500/40">
                        Active In Chair
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">
                      {SALON_SERVICES[servingTicket.serviceName]?.iconEmoji}{' '}
                      {SALON_SERVICES[servingTicket.serviceName]?.title} • Phone:{' '}
                      {servingTicket.customerPhone || 'N/A'}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleCompleteService(servingTicket.id, servingTicket.customerName)}
                      disabled={isProcessing}
                      className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold rounded-xl text-xs transition"
                    >
                      ✓ Complete Service
                    </button>
                    <button
                      onClick={() => handleSkipCustomer(servingTicket.id, servingTicket.customerName)}
                      disabled={isProcessing}
                      className="px-3 py-2 bg-slate-900 text-orange-400 border border-orange-500/30 font-bold rounded-xl text-xs transition"
                    >
                      ⏭ Skip
                    </button>
                  </div>
                </div>
              ) : (
                <div className="py-12 text-center text-slate-400 text-xs">
                  No customer is currently in the chair.
                </div>
              )}
            </div>
          )}

          {/* TAB 3: SKIPPED QUEUE (REJOIN) */}
          {activeTab === 'SKIPPED' && (
            <div className="space-y-3">
              <div className="p-3 rounded-xl bg-orange-500/10 border border-orange-500/30 text-xs text-orange-300">
                💡 <strong>Skipped Customers:</strong> Click <strong>"↩ Rejoin Priority"</strong> to insert them back into the waiting line with top priority.
              </div>

              {skippedTickets.length === 0 ? (
                <div className="py-12 text-center text-slate-400 text-xs">
                  No skipped customers today.
                </div>
              ) : (
                skippedTickets.map((ticket) => (
                  <div
                    key={ticket.id}
                    className="p-4 rounded-2xl bg-slate-950 border border-orange-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-white text-sm">
                          #{ticket.queueNumber} - {ticket.customerName}
                        </span>
                        <span className="text-[10px] font-extrabold bg-orange-500/20 text-orange-400 px-2 py-0.5 rounded-full border border-orange-500/30">
                          SKIPPED
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mt-1">
                        {SALON_SERVICES[ticket.serviceName]?.iconEmoji}{' '}
                        {SALON_SERVICES[ticket.serviceName]?.title} • Phone:{' '}
                        {ticket.customerPhone || 'N/A'}
                      </p>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <button
                        onClick={() => handleRejoinCustomer(ticket.id, ticket.customerName)}
                        disabled={isProcessing}
                        className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-black rounded-xl text-xs transition shadow-sm flex items-center gap-1.5"
                      >
                        <span>↩</span>
                        <span>Rejoin with Priority</span>
                      </button>
                      <button
                        onClick={() => handleCancelTicket(ticket.id, ticket.customerName)}
                        disabled={isProcessing}
                        className="p-2 text-slate-500 hover:text-rose-400 rounded-lg transition text-xs"
                        title="Cancel"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* TAB 4: COMPLETED TICKETS */}
          {activeTab === 'COMPLETED' && (
            <div className="space-y-3">
              {completedTickets.length === 0 ? (
                <div className="py-12 text-center text-slate-400 text-xs">
                  No completed customers yet today.
                </div>
              ) : (
                completedTickets.map((ticket) => (
                  <div
                    key={ticket.id}
                    className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-200 text-sm">
                          #{ticket.queueNumber} - {ticket.customerName}
                        </span>
                        <span className="text-[10px] font-extrabold bg-sky-500/20 text-sky-400 px-2 py-0.5 rounded-full">
                          COMPLETED
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">
                        {SALON_SERVICES[ticket.serviceName]?.iconEmoji}{' '}
                        {SALON_SERVICES[ticket.serviceName]?.title}
                      </p>
                    </div>
                    {ticket.completedAt && (
                      <span className="text-xs text-slate-500 font-mono">
                        {new Date(ticket.completedAt).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </span>
                    )}
                  </div>
                ))
              )}
            </div>
          )}
        </section>

        {/* Danger Zone / Admin Utilities */}
        <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h4 className="text-xs font-black text-slate-400 uppercase tracking-widest">
              ADMINISTRATIVE UTILITIES
            </h4>
            <p className="text-xs text-slate-500 mt-1">
              Load demo testing tickets or reset today's queue sequence for a fresh business day.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleSeedDemo}
              disabled={isProcessing}
              className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 active:scale-95 text-amber-400 border border-amber-500/30 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
            >
              <span>🔄</span>
              <span>Load Demo Queue</span>
            </button>

            <button
              onClick={() => setShowResetModal(true)}
              disabled={isProcessing}
              className="px-3.5 py-2 bg-rose-600/20 hover:bg-rose-600/30 active:scale-95 text-rose-400 border border-rose-500/40 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
            >
              <span>⚠️</span>
              <span>Reset Today's Queue</span>
            </button>
          </div>
        </section>
      </main>

      {/* Announcement Edit Modal */}
      {showAnnouncementModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-amber-500/30 rounded-3xl p-6 w-full max-w-md shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-extrabold text-amber-400">
                Update Salon Announcement
              </h3>
              <button
                onClick={() => setShowAnnouncementModal(false)}
                className="text-slate-400 hover:text-white text-lg"
              >
                ✕
              </button>
            </div>

            <textarea
              rows={3}
              value={announcementDraft}
              onChange={(e) => setAnnouncementDraft(e.target.value)}
              placeholder="e.g. Special student discount today! Digital queue is active."
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:border-amber-400"
            />

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAnnouncementModal(false)}
                className="px-4 py-2 bg-slate-800 text-slate-300 font-semibold rounded-xl text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveAnnouncement}
                disabled={isProcessing}
                className="px-4 py-2 bg-amber-400 hover:bg-amber-300 text-slate-950 font-extrabold rounded-xl text-xs"
              >
                Save Announcement
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reset Queue Confirmation Modal */}
      {showResetModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-rose-500/40 rounded-3xl p-6 w-full max-w-md shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-500/20 text-rose-400 flex items-center justify-center text-2xl mx-auto">
              ⚠️
            </div>

            <div className="text-center">
              <h3 className="text-lg font-black text-rose-400">
                Reset Today's Queue?
              </h3>
              <p className="text-xs text-slate-300 mt-2">
                This will delete all queue tickets for today and restart queue numbering at <strong>#1</strong>. This action cannot be undone.
              </p>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowResetModal(false)}
                className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold rounded-xl text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleResetQueue}
                disabled={isProcessing}
                className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white font-extrabold rounded-xl text-xs shadow-lg shadow-rose-600/30"
              >
                Yes, Reset Queue
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
