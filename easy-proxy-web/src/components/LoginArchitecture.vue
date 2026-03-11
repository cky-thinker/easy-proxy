<template>
  <div class="w-full h-full flex items-center justify-center p-8">
    <svg
      viewBox="0 0 800 600"
      class="w-full h-full max-w-4xl drop-shadow-xl"
      xmlns="http://www.w3.org/2000/svg"
    >
      <!-- Definitions for gradients and filters -->
      <defs>
        <linearGradient id="cloudGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color: #ffffff; stop-opacity: 0.9" />
          <stop offset="100%" style="stop-color: #e0f2fe; stop-opacity: 0.8" />
        </linearGradient>
        <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      <!-- Connection Lines (Behind elements) -->
      <!-- Web to Internet -->
      <path
        d="M200 420 Q 250 300 350 180"
        fill="none"
        stroke="rgba(255, 255, 255, 0.4)"
        stroke-width="4"
        stroke-dasharray="10 10"
      />
      <path
        d="M200 420 Q 250 300 350 180"
        fill="none"
        stroke="#60a5fa"
        stroke-width="4"
        stroke-dasharray="20 40"
        class="animate-flow"
      />

      <!-- Internet to Computer -->
      <path
        d="M450 180 Q 550 300 600 420"
        fill="none"
        stroke="rgba(255, 255, 255, 0.4)"
        stroke-width="4"
        stroke-dasharray="10 10"
      />
      <path
        d="M450 180 Q 550 300 600 420"
        fill="none"
        stroke="#60a5fa"
        stroke-width="4"
        stroke-dasharray="20 40"
        class="animate-flow-reverse"
      />

      <!-- 1. Internet (Cloud) - Top Center -->
      <g transform="translate(300, 80)">
        <path
          d="M140,40 C140,17.9 122.1,0 100,0 C83.4,0 69.2,10.1 63.3,24.4 C58.9,21.6 53.7,20 48,20 C21.5,20 0,41.5 0,68 C0,94.5 21.5,116 48,116 L140,116 C166.5,116 188,94.5 188,68 C188,41.5 166.5,20 140,20 L140,40 Z"
          fill="url(#cloudGradient)"
          filter="url(#glow)"
        />
        <!-- Logo Image -->
        <image
          :href="logoUrl"
          x="54"
          y="28"
          width="80"
          height="60"
          preserveAspectRatio="xMidYMid meet"
        />
      </g>

      <!-- 2. Web Page - Bottom Left -->
      <g transform="translate(100, 400)">
        <!-- Browser Window -->
        <rect x="0" y="0" width="200" height="150" rx="8" ry="8" fill="rgba(255, 255, 255, 0.9)" />
        <!-- Header Bar -->
        <rect x="0" y="0" width="200" height="30" rx="8" ry="8" fill="#e2e8f0" />
        <rect x="0" y="20" width="200" height="10" fill="#e2e8f0" /> <!-- Square off bottom corners of header -->
        <!-- Traffic Lights -->
        <circle cx="15" cy="15" r="4" fill="#ef4444" />
        <circle cx="30" cy="15" r="4" fill="#f59e0b" />
        <circle cx="45" cy="15" r="4" fill="#22c55e" />
        <!-- Content placeholders -->
        <rect x="20" y="55" width="160" height="10" rx="2" fill="#cbd5e1" />
        <rect x="20" y="80" width="100" height="10" rx="2" fill="#cbd5e1" />
        <rect x="20" y="105" width="140" height="45" rx="2" fill="#e2e8f0" />
        
      </g>

      <!-- 3. Computer - Bottom Right -->
      <g transform="translate(500, 400)">
        <!-- Monitor -->
        <rect x="0" y="0" width="200" height="140" rx="8" ry="8" fill="#1e293b" stroke="#334155" stroke-width="4" />
        <!-- Screen -->
        <rect x="10" y="10" width="180" height="120" fill="#0f172a" />
        <!-- Code lines on screen -->
        <rect x="20" y="30" width="80" height="4" fill="#22c55e" opacity="0.8" />
        <rect x="20" y="45" width="120" height="4" fill="#38bdf8" opacity="0.8" />
        <rect x="20" y="60" width="100" height="4" fill="#a78bfa" opacity="0.8" />
        <rect x="20" y="75" width="60" height="4" fill="#fbbf24" opacity="0.8" />
        <!-- Stand -->
        <path d="M70,140 L130,140 L140,160 L60,160 Z" fill="#334155" />
        <rect x="40" y="160" width="120" height="10" rx="2" fill="#1e293b" />
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import logoUrl from '../assets/logo.png'
</script>

<style scoped>
.animate-flow {
  animation: flow-reverse 0.5s linear infinite;
}

.animate-flow-reverse {
  animation: flow-reverse 0.5s linear infinite;
}

@keyframes flow {
  from {
    stroke-dashoffset: 60;
  }
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes flow-reverse {
  from {
    stroke-dashoffset: 0;
  }
  to {
    stroke-dashoffset: 60;
  }
}
</style>
