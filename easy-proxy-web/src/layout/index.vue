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
      <el-header class="header"></el-header>
      <el-header class="header header-fix">
        <div class="h-left">
          <img src="@/assets/images/logo.png" class="logo" alt=""/>
          <span class="logo-title">Easy Proxy</span>
        </div>
        <div class="h-right">
          <div class="avatar">
            <img src="@/assets/images/avatar.png" alt=""/>
          </div>
        </div>
      </el-header>
      <el-container style="display: flex; flex-direction: row; position: relative;">
        <el-aside class="aside"></el-aside>
        <el-aside class="aside aside-fix">Aside</el-aside>
        <el-main class="main">
          <router-view :key="key"/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style lang="scss">
:root {
  --color-white: #FFFFFF;
}

@font-face {
  font-family: 'Alatsi-Regular';
  src: url("@/assets/fonts/Alatsi-Regular.ttf")
}

.app-container {
  min-height: 100vh;
}

.header {
  background-color: var(--color-white);
  height: 60px;
  line-height: 60px;
  width: 100%;
  box-shadow: rgba(149, 157, 165, 0.2) 0 8px 24px;
  display: flex;
  justify-items: center;
  justify-content: space-between;
}

.header-fix {
  position: fixed;
  top: 0;
  z-index: 100;
}

.h-left {
  display: flex;
  align-items: center;
}

.h-right {
  display: flex;
  align-items: center;

  .avatar {
    img {
      max-height: 40px;
    }

    height: 40px;
    width: 40px;
    border-radius: 20px;
    overflow: hidden;
  }
}

.logo {
  max-height: 40px;
}

.logo-title {
  font-family: 'Alatsi-Regular', serif;
  font-size: 20px;
  font-style: italic;
  color: #003087;
  margin-left: 6px;
}

.aside {
  width: 200px;
  background-color: var(--color-white);
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
