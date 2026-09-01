import { useEffect, useState } from 'react';
import { ensureAnonymousAuth } from './firebase/config';
import { CustomerView } from './views/CustomerView';
import { LiveQueueView } from './views/LiveQueueView';
import { OwnerDashboardView } from './views/OwnerDashboardView';

type AppTab = 'CUSTOMER' | 'LIVE_BOARD' | 'OWNER';

export default function App() {
  const [isAuthReady, setIsAuthReady] = useState<boolean>(false);
  const [currentTab, setCurrentTab] = useState<AppTab>('CUSTOMER');

  useEffect(() => {
    ensureAnonymousAuth()
      .then(() => {
        setIsAuthReady(true);
      })
      .catch((err) => {
        console.error('Anonymous auth init error:', err);
        setIsAuthReady(true); // proceed to render with offline/fallback support
      });
  }, []);

  if (!isAuthReady) {
    return (
      <div className="min-h-screen w-full bg-slate-950 flex flex-col items-center justify-center p-4 text-center">
        <div className="w-14 h-14 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-3xl mb-4 animate-pulse">
          💈
        </div>
        <h2 className="text-base font-bold text-amber-400">Student Salon 2</h2>
        <p className="text-xs text-slate-400 mt-1">Connecting to live cloud queue system...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col font-sans">
      {/* Top Application Navigation Bar */}
      <nav className="bg-slate-900 border-b border-slate-800 px-4 py-2 sticky top-0 z-40 shadow-lg">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xl">💈</span>
            <span className="font-black text-amber-400 text-sm tracking-tight hidden sm:inline">
              Student Salon 2
            </span>
          </div>

          {/* Navigation Mode Switcher */}
          <div className="flex items-center gap-1 bg-slate-950 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setCurrentTab('CUSTOMER')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                currentTab === 'CUSTOMER'
                  ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>🎟</span>
              <span>Customer</span>
            </button>

            <button
              onClick={() => setCurrentTab('LIVE_BOARD')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                currentTab === 'LIVE_BOARD'
                  ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>📺</span>
              <span>Live Board</span>
            </button>

            <button
              onClick={() => setCurrentTab('OWNER')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                currentTab === 'OWNER'
                  ? 'bg-amber-400 text-slate-950 shadow-md shadow-amber-400/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span>🔐</span>
              <span>Owner</span>
            </button>
          </div>
        </div>
      </nav>

      {/* Main Tab View */}
      <div className="flex-1">
        {currentTab === 'CUSTOMER' && <CustomerView />}
        {currentTab === 'LIVE_BOARD' && <LiveQueueView />}
        {currentTab === 'OWNER' && <OwnerDashboardView />}
      </div>
    </div>
  );
}
