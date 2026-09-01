import type { FC } from 'react';
import { CustomerTicketInfo } from '../types/salon';

interface Props {
  ticketsInfo: CustomerTicketInfo[];
  selectedTicketId: string | null;
  onSelectTicket: (ticketId: string) => void;
}

export const MultiTicketSwitcher: FC<Props> = ({
  ticketsInfo,
  selectedTicketId,
  onSelectTicket
}) => {
  if (ticketsInfo.length <= 1) return null;

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-xs text-slate-400">
        <span className="font-bold uppercase tracking-wider text-amber-400">
          Active Tickets ({ticketsInfo.length})
        </span>
        <span className="text-[11px] text-slate-400">Tap to switch view</span>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-thin">
        {ticketsInfo.map((info) => {
          const isSelected =
            info.ticket.id === (selectedTicketId || ticketsInfo[0].ticket.id);
          const isServing = info.ticket.statusName === 'SERVING';

          return (
            <button
              key={info.ticket.id}
              type="button"
              onClick={() => onSelectTicket(info.ticket.id)}
              className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold whitespace-nowrap border transition-all ${
                isSelected
                  ? 'bg-amber-950/40 border-amber-500 text-amber-300 ring-1 ring-amber-500/40'
                  : 'bg-slate-800/60 border-slate-700 text-slate-300 hover:bg-slate-800'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  isServing ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'
                }`}
              />
              <span>
                #{info.ticket.queueNumber} {info.ticket.customerName}
              </span>
              {isSelected && (
                <span className="text-[10px] bg-amber-500/20 text-amber-300 px-1.5 py-0.5 rounded font-bold">
                  Active
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};
