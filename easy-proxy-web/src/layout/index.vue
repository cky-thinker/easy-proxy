<script setup lang="ts">
import IconList from '../components/icons/IconList.vue'
import IconUser from '../components/icons/IconUser.vue'
import Computer from '../components/icons/Computer3.vue'
</script>

<template>
    <div class="app-container">
        <div class="app-container-l">
            <div>
                <img class="app-icon" src="@/assets/logo.png" alt="easy proxy">
            </div>
            <div class="app-menu">
                <div class="app-menu-item app-menu-item-active">
                    <div class="app-menu-item-icon">
                        <IconList/>
                    </div>
                    <div class="app-menu-item-text">客户端管理</div>
                </div>
                <div class="app-menu-item">
                    <div class="app-menu-item-icon">
                        <IconUser/>
                    </div>
                    <div class="app-menu-item-text">账号设置</div>
                </div>
            </div>
        </div>
        <div class="app-container-r">
            <div class="app-header">
                <div class="app-header-l">
                    客户端管理
                </div>
                <div class="app-header-r">
                    <div class="user-avatar">
                    </div>
                </div>
            </div>
            <div>

            </div>
            <div class="app-content" >
                <div class="item" v-for='index in 10' :key='index'>
                    <div class="item-left">
                        <computer class="item-logo"/>
                    </div>
                    <div class="item-right">
                        <div class="item-title" >OSS服务器</div>
                        <div>配置</div>
                    </div>
                    <div class="bg1"></div>
                    <div class="bg2"></div>
                    <div class="status-label">
                        <span class="" style="background: rgb(229, 0, 18);"></span>
                        离线</div>
                </div>
            </div>
        </div>
    </div>
</template>

<style>
:root {
    /** 阴影 **/
    /** 组件 hover 状态使用，例如表格和树拖动 **/
    --shadow1: 0 1px 10px rgba(0, 0, 0, 5%), 0 4px 5px rgba(0, 0, 0, 8%), 0 2px 4px -1px rgba(0, 0, 0, 12%);
    /** 下拉组件使用，例如下拉菜单 / 气泡确认框 / 选择器 等 **/
    --shadow2: 0 3px 14px 2px rgba(0, 0, 0, 5%), 0 8px 10px 1px rgba(0, 0, 0, 6%), 0 5px 5px -3px rgba(0, 0, 0, 10%);
    /** 警示或弹窗组件使用，例如全局提示 / 消息通知等 */
    --shadow3: 0 6px 30px 5px rgba(0, 0, 0, 5%), 0 16px 24px 2px rgba(0, 0, 0, 4%), 0 8px 10px -5px rgba(0, 0, 0, 8%);

    /** 间距 **/
    --sp4: 4px;
    --sp8: 8px;
    --sp12: 12px;
    --sp16: 16px;
    --sp24: 24px;
    --sp32: 32px;
    --sp40: 40px;
    --sp48: 48px;
    --sp64: 64px;
    --sp96: 96px;
    --sp160: 160px;

    /** 字体 **/
    --font-size-tile-s: 14px;
    --font-size-tile-m: 16px;
    --font-size-tile-l: 20px;
    --font-size-header-s: 24px;
    --font-size-header-m: 28px;
    --font-size-header-l: 36px;

    /** 颜色 **/
    --color-text-primary: rgb(0, 0, 0, 0.9);
    --color-text-secondary: rgb(0, 0, 0, 0.6);
    --color-text-placeholder: rgb(0, 0, 0, 0.4);
    --color-text-disabled: rgb(0, 0, 0, 0.26);
}

.app-container {
    display: flex;
    justify-content: flex-start;
    height: 100%
}

.app-container-l {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 300px;
    height: 100%;
    background: rgba(255, 255, 255, 0.3);
}

.app-container-r {
    width: 100%;
    //height: 100%;
    background: rgba(255, 255, 255, 0.5);
    padding: var(--sp16) var(--sp32);
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
}

.app-header {
    margin-bottom: var(--sp24);
    display: flex;
    justify-content: space-between;
}

.app-header-l {
    font-size: var(--font-size-header-s);
    font-weight: 400;
    color: var(--color-text-primary);
    display: flex;
    justify-content: center;
    align-items: center
}

.app-header-r {

}

.user-avatar {
    height: var(--sp40);
    width: var(--sp40);
    border-radius: var(--sp12);
    box-shadow: var(--shadow1);
    background-color: var(--color-text-placeholder);
}

.app-icon {
    width: 200px;
    margin-top: var(--sp12);
}

.app-menu {
    margin: 16px;
    font-size: 18px;
    color: #665E75;
}

.app-menu-item {
    display: flex;
    justify-content: flex-start;
    width: 150px;
    padding: var(--sp12) var(--sp24);
    margin-top: var(--sp12);
}

.app-menu-item:hover {
    cursor: pointer;
}

.app-menu-item-active {
    background-color: rgba(252, 252, 252, 0.4);
    border-radius: var(--sp12);
    box-shadow: var(--shadow1);
}

.app-menu-item-icon {
    width: var(--sp32);
    height: var(--sp32);
    margin-right: var(--sp24);
}

.app-menu-item-text {
    display: flex;
    justify-content: flex-start;
}

.item-row {
    display: flex;
    justify-content: space-around;
}

.item {
    box-shadow: rgba(149, 157, 165, 0.2) 0 8px 24px;
    width: 300px;
    height: 150px;
    border-radius: 12px;
    background-color: #fcfcfc;
    color: var(--color-text-primary);
    display: flex;
    justify-content: left;
    justify-items: left;
    position: relative;
    overflow: hidden;
}

.bg1 {
    position: absolute;
    right: -39%;
    top: -13%;
    width: 300px;
    height: 300px;
    border-radius: 50%;
    background: linear-gradient(188.4deg, rgba(229, 0, 18, .03) 22.94%, rgba(229, 0, 18, 0) 94.62%);
}

.bg2 {
    position: absolute;
    right: -52%;
    top: 1%;
    width: 300px;
    height: 300px;
    border-radius: 50%;
    background: linear-gradient(188.4deg, rgba(229, 0, 18, 0.03) 30%, rgba(229, 0, 18, 0) 80%);
}

.status-label {
    position: absolute;
    width: 70px;
    height: 30px;
    right: -5%;
    top: 18%;
    border-radius: 15px;
    background-color: rgba(229, 0, 18, 0.1);
}

.app-content {
    display: grid;
    grid-template-columns: repeat(auto-fill, 300px);
    grid-gap: 32px;
}

.item-left {
    width: 180px;
    display: flex;
    justify-content: center;
    align-items: center;
}

.item-title {
    padding: var(--sp16) 0;
    font-size: var(--sp16);
    font-weight: 600;
}

.item-right {
    width: 100%;
}

.item-logo {
    width: var(--sp64);
    height: var(--sp64);
}

</style>
