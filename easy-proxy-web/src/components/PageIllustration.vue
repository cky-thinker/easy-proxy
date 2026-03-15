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
      <!-- Background Card -->
      <g transform="translate(90, 30) rotate(15)">
         <rect x="0" y="0" width="80" height="100" rx="12" fill="#C7D2FE" opacity="0.5" />
      </g>
      
      <!-- Main User Card -->
      <g transform="translate(60, 40)" class="animate-float">
        <rect x="0" y="0" width="90" height="110" rx="12" fill="#FFFFFF" stroke="#4F46E5" stroke-width="2" />
        
        <!-- Avatar Circle -->
        <circle cx="45" cy="35" r="20" fill="#EEF2FF" />
        <circle cx="45" cy="30" r="8" fill="#4F46E5" />
        <path d="M29 52 C29 44 36 40 45 40 C54 40 61 44 61 52 V55 H29 V52 Z" fill="#4F46E5" />
        
        <!-- Text Lines -->
        <rect x="20" y="65" width="50" height="6" rx="3" fill="#E0E7FF" />
        <rect x="20" y="80" width="35" height="6" rx="3" fill="#E0E7FF" />
        
        <!-- Active Dot -->
        <circle cx="75" cy="20" r="4" fill="#10B981" class="animate-pulse" />
      </g>
      
      <!-- Admin Shield -->
      <g transform="translate(140, 90) rotate(-10)" class="animate-scale">
         <path d="M10 2 L18 6 V11 C18 16 10 20 10 20 C10 20 2 16 2 11 V6 L10 2 Z" fill="#FCD34D" stroke="#FFFFFF" stroke-width="2" />
         <path d="M6 10 L9 13 L14 8" stroke="#B45309" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      </g>
      
      <!-- Decor -->
      <circle cx="40" cy="110" r="5" fill="#818CF8" class="animate-fade-1" />
      <circle cx="160" cy="40" r="3" fill="#6366F1" class="animate-fade-2" />
    </svg>

    <!-- Rule Illustration -->
    <svg v-if="type === 'rule'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <!-- Background Card -->
      <rect x="30" y="20" width="140" height="110" rx="8" fill="#ECFDF5" stroke="#D1FAE5" stroke-width="2" />
      
      <!-- Network Flow Diagram -->
      <g transform="translate(50, 45)">
         <!-- Ingress Node (Source) -->
         <circle cx="15" cy="30" r="12" fill="#FFFFFF" stroke="#10B981" stroke-width="2" />
         <path d="M10 30 H20" stroke="#10B981" stroke-width="2" stroke-linecap="round" />
         
         <!-- Egress Node (Target) -->
         <circle cx="85" cy="30" r="12" fill="#FFFFFF" stroke="#10B981" stroke-width="2" />
         <path d="M80 30 H90 M85 25 V35" stroke="#10B981" stroke-width="2" stroke-linecap="round" />
         
         <!-- Connection Line (Animated) -->
         <path d="M27 30 H73" stroke="#34D399" stroke-width="2" stroke-dasharray="4 4" class="animate-dash" />
         
         <!-- Arrow Head -->
         <path d="M70 27 L73 30 L70 33" stroke="#34D399" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
         
         <!-- Moving Dot (Packet) -->
         <circle cx="30" cy="30" r="3" fill="#059669">
            <animate attributeName="cx" from="30" to="70" dur="1.5s" repeatCount="indefinite" />
            <animate attributeName="opacity" values="0;1;1;0" dur="1.5s" repeatCount="indefinite" />
         </circle>
      </g>
      
      <!-- Configuration Lines below -->
      <g transform="translate(60, 100)">
        <rect x="0" y="0" width="80" height="6" rx="3" fill="#D1FAE5" />
        <rect x="0" y="12" width="50" height="6" rx="3" fill="#D1FAE5" />
      </g>
      
      <!-- Settings Gear (Floating) -->
      <g transform="translate(150, 40)" class="animate-spin-slow">
        <path d="M12 0L14.5 4.3L19.4 5L15.9 8.5L16.7 13.4L12 11L7.3 13.4L8.1 8.5L4.6 5L9.5 4.3L12 0Z" fill="#34D399" opacity="0.6" />
        <circle cx="12" cy="7" r="3" fill="#ECFDF5" />
      </g>
    </svg>

    <!-- Init Illustration -->
    <svg v-if="type === 'init'" viewBox="0 0 200 150" fill="none" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <!-- Background Hexagon -->
      <path d="M100 20 L160 55 L160 125 L100 160 L40 125 L40 55 Z" fill="#EEF2FF" opacity="0.6" class="animate-pulse-slow" />

      <!-- Progress Rings -->
      <g transform="translate(100, 75)">
        <circle cx="0" cy="0" r="50" fill="none" stroke="#C7D2FE" stroke-width="2" stroke-dasharray="4 4" class="animate-spin-slow" />
        <circle cx="0" cy="0" r="65" fill="none" stroke="#E0E7FF" stroke-width="1" stroke-dasharray="2 6" class="animate-spin-reverse" />
      </g>

      <!-- Core Server / Setup Base -->
      <g transform="translate(60, 50)" class="animate-float">
        <!-- Layers -->
        <path d="M40 0 L80 20 L40 40 L0 20 Z" fill="#818CF8" opacity="0.3" />
        <path d="M40 15 L80 35 L40 55 L0 35 Z" fill="#6366F1" opacity="0.5" />
        <path d="M40 30 L80 50 L40 70 L0 50 Z" fill="#4F46E5" />

        <!-- Magic Sparkles -->
        <path d="M40 -15 L43 -25 L53 -28 L43 -31 L40 -41 L37 -31 L27 -28 L37 -25 Z" fill="#FCD34D" class="animate-scale" />
        <path d="M60 -5 L62 -12 L69 -14 L62 -16 L60 -23 L58 -16 L51 -14 L58 -12 Z" fill="#FBBF24" class="animate-scale" style="animation-delay: 0.5s;" />

        <!-- Loading/Init dots -->
        <circle cx="20" cy="55" r="3" fill="#34D399" class="animate-blink-1" />
        <circle cx="40" cy="65" r="3" fill="#34D399" class="animate-blink-2" />
        <circle cx="60" cy="55" r="3" fill="#34D399" class="animate-blink-3" />
      </g>

      <!-- Floating Setup Gears -->
      <g transform="translate(145, 35)" class="animate-spin-slow">
        <path d="M10,-2 L13,-6 L18,-3 L16,2 C17,4 17,6 16,8 L21,10 L18,15 L13,13 C10,15 8,15 6,14 L3,19 L-2,16 L0,11 C-2,9 -2,7 0,5 L-5,3 L-2,-2 L3,0 C6,-2 8,-2 10,-2 Z" fill="#A5B4FC" opacity="0.8"/>
        <circle cx="8" cy="6" r="4" fill="#EEF2FF" />
      </g>

      <g transform="translate(45, 105)" class="animate-spin-reverse">
         <path d="M6,-1 L8,-4 L12,-2 L10,1 C11,2 11,4 10,5 L14,7 L12,11 L8,9 C6,10 5,10 3,9 L1,13 L-2,11 L0,7 C-1,6 -1,4 0,3 L-3,1 L-2,-2 L2,-1 C4,-2 5,-2 6,-1 Z" fill="#818CF8" opacity="0.9"/>
         <circle cx="5" cy="4" r="2" fill="#EEF2FF" />
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  type: 'dashboard' | 'client' | 'user' | 'rule' | 'init'
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
