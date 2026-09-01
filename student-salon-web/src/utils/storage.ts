const STORAGE_KEY_TICKET_IDS = 'student_salon_my_ticket_ids';
const STORAGE_KEY_SELECTED_TICKET_ID = 'student_salon_selected_ticket_id';
const STORAGE_KEY_OWNER_PIN = 'student_salon_owner_pin';

export function getSavedTicketIds(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_TICKET_IDS);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    console.error('Failed to read saved ticket IDs from storage', e);
    return [];
  }
}

export function saveTicketId(ticketId: string): void {
  try {
    const ids = getSavedTicketIds();
    if (!ids.includes(ticketId)) {
      ids.push(ticketId);
      localStorage.setItem(STORAGE_KEY_TICKET_IDS, JSON.stringify(ids));
    }
    localStorage.setItem(STORAGE_KEY_SELECTED_TICKET_ID, ticketId);
  } catch (e) {
    console.error('Failed to save ticket ID to storage', e);
  }
}

export function removeSavedTicketId(ticketId: string): void {
  try {
    const ids = getSavedTicketIds().filter((id) => id !== ticketId);
    localStorage.setItem(STORAGE_KEY_TICKET_IDS, JSON.stringify(ids));

    const selected = getSelectedTicketId();
    if (selected === ticketId) {
      if (ids.length > 0) {
        localStorage.setItem(STORAGE_KEY_SELECTED_TICKET_ID, ids[0]);
      } else {
        localStorage.removeItem(STORAGE_KEY_SELECTED_TICKET_ID);
      }
    }
  } catch (e) {
    console.error('Failed to remove ticket ID from storage', e);
  }
}

export function clearAllSavedTickets(): void {
  try {
    localStorage.removeItem(STORAGE_KEY_TICKET_IDS);
    localStorage.removeItem(STORAGE_KEY_SELECTED_TICKET_ID);
  } catch (e) {
    console.error('Failed to clear saved tickets', e);
  }
}

export function getSelectedTicketId(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY_SELECTED_TICKET_ID);
  } catch (e) {
    return null;
  }
}

export function setSelectedTicketId(ticketId: string): void {
  try {
    localStorage.setItem(STORAGE_KEY_SELECTED_TICKET_ID, ticketId);
  } catch (e) {
    console.error('Failed to set selected ticket ID', e);
  }
}

export function getSavedOwnerPin(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY_OWNER_PIN);
  } catch (e) {
    return null;
  }
}

export function saveOwnerPin(pin: string): void {
  try {
    localStorage.setItem(STORAGE_KEY_OWNER_PIN, pin);
  } catch (e) {
    console.error('Failed to save owner pin', e);
  }
}
