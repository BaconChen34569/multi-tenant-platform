<template>
  <svg
    class="svg-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="name === 'dashboard'">
      <line x1="6" y1="20" x2="6" y2="12" />
      <line x1="12" y1="20" x2="12" y2="6" />
      <line x1="18" y1="20" x2="18" y2="14" />
      <line x1="4" y1="20" x2="20" y2="20" />
    </template>
    <template v-else-if="name === 'org'">
      <rect x="5" y="4" width="14" height="16" rx="1.5" />
      <line x1="9" y1="9" x2="9" y2="9" />
      <line x1="15" y1="9" x2="15" y2="9" />
      <line x1="9" y1="13" x2="9" y2="13" />
      <line x1="15" y1="13" x2="15" y2="13" />
      <line x1="9" y1="17" x2="15" y2="17" />
    </template>
    <template v-else-if="name === 'employee'">
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20c0-3.5 3-6 7-6s7 2.5 7 6" />
    </template>
    <template v-else-if="name === 'approval'">
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <path d="M8 12l3 3 5-6" />
    </template>
    <template v-else-if="name === 'users'">
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-3 2.5-5 6-5s6 2 6 5" />
      <path d="M16 6a3 3 0 0 1 0 6" />
      <path d="M18 20c0-2.6-1.6-4.6-4-5" />
    </template>
    <template v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4l3 2" />
    </template>
    <template v-else-if="name === 'check'">
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12l3 3 5-6" />
    </template>
  </svg>
</template>

<script setup>
defineProps({ name: { type: String, required: true } })
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  display: block;
}
</style>
