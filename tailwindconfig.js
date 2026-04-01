/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        './src/main/resources/static/**/*.html'
    ],
    theme: {
        extend: {
            colors: {
                primary: '#165DFF',
                secondary: '#36CBCB',
                accent: '#FF7D00',
                neutral: '#1D2129',
            },
            fontFamily: {
                sans: ['Inter', 'system-ui', 'sans-serif'],
            },
        },
    },
    plugins: [],
}