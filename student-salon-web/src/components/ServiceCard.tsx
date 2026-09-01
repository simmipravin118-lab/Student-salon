import type { FC } from 'react';
import { ServiceDefinition } from '../types/salon';

interface Props {
  service: ServiceDefinition;
  isSelected: boolean;
  onSelect: (serviceId: ServiceDefinition['id']) => void;
}

export const ServiceCard: FC<Props> = ({ service, isSelected, onSelect }) => {
  return (
    <button
      type="button"
      onClick={() => onSelect(service.id)}
      className={`w-full text-left p-3.5 rounded-2xl border transition-all flex items-center justify-between ${
        isSelected
          ? 'bg-amber-950/30 border-amber-500 shadow-lg shadow-amber-950/50 ring-1 ring-amber-500/50'
          : 'bg-slate-800/40 border-slate-700/60 hover:border-slate-600 hover:bg-slate-800/60'
      }`}
    >
      <div className="flex items-center gap-3.5">
        <div
          className={`w-11 h-11 rounded-xl flex items-center justify-center text-xl transition-colors ${
            isSelected
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
              : 'bg-slate-800 border border-slate-700 text-slate-300'
          }`}
        >
          {service.iconEmoji}
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h3
              className={`text-sm font-semibold transition-colors ${
                isSelected ? 'text-amber-300' : 'text-slate-100'
              }`}
            >
              {service.title}
            </h3>
            {isSelected && (
              <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-amber-500 text-slate-950">
                Selected
              </span>
            )}
          </div>
          <p className="text-xs text-slate-400 mt-0.5 line-clamp-1">
            {service.description}
          </p>
        </div>
      </div>

      <div
        className={`px-2.5 py-1 rounded-lg text-xs font-bold whitespace-nowrap ml-2 border ${
          isSelected
            ? 'bg-amber-500/20 border-amber-500/50 text-amber-300'
            : 'bg-slate-800 border-slate-700 text-slate-400'
        }`}
      >
        {service.durationMinutes} min
      </div>
    </button>
  );
};
