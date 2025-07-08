<script setup lang="ts">
import {ElAside, ElContainer, ElHeader} from 'element-plus'
import {computed, ref} from "vue";
import {useRoute} from "vue-router";

let route = useRoute();

const key = computed(() => {
  return route.path
})

const menuList = ref([
  {title: '工作台', iconName: 'DataLine', selected: true},
  {title: '客户端管理', iconName: 'Monitor', selected: false},
  {title: '账号设置', iconName: 'Setting', selected: false}])

function onMenuClickHandler(menu: any) {
  menuList.value.forEach(m => {
    m.selected = m.title === menu.title
  })
}

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
          <div class="username">管理员</div>
          <div class="avatar">
            <img src="@/assets/images/avatar.png" alt=""/>
          </div>
        </div>
      </el-header>
      <el-container style="display: flex; flex-direction: row; position: relative;">
        <el-aside class="aside"></el-aside>
        <el-aside class="aside aside-fix">
          <div class="menu-item-wrapper">
            <div @click="onMenuClickHandler(menu)" v-for="menu of menuList" :class="'menu-item' + (menu.selected ? ' selected' : '')">
              <div class="menu-title-wrapper">
                <div class="menu-icon">
                  <component :is="menu.iconName"></component>
                </div>
                <div class="menu-title">{{ menu.title }}</div>
              </div>
              <div class="menu-fold">
                <!--                <ArrowDownBold/>-->
              </div>
            </div>
          </div>
        </el-aside>
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

@font-face {
  font-family: 'AliRegular';
  src: url("@/assets/fonts/AlibabaPuHuiTi-2-55-Regular.ttf")
}

.app-container {
  min-height: 100vh;
}

.header {
  background-color: var(--color-white);
  height: 60px;
  line-height: 60px;
  width: 100%;
  box-shadow: rgba(149, 157, 165, 0.2) 0 3px 4px;
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

  .username {
    margin-right: 12px;
    font-size: 14px;
    line-height: 14px;
    color: #000000d9;
  }

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
  margin-left: 8px;
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

.menu-item-wrapper {
  margin-top: 12px;
}

.menu-item:hover {
  color: #165DFF;
}

.selected {
  background-color: #f0f6ff;
  color: #165DFF;
  border-right: 3px solid #165DFF ;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  padding: 12px 16px;
  cursor: pointer;
  margin-top: 8px;

  .menu-title-wrapper {
    display: flex;
    justify-content: start;
    align-items: center;

    .menu-icon {
      height: 18px;
      width: 18px;
      margin-right: 16px;

      svg {
        height: 18px;
        width: 18px;
      }
    }

    .menu-title {
        font-family: 'AliRegular', serif !important;
      font-weight: normal;
      height: 20px;
      line-height: 20px;
    }
  }

  .menu-fold {
    height: 16px;
    width: 16px;

    svg {
      height: 12px;
      width: 12px;
    }
  }
}

.main {
  display: flex;
  flex-direction: column;
  width: 100%;
  background-color: var(--el-color-primary-light-9);
}
</style>
