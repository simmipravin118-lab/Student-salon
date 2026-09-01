import {
  QueueTicket,
  CustomerTicketInfo,
  DailySummary,
  SALON_SERVICES,
  ServiceType
} from '../types/salon';

export function getTodayDateString(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatMinutesSeconds(minutes: number, seconds: number): string {
  if (minutes > 0 && seconds > 0) {
    return `${minutes} min ${seconds} sec`;
  } else if (minutes > 0) {
    return `${minutes} min`;
  } else if (seconds > 0) {
    return `${seconds} sec`;
  }
  return '0 min';
}

export function formatTurnTime(timestamp: number): string {
  const date = new Date(timestamp);
  let hours = date.getHours();
  const minutes = date.getMinutes();
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12;
  hours = hours ? hours : 12; // the hour '0' should be '12'
  const minutesStr = minutes < 10 ? '0' + minutes : minutes;
  const hoursStr = hours < 10 ? '0' + hours : hours;
  return `${hoursStr}:${minutesStr} ${ampm}`;
}

export function getServiceDurationMinutes(serviceName: ServiceType): number {
  return SALON_SERVICES[serviceName]?.durationMinutes ?? 20;
}

export function calculateTicketInfo(
  ticket: QueueTicket,
  allTodayTickets: QueueTicket[],
  now: number
): CustomerTicketInfo {
  const serviceDurationMinutes = getServiceDurationMinutes(ticket.serviceName);

  if (ticket.statusName === 'SERVING') {
    const elapsedMillis = ticket.startedAt ? Math.max(0, now - ticket.startedAt) : 0;
    const totalDurationMillis = serviceDurationMinutes * 60_000;
    const remainingMillis = Math.max(0, totalDurationMillis - elapsedMillis);
    const remainingTotalSeconds = Math.floor(remainingMillis / 1000);
    const remainingMinutes = Math.floor(remainingTotalSeconds / 60);
    const remainingSeconds = remainingTotalSeconds % 60;
    const formatted = formatMinutesSeconds(remainingMinutes, remainingSeconds);

    return {
      ticket,
      customersAhead: 0,
      estimatedWaitingMinutes: 0,
      estimatedTurnTimeFormatted: 'NOW (In Chair)',
      remainingServingMinutes: remainingMinutes,
      estimatedWaitingSeconds: 0,
      estimatedWaitingMillis: 0,
      estimatedWaitingFormatted: '0 min',
      remainingServingSeconds: remainingSeconds,
      remainingServingMillis: remainingMillis,
      remainingServingFormatted: formatted
    };
  }

  if (
    ticket.statusName === 'COMPLETED' ||
    ticket.statusName === 'CANCELLED' ||
    ticket.statusName === 'SKIPPED'
  ) {
    return {
      ticket,
      customersAhead: 0,
      estimatedWaitingMinutes: 0,
      estimatedTurnTimeFormatted: ticket.statusName,
      remainingServingMinutes: 0,
      estimatedWaitingSeconds: 0,
      estimatedWaitingMillis: 0,
      estimatedWaitingFormatted: '0 min',
      remainingServingSeconds: 0,
      remainingServingMillis: 0,
      remainingServingFormatted: '0 min'
    };
  }

  // Waiting customer calculation
  const serving = allTodayTickets.find((t) => t.statusName === 'SERVING');
  const waitingQueue = allTodayTickets
    .filter((t) => t.statusName === 'WAITING')
    .sort((a, b) => {
      if (a.isRejoinedPriority !== b.isRejoinedPriority) {
        return a.isRejoinedPriority ? -1 : 1;
      }
      return a.queueNumber - b.queueNumber;
    });

  let servingRemainingMillis = 0;
  if (serving) {
    const servingDuration = getServiceDurationMinutes(serving.serviceName) * 60_000;
    if (serving.startedAt) {
      const elapsed = Math.max(0, now - serving.startedAt);
      servingRemainingMillis = Math.max(0, servingDuration - elapsed);
    } else {
      servingRemainingMillis = servingDuration;
    }
  }

  const servingCustomerCount = serving ? 1 : 0;
  const myIndexInWaiting = waitingQueue.findIndex((t) => t.id === ticket.id);

  let customersAhead = 0;
  let totalEstimatedWaitingMillis = 0;

  if (myIndexInWaiting === -1) {
    customersAhead = servingCustomerCount;
    totalEstimatedWaitingMillis = servingRemainingMillis;
  } else {
    const waitingAhead = waitingQueue.slice(0, myIndexInWaiting);
    customersAhead = servingCustomerCount + waitingAhead.length;
    const waitingAheadMillis = waitingAhead.reduce(
      (sum, t) => sum + getServiceDurationMinutes(t.serviceName) * 60_000,
      0
    );
    totalEstimatedWaitingMillis = servingRemainingMillis + waitingAheadMillis;
  }

  const totalWaitSeconds = Math.floor(totalEstimatedWaitingMillis / 1000);
  const waitMinutes = Math.floor(totalWaitSeconds / 60);
  const waitSeconds = totalWaitSeconds % 60;
  const formattedWait = formatMinutesSeconds(waitMinutes, waitSeconds);

  const estimatedTurnTimestamp = now + totalEstimatedWaitingMillis;
  const estimatedTurnTimeFormatted =
    customersAhead === 0 && totalEstimatedWaitingMillis === 0
      ? formatTurnTime(now)
      : formatTurnTime(estimatedTurnTimestamp);

  const servingSecTotal = Math.floor(servingRemainingMillis / 1000);
  const servingMinutes = Math.floor(servingSecTotal / 60);
  const servingSeconds = servingSecTotal % 60;

  return {
    ticket,
    customersAhead,
    estimatedWaitingMinutes: waitMinutes,
    estimatedTurnTimeFormatted,
    remainingServingMinutes: servingMinutes,
    estimatedWaitingSeconds: waitSeconds,
    estimatedWaitingMillis: totalEstimatedWaitingMillis,
    estimatedWaitingFormatted: formattedWait,
    remainingServingSeconds: servingSeconds,
    remainingServingMillis: servingRemainingMillis,
    remainingServingFormatted: formatMinutesSeconds(servingMinutes, servingSeconds)
  };
}

export function calculateDailySummary(tickets: QueueTicket[]): DailySummary {
  return {
    totalCustomers: tickets.length,
    waitingCount: tickets.filter((t) => t.statusName === 'WAITING').length,
    servingCount: tickets.filter((t) => t.statusName === 'SERVING').length,
    completedCount: tickets.filter((t) => t.statusName === 'COMPLETED').length,
    skippedCount: tickets.filter((t) => t.statusName === 'SKIPPED').length,
    cancelledCount: tickets.filter((t) => t.statusName === 'CANCELLED').length
  };
}
