import type { FC } from 'react';
import { SyncStatus } from '../types/salon';

interface Props {
  syncStatus: SyncStatus;
}

export const SyncStatusIndicator: FC<Props> = ({ syncStatus }) => {
  const isLive = syncStatus.isCloudConnected && !syncStatus.isUsingLocalCache;

  return (
    <div
      className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
        isLive
          ? 'bg-slate-900/90 border-slate-700/80 text-slate-300'
          : 'bg-amber-950/60 border-amber-800/80 text-amber-300'
      }`}
    >
      <span
        className={`w-2 h-2 rounded-full ${
          isLive ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'
        }`}
      />
      <span className="text-[11px] tracking-wide">
        {isLive ? 'Live Cloud Synced' : 'Offline Mode • Showing Local Cache'}
      </span>
    </div>
  );
};
