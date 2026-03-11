<template>
  <div class="page-illustration select-none pointer-events-none" :class="customClass">
    <!-- Dashboard Illustration -->
    <svg v-if="type === 'dashboard'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <defs>
        <linearGradient id="dash-bg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#E0E7FF" stop-opacity="0.5"/>
          <stop offset="100%" stop-color="#E0E7FF" stop-opacity="0.1"/>
        </linearGradient>
      </defs>
      <!-- Background Shape -->
      <rect x="10" y="20" width="180" height="110" rx="8" fill="url(#dash-bg)" />
      
      <!-- Chart Bars -->
      <g transform="translate(30, 100) scale(1, -1)">
        <rect x="0" y="0" width="20" height="40" rx="2" fill="#818CF8" class="animate-bar-1" />
        <rect x="30" y="0" width="20" height="70" rx="2" fill="#6366F1" class="animate-bar-2" />
        <rect x="60" y="0" width="20" height="50" rx="2" fill="#4F46E5" class="animate-bar-3" />
        <rect x="90" y="0" width="20" height="85" rx="2" fill="#4338CA" class="animate-bar-4" />
        <rect x="120" y="0" width="20" height="60" rx="2" fill="#3730A3" class="animate-bar-5" />
      </g>
      
      <!-- Floating Elements -->
      <circle cx="160" cy="40" r="15" fill="#C7D2FE" class="animate-float" />
      <path d="M140 30 L150 20 L160 30" stroke="#4F46E5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="animate-pulse-slow" />
    </svg>

    <!-- Client Illustration -->
    <svg v-if="type === 'client'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <!-- Server Rack -->
      <rect x="40" y="20" width="120" height="110" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2" />
      
      <!-- Server Units -->
      <g class="server-unit">
        <rect x="50" y="30" width="100" height="20" rx="2" fill="#F3F4F6" stroke="#D1D5DB" />
        <circle cx="60" cy="40" r="3" fill="#10B981" class="animate-blink-1" />
        <circle cx="70" cy="40" r="3" fill="#E5E7EB" />
        <rect x="80" y="38" width="60" height="4" rx="2" fill="#D1D5DB" />
      </g>
      
      <g class="server-unit" transform="translate(0, 30)">
        <rect x="50" y="30" width="100" height="20" rx="2" fill="#F3F4F6" stroke="#D1D5DB" />
        <circle cx="60" cy="40" r="3" fill="#10B981" class="animate-blink-2" />
        <circle cx="70" cy="40" r="3" fill="#E5E7EB" />
        <rect x="80" y="38" width="60" height="4" rx="2" fill="#D1D5DB" />
      </g>
      
      <g class="server-unit" transform="translate(0, 60)">
        <rect x="50" y="30" width="100" height="20" rx="2" fill="#F3F4F6" stroke="#D1D5DB" />
        <circle cx="60" cy="40" r="3" fill="#EF4444" class="animate-blink-3" /> <!-- Error state -->
        <circle cx="70" cy="40" r="3" fill="#E5E7EB" />
        <rect x="80" y="38" width="60" height="4" rx="2" fill="#D1D5DB" />
      </g>
      
      <!-- Connection Lines -->
      <path d="M160 75 H180 M180 75 V40 M180 75 V110" stroke="#60A5FA" stroke-width="2" stroke-dasharray="4 4" class="animate-dash" />
      <circle cx="180" cy="75" r="4" fill="#3B82F6" class="animate-pulse" />
    </svg>

    <!-- User Illustration -->
    <svg v-if="type === 'user'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <!-- Background Circle -->
      <circle cx="100" cy="75" r="50" fill="#E0E7FF" class="animate-scale" />
      
      <!-- User Avatar -->
      <g transform="translate(100, 75)" class="animate-float">
        <circle cx="0" cy="-15" r="20" fill="#4F46E5" />
        <path d="M-30 35 C-30 15 -15 10 0 10 C15 10 30 15 30 35 V40 H-30 V35 Z" fill="#4F46E5" />
      </g>
      
      <!-- Badge -->
      <g transform="translate(130, 45)">
        <circle cx="0" cy="0" r="12" fill="#FCD34D" stroke="#FFFFFF" stroke-width="2" class="animate-bounce-small" />
        <path d="M-4 0 L-1 3 L4 -3" stroke="#B45309" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" transform="translate(0, 0)" />
      </g>
      
      <!-- Decorative particles -->
      <circle cx="50" cy="50" r="4" fill="#818CF8" class="animate-fade-1" />
      <circle cx="150" cy="100" r="3" fill="#818CF8" class="animate-fade-2" />
      <circle cx="60" cy="110" r="5" fill="#818CF8" class="animate-fade-3" />
    </svg>

    <!-- Rule Illustration -->
    <svg v-if="type === 'rule'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <!-- Gears -->
      <g transform="translate(60, 60)" class="animate-spin-slow">
        <circle cx="0" cy="0" r="25" fill="none" stroke="#6B7280" stroke-width="8" stroke-dasharray="8 8" />
        <circle cx="0" cy="0" r="10" fill="#9CA3AF" />
      </g>
      
      <g transform="translate(110, 90)" class="animate-spin-reverse">
        <circle cx="0" cy="0" r="20" fill="none" stroke="#4B5563" stroke-width="6" stroke-dasharray="6 6" />
        <circle cx="0" cy="0" r="8" fill="#6B7280" />
      </g>
      
      <!-- Flow Lines -->
      <path d="M20 75 Q 60 20 100 75 T 180 75" stroke="#10B981" stroke-width="3" fill="none" stroke-dasharray="10 5" class="animate-flow" />
      
      <!-- Particles on path -->
      <circle r="4" fill="#059669">
        <animateMotion dur="3s" repeatCount="indefinite" path="M20 75 Q 60 20 100 75 T 180 75" />
      </circle>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  type: 'dashboard' | 'client' | 'user' | 'rule'
  class?: string
}>()

const customClass = computed(() => props.class || '')
</script>

<style scoped>
/* Keyframes */
@keyframes bar-grow {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(1.2); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes dash {
  to { stroke-dashoffset: -20; }
}

@keyframes flow {
  to { stroke-dashoffset: -30; }
}

@keyframes scale-pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.3; }
}

@keyframes bounce-small {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@keyframes fade {
  0%, 100% { opacity: 0; transform: scale(0.5); }
  50% { opacity: 1; transform: scale(1); }
}

/* Animations Classes */
.animate-bar-1 { animation: bar-grow 2s infinite ease-in-out; transform-origin: bottom; }
.animate-bar-2 { animation: bar-grow 2.2s infinite ease-in-out 0.2s; transform-origin: bottom; }
.animate-bar-3 { animation: bar-grow 1.8s infinite ease-in-out 0.4s; transform-origin: bottom; }
.animate-bar-4 { animation: bar-grow 2.5s infinite ease-in-out 0.1s; transform-origin: bottom; }
.animate-bar-5 { animation: bar-grow 2.1s infinite ease-in-out 0.3s; transform-origin: bottom; }

.animate-float { animation: float 3s ease-in-out infinite; }
.animate-pulse-slow { animation: blink 3s ease-in-out infinite; }

.animate-blink-1 { animation: blink 1s step-end infinite; }
.animate-blink-2 { animation: blink 1.5s step-end infinite 0.5s; }
.animate-blink-3 { animation: blink 0.8s step-end infinite; }

.animate-dash { animation: dash 1s linear infinite; }
.animate-pulse { animation: blink 1s ease-in-out infinite; }

.animate-scale { animation: scale-pulse 3s ease-in-out infinite; transform-origin: center; }
.animate-bounce-small { animation: bounce-small 2s ease-in-out infinite; }

.animate-fade-1 { animation: fade 2s ease-in-out infinite 0s; }
.animate-fade-2 { animation: fade 2.5s ease-in-out infinite 0.8s; }
.animate-fade-3 { animation: fade 2.2s ease-in-out infinite 1.5s; }

.animate-spin-slow { animation: spin 8s linear infinite; transform-origin: center; }
.animate-spin-reverse { animation: spin 6s linear infinite reverse; transform-origin: center; }
.animate-flow { animation: flow 1s linear infinite; }
</style>
