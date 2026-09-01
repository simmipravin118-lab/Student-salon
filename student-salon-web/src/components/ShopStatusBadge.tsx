import type { FC } from 'react';

interface Props {
  isOpen: boolean;
  openingHours?: string;
}

export const ShopStatusBadge: FC<Props> = ({ isOpen, openingHours = '08:00 AM - 09:00 PM' }) => {
  return (
    <div className="flex items-center justify-between bg-slate-800/80 border border-slate-700/80 rounded-xl px-3.5 py-2">
      <div className="flex items-center gap-2">
        <span
          className={`w-2.5 h-2.5 rounded-full ${
            isOpen ? 'bg-emerald-400 animate-pulse' : 'bg-rose-500'
          }`}
        />
        <span
          className={`text-xs font-bold uppercase tracking-wider ${
            isOpen ? 'text-emerald-400' : 'text-rose-400'
          }`}
        >
          {isOpen ? 'Salon Is Open' : 'Salon Is Closed'}
        </span>
      </div>
      <span className="text-xs text-slate-400 font-medium">
        {openingHours}
      </span>
    </div>
  );
};
