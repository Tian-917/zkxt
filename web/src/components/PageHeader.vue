<template>
  <div class="section-title">
    <h2>{{ title }}</h2>
    <span v-if="sub" class="sub">{{ sub }}</span>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  sub: { type: String, default: '' }
})
</script>

<style scoped>
.sub { font-size: 12px; color: var(--muted); }
</style>
