import { useState } from 'react';
import type { FC } from 'react';
import { CustomerTicketInfo, SALON_SERVICES } from '../types/salon';

interface Props {
  ticketInfo: CustomerTicketInfo;
  onCancelTicket: (ticketId: string) => Promise<void>;
  onDismissTicket: (ticketId: string) => void;
}

export const DigitalTicketCard: FC<Props> = ({
  ticketInfo,
  onCancelTicket,
  onDismissTicket
}) => {
  const { ticket } = ticketInfo;
  const service = SALON_SERVICES[ticket.serviceName] || SALON_SERVICES.HAIRCUT;
  const isServing = ticket.statusName === 'SERVING';
  const isWaiting = ticket.statusName === 'WAITING';
  const isCompleted = ticket.statusName === 'COMPLETED';
  const isSkipped = ticket.statusName === 'SKIPPED';
  const isCancelled = ticket.statusName === 'CANCELLED';

  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  const handleConfirmCancel = async () => {
    try {
      setIsCancelling(true);
      await onCancelTicket(ticket.id);
      setShowCancelModal(false);
    } catch (e) {
      console.error('Cancel failed', e);
      alert('Failed to cancel ticket. Please try again.');
    } finally {
      setIsCancelling(false);
    }
  };

  // Calculate serving progress percentage
  const totalServiceMillis = service.durationMinutes * 60_000;
  const remainingMillis = ticketInfo.remainingServingMillis;
  const elapsedMillis = Math.max(0, totalServiceMillis - remainingMillis);
  const progressPercent = Math.min(100, Math.max(0, (elapsedMillis / totalServiceMillis) * 100));

  return (
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-b from-slate-800/90 to-slate-900/95 border-2 border-amber-500/60 shadow-2xl shadow-amber-950/40 p-5 space-y-4">
      {/* Top Background Glow */}
      <div className="absolute -top-16 -right-16 w-36 h-36 bg-amber-500/10 rounded-full blur-2xl pointer-events-none" />

      {/* Ticket Header */}
      <div className="flex items-center justify-between border-b border-slate-700/60 pb-3.5">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-300 font-extrabold text-lg shadow-inner">
            #{ticket.queueNumber}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-bold text-slate-100">{ticket.customerName}</h2>
              {ticket.isRejoinedPriority && (
                <span className="text-[10px] font-extrabold uppercase px-1.5 py-0.5 rounded bg-amber-500 text-slate-950">
                  Priority Rejoined
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400">
              {ticket.customerPhone ? ticket.customerPhone : 'No phone provided'}
            </p>
          </div>
        </div>

        {/* Status Badge */}
        <div
          className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 border ${
            isServing
              ? 'bg-emerald-950/80 border-emerald-500/60 text-emerald-300'
              : isWaiting
              ? 'bg-amber-950/60 border-amber-500/50 text-amber-300'
              : isCompleted
              ? 'bg-blue-950/60 border-blue-500/50 text-blue-300'
              : isSkipped
              ? 'bg-orange-950/60 border-orange-500/50 text-orange-300'
              : 'bg-rose-950/60 border-rose-500/50 text-rose-300'
          }`}
        >
          <span
            className={`w-2 h-2 rounded-full ${
              isServing
                ? 'bg-emerald-400 animate-pulse'
                : isWaiting
                ? 'bg-amber-400'
                : isCompleted
                ? 'bg-blue-400'
                : isSkipped
                ? 'bg-orange-400'
                : 'bg-rose-400'
            }`}
          />
          <span>{ticket.statusName}</span>
        </div>
      </div>

      {/* Service Info Banner */}
      <div className="flex items-center justify-between bg-slate-950/60 border border-slate-800 rounded-2xl p-3.5">
        <div className="flex items-center gap-3">
          <span className="text-2xl">{service.iconEmoji}</span>
          <div>
            <h3 className="text-sm font-semibold text-slate-200">{service.title}</h3>
            <p className="text-xs text-slate-400">{service.description}</p>
          </div>
        </div>
        <div className="text-right">
          <span className="text-xs font-bold text-amber-400 block">
            {service.durationMinutes} min
          </span>
          <span className="text-[10px] text-slate-500">Duration</span>
        </div>
      </div>

      {/* Live Serving Chair Mode */}
      {isServing && (
        <div className="bg-emerald-950/30 border border-emerald-800/60 rounded-2xl p-4 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-lg animate-bounce">💈</span>
              <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider">
                You Are In The Chair Now!
              </span>
            </div>
            <span className="text-xs font-mono font-bold text-emerald-300">
              {ticketInfo.remainingServingFormatted} left
            </span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden border border-slate-700">
            <div
              className="bg-gradient-to-r from-emerald-500 to-teal-400 h-2.5 rounded-full transition-all duration-1000 ease-linear"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <p className="text-[11px] text-slate-400 text-center">
            Service in progress. Estimated turn: <span className="text-emerald-300 font-semibold">NOW</span>
          </p>
        </div>
      )}

      {/* Waiting In Line Mode */}
      {isWaiting && (
        <div className="grid grid-cols-3 gap-2 text-center">
          <div className="bg-slate-950/70 border border-slate-800 rounded-2xl p-3">
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
              Ahead of You
            </span>
            <span className="text-lg font-extrabold text-amber-400 mt-1 block">
              {ticketInfo.customersAhead}
            </span>
            <span className="text-[10px] text-slate-500">
              {ticketInfo.customersAhead === 1 ? 'Customer' : 'Customers'}
            </span>
          </div>

          <div className="bg-slate-950/70 border border-slate-800 rounded-2xl p-3">
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
              Est. Wait
            </span>
            <span className="text-sm font-extrabold text-slate-100 mt-1.5 block line-clamp-1">
              {ticketInfo.estimatedWaitingFormatted}
            </span>
            <span className="text-[10px] text-slate-500">Dynamic</span>
          </div>

          <div className="bg-slate-950/70 border border-slate-800 rounded-2xl p-3">
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
              Est. Turn
            </span>
            <span className="text-sm font-extrabold text-amber-300 mt-1.5 block">
              {ticketInfo.estimatedTurnTimeFormatted}
            </span>
            <span className="text-[10px] text-slate-500">Approx.</span>
          </div>
        </div>
      )}

      {/* Skipped Mode Notice */}
      {isSkipped && (
        <div className="bg-orange-950/40 border border-orange-800/60 rounded-2xl p-3.5 text-center space-y-1">
          <p className="text-xs font-bold text-orange-300">You were skipped by the barber</p>
          <p className="text-[11px] text-slate-400">
            Please speak with the salon staff to rejoin with priority.
          </p>
        </div>
      )}

      {/* Completed / Cancelled Mode */}
      {(isCompleted || isCancelled) && (
        <div className="bg-slate-950/60 border border-slate-800 rounded-2xl p-3.5 text-center space-y-1">
          <p className="text-xs font-semibold text-slate-300">
            {isCompleted ? 'Service has been completed. Thank you!' : 'This ticket has been cancelled.'}
          </p>
          <button
            type="button"
            onClick={() => onDismissTicket(ticket.id)}
            className="mt-2 text-xs font-medium text-amber-400 underline hover:text-amber-300"
          >
            Dismiss Ticket
          </button>
        </div>
      )}

      {/* Action Footer */}
      {(isWaiting || isSkipped) && (
        <div className="pt-2">
          <button
            type="button"
            onClick={() => setShowCancelModal(true)}
            className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold text-rose-400 hover:text-rose-300 bg-rose-950/30 hover:bg-rose-950/50 border border-rose-900/50 transition-colors"
          >
            Cancel My Queue Ticket
          </button>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-3xl bg-slate-900 border border-slate-700 p-5 space-y-4 shadow-2xl">
            <h4 className="text-base font-bold text-slate-100">Cancel Queue Ticket?</h4>
            <p className="text-xs text-slate-400 leading-relaxed">
              Are you sure you want to cancel Ticket #{ticket.queueNumber} for {ticket.customerName}? You will lose your current spot in line.
            </p>
            <div className="flex gap-2.5 pt-2">
              <button
                type="button"
                disabled={isCancelling}
                onClick={() => setShowCancelModal(false)}
                className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-slate-800 text-slate-300 hover:bg-slate-700 border border-slate-700 transition-colors"
              >
                Keep Spot
              </button>
              <button
                type="button"
                disabled={isCancelling}
                onClick={handleConfirmCancel}
                className="flex-1 py-2.5 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-950/50 transition-colors disabled:opacity-50"
              >
                {isCancelling ? 'Cancelling...' : 'Yes, Cancel'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
