/** @type {import('tailwindcss').Config} */
import path from 'path';

export default {
  content: [
    path.join(__dirname, 'index.html'),
    path.join(__dirname, 'src/**/*.{js,ts,jsx,tsx}')
  ],
  theme: {
    extend: {
      colors: {
        salon: {
          dark: '#0F172A',
          card: '#1E293B',
          cardBorder: '#334155',
          subCard: '#090D16',
          gold: '#F59E0B',
          goldLight: '#FCD34D',
          goldDark: '#D97706',
          green: '#10B981',
          blue: '#38BDF8',
          red: '#EF4444'
        }
      }
    },
  },
  plugins: [],
}

