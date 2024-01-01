<script setup lang="ts">
import IconList from '../components/icons/IconList.vue'
import IconUser from '../components/icons/IconUser.vue'</script>

<template>
    <div class="app-container">
        <div class="app-container-l">
            <div class="app-icon">
                Easy Proxy
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
                    <div class="app-menu-item-text">账号管理</div>
                </div>
            </div>
        </div>
        <div class="app-container-r">
            <div class="app-header"
                 style="margin-bottom: 24px; height: 60px; display: flex; justify-content: space-between; ">
                <div class="app-header-l" style="font-size: 18px; font-weight: bold; color: #363B64">
                    客户端管理
                </div>
                <div class="app-header-r">
                    <div
                        style="height: 60px; width: 60px; border-radius: 12px; box-shadow: 0 20px 50px rgba(204, 204, 204, 0.05); background-color: #fcfcfc">
                    </div>
                </div>
            </div>
            <div class="app-content" style="">
                <div class="item-row">
                    <div class="item">
                    </div>
                    <div class="item">
                    </div>
                    <div class="item">
                    </div>
                </div>
                <div class="item-row">
                    <div class="item">
                    </div>
                    <div class="item">
                    </div>
                    <div class="item">
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style>
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
    height: 100%;
    background: rgba(255, 255, 255, 0.5);
    padding: 26px 32px;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
}

.app-icon {
    font-size: 32px;
    font-weight: bold;
    text-align: center;
    width: 100%;
    color: #363B64;
    margin-top: 12px;
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
    padding: 12px 24px;
    margin-top: 12px;
}

.app-menu-item:hover {
    cursor: pointer;
}

.app-menu-item-active {
    background-color: rgba(252, 252, 252, 0.4);
    border-radius: 12px;
    box-shadow: 0 20px 50px rgba(204, 204, 204, 0.05);
}

.app-menu-item-icon {
    width: 32px;
    height: 32px;
    margin-right: 24px;
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
    width: 300px;
    height: 150px;
    border-radius: 12px;
    box-shadow: 0 20px 50px rgba(204, 204, 204, 0.05);
    background-color: #fcfcfc;
    margin-right: 38px;
    margin-bottom: 38px;
}

</style>
