/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Outfit', 'sans-serif'],
      },
      colors: {
        darkBg: "#0B0F19",
        darkSurface: "#161E2E",
        darkBorder: "#243248",
        brandPrimary: "#4F46E5",
        brandSecondary: "#8B5CF6",
      }
    },
  },
  plugins: [],
}
