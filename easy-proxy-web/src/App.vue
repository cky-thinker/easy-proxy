<script setup lang="ts">
import {ref} from 'vue'
import {ElConfigProvider} from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const locale = ref(zhCn)
</script>

<template>
  <el-config-provider :locale="locale">
    <transition name="fade-transform" mode="out-in">
      <router-view/>
    </transition>
  </el-config-provider>
</template>

<style>

</style>
