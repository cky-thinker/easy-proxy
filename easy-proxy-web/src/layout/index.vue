<script setup lang="ts">
import {ElContainer, ElFooter, ElHeader} from 'element-plus'
import {computed} from "vue";
import {useRoute} from "vue-router";

let route = useRoute();

const key = computed(() => {
  return route.path
})

</script>

<template>
  <div class="app-container">
    <el-container>
      <el-header class="header">Header</el-header>
      <el-main class="main">
        <router-view :key="key"/>
      </el-main>
      <el-footer class="footer">Footer</el-footer>
    </el-container>
  </div>
</template>

<style lang="scss">
.header {
  background-color: var(--el-color-primary-light-7);
}

.aside {
  background-color: var(--el-color-primary-light-8);
}

.main {
  background-color: var(--el-color-primary-light-9);
  height: 150px;
}

.footer {
  background-color: var(--el-color-primary-light-7);
}

</style>
