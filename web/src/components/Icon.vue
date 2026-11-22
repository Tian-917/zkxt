<template>
  <svg class="ico" :width="size" :height="size" viewBox="0 0 24 24" fill="none"
       stroke="currentColor" :stroke-width="stroke" stroke-linecap="round" stroke-linejoin="round"
       aria-hidden="true" v-html="path"></svg>
</template>

<script setup>
import { computed } from 'vue'
import { ICONS } from '../icons'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  stroke: { type: [Number, String], default: 2 }
})

const path = computed(() => ICONS[props.name] || '')
</script>

<style scoped>
.ico { display: inline-block; vertical-align: -0.15em; flex: none; }
</style>
