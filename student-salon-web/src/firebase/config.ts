import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth, signInAnonymously, onAuthStateChanged, User } from 'firebase/auth';
import { getFirestore, enableIndexedDbPersistence } from 'firebase/firestore';

// Detected from project configuration (google-services.json)
const firebaseConfig = {
  apiKey: "AIzaSyCeHF-PmjxQHkYSuTsNfHC-Fg53lq5WGqE",
  authDomain: "student-salon-2.firebaseapp.com",
  projectId: "student-salon-2",
  storageBucket: "student-salon-2.firebasestorage.app",
  messagingSenderId: "1046653384398",
  appId: "1:1046653384398:web:student-salon-2"
};

export const SALON_ID = 'student_salon_telo';

// Initialize Firebase App
export const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);

// Initialize Auth & Firestore
export const auth = getAuth(app);
export const db = getFirestore(app);

// Enable offline persistence for Firestore if available in browser
if (typeof window !== 'undefined') {
  enableIndexedDbPersistence(db).catch((err: { code?: string }) => {
    if (err.code === 'failed-precondition') {
      console.warn('Firestore persistence failed: Multiple tabs open');
    } else if (err.code === 'unimplemented') {
      console.warn('Firestore persistence is not supported in this browser');
    }
  });
}


/**
 * Ensures anonymous authentication for customers without requiring login forms.
 * Returns the current authenticated UID.
 */
export async function ensureAnonymousAuth(): Promise<string> {
  if (auth.currentUser) {
    return auth.currentUser.uid;
  }

  try {
    const userCredential = await signInAnonymously(auth);
    return userCredential.user.uid;
  } catch (error) {
    console.error('Anonymous authentication error:', error);
    throw error;
  }
}

/**
 * Subscribes to Firebase Auth state changes.
 */
export function onAuthUserChanged(callback: (user: User | null) => void) {
  return onAuthStateChanged(auth, callback);
}
