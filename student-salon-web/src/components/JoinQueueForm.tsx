import { useState } from 'react';
import type { FC, FormEvent } from 'react';
import { SALON_SERVICES, ServiceType } from '../types/salon';
import { ServiceCard } from './ServiceCard';

interface Props {
  isOpen: boolean;
  onJoin: (name: string, phone: string, service: ServiceType, notes: string) => Promise<void>;
}

export const JoinQueueForm: FC<Props> = ({ isOpen, onJoin }) => {
  const [selectedService, setSelectedService] = useState<ServiceType>('HAIRCUT');
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!isOpen) {
      setErrorMessage('The salon is currently closed. Please check back during business hours.');
      return;
    }

    if (!customerName.trim()) {
      setErrorMessage('Please enter your name to join the queue.');
      return;
    }

    try {
      setIsSubmitting(true);
      await onJoin(customerName.trim(), customerPhone.trim(), selectedService, notes.trim());
      setCustomerName('');
      setCustomerPhone('');
      setNotes('');
      setSuccessMessage('Successfully joined the queue! Your digital ticket is displayed above.');
    } catch (err: any) {
      console.error('Failed to join queue', err);
      setErrorMessage(err.message || 'Failed to join the queue. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-3xl bg-slate-800/60 border border-slate-700/60 p-5 space-y-4 shadow-xl"
    >
      <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
        <div>
          <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider">
            Join The Line
          </h3>
          <p className="text-xs text-slate-400">
            Select a service & enter your details
          </p>
        </div>
        <span className="text-xs font-semibold text-amber-400 bg-amber-950/40 border border-amber-800/50 px-2.5 py-1 rounded-lg">
          No Waiting in Person
        </span>
      </div>

      {/* Service Options */}
      <div className="space-y-2">
        <label className="text-xs font-semibold text-slate-300 block">
          Select Service
        </label>
        <div className="grid gap-2">
          {Object.values(SALON_SERVICES).map((service) => (
            <ServiceCard
              key={service.id}
              service={service}
              isSelected={selectedService === service.id}
              onSelect={(id) => setSelectedService(id)}
            />
          ))}
        </div>
      </div>

      {/* Customer Name */}
      <div className="space-y-1.5">
        <label htmlFor="customerName" className="text-xs font-semibold text-slate-300 block">
          Your Name <span className="text-amber-400">*</span>
        </label>
        <input
          id="customerName"
          type="text"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          placeholder="e.g. John Doe"
          className="w-full bg-slate-900/90 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors"
          disabled={!isOpen || isSubmitting}
        />
      </div>

      {/* Phone Number */}
      <div className="space-y-1.5">
        <label htmlFor="customerPhone" className="text-xs font-semibold text-slate-300 block">
          Phone Number <span className="text-slate-500 font-normal">(Optional for notifications)</span>
        </label>
        <input
          id="customerPhone"
          type="tel"
          value={customerPhone}
          onChange={(e) => setCustomerPhone(e.target.value)}
          placeholder="e.g. +91 98765 43210"
          className="w-full bg-slate-900/90 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors"
          disabled={!isOpen || isSubmitting}
        />
      </div>

      {/* Optional Notes */}
      <div className="space-y-1.5">
        <label htmlFor="notes" className="text-xs font-semibold text-slate-300 block">
          Special Notes <span className="text-slate-500 font-normal">(Optional)</span>
        </label>
        <input
          id="notes"
          type="text"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. fade cut, trim beard short"
          className="w-full bg-slate-900/90 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500 transition-colors"
          disabled={!isOpen || isSubmitting}
        />
      </div>

      {/* Error & Success Feedback */}
      {errorMessage && (
        <div className="p-3 bg-rose-950/60 border border-rose-800/80 rounded-xl text-xs text-rose-300">
          {errorMessage}
        </div>
      )}
      {successMessage && (
        <div className="p-3 bg-emerald-950/60 border border-emerald-800/80 rounded-xl text-xs text-emerald-300">
          {successMessage}
        </div>
      )}

      {/* Submit Button */}
      <button
        type="submit"
        disabled={!isOpen || isSubmitting}
        className={`w-full py-3.5 px-4 rounded-2xl font-bold text-sm flex items-center justify-center gap-2 shadow-lg transition-all ${
          !isOpen
            ? 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed'
            : isSubmitting
            ? 'bg-amber-600 text-slate-950 cursor-wait opacity-80'
            : 'bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 shadow-amber-950/60 active:scale-[0.99]'
        }`}
      >
        {isSubmitting ? (
          <>
            <span className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
            <span>Securing Queue Position...</span>
          </>
        ) : !isOpen ? (
          <span>Salon Closed • Cannot Join Line</span>
        ) : (
          <span>Get Digital Queue Ticket</span>
        )}
      </button>
    </form>
  );
};
