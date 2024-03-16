<script setup lang="ts">
import {ElAside, ElContainer, ElHeader} from 'element-plus'
import {computed} from "vue";
import {useRoute} from "vue-router";

let route = useRoute();

const key = computed(() => {
  return route.path
})

</script>

<template>
  <div class="app-container">
    <el-container style="min-height: 100vh;">
      <el-header class="header">Header</el-header>
      <el-header class="header header-fix">Header</el-header>
      <el-container style="display: flex; flex-direction: row; position: relative;">
        <el-aside class="aside">Aside</el-aside>
        <el-aside class="aside aside-fix">Aside</el-aside>
        <el-main class="main">
          <router-view :key="key"/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="scss">
.app-container {
  min-height: 100vh;
}

.header {
  background-color: var(--el-color-primary-light-7);
  height: 60px;
  line-height: 60px;
  width: 100%;
}

.header-fix {
  position: fixed;
  top: 0;
  z-index: 100;
}

.aside {
  width: 200px;
  background-color: var(--el-color-primary-light-8);
}

.aside-fix {
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
}

.main {
  display: flex;
  flex-direction: column;
  width: 100%;
  background-color: var(--el-color-primary-light-9);
}
</style>
