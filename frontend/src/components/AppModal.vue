<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="modal-box" :style="{ maxWidth: width }">
          <div class="modal-head">
            <div class="modal-title">{{ title }}</div>
            <button class="modal-close" @click="$emit('update:modelValue', false)">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({ modelValue: Boolean, title: String, width: { type: String, default: '600px' } })
defineEmits(['update:modelValue'])
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.55);
  z-index: 200; display: flex; align-items: center;
  justify-content: center; padding: 20px;
}
.modal-box {
  background: var(--bg2); border-radius: var(--r2); padding: 24px;
  width: 100%; max-height: 90vh; overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
}
.modal-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 18px;
}
.modal-title { font-size: 17px; font-weight: 700; color: var(--tx); }
.modal-close {
  width: 28px; height: 28px; border-radius: 6px; background: var(--bg3);
  border: none; cursor: pointer; display: flex; align-items: center;
  justify-content: center; color: var(--tx2);
}
.modal-close:hover { background: var(--erl); color: var(--er); }
.modal-footer {
  display: flex; gap: 10px; justify-content: flex-end;
  margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--bd);
}
.modal-enter-active, .modal-leave-active { transition: opacity .22s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .modal-box, .modal-leave-active .modal-box { transition: transform .22s ease; }
.modal-enter-from .modal-box { transform: translateY(-16px); }
.modal-leave-to .modal-box { transform: translateY(-16px); }
</style>
