<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
    <header>
        <span class="logo"></span>
        <span class="title"></span>
    </header>
    <div>
        <aside>
            <div class="menu">
                <div class="item"><RouterLink to="/">Home</RouterLink></div>
                <div class="item"><RouterLink to="/about">About</RouterLink></div>
                <div class="item"><RouterLink to="/proxy">代理管理</RouterLink></div>
                <div class="item"><RouterLink to="/tailwind">Tailwind示例</RouterLink></div>
            </div>
        </aside>
        <div>
            <main>
                <RouterView />
            </main>
            <footer>
                <div>
                    <p>
                        Copyright &copy; 2023
                    </p>
                </div>
            </footer>
        </div>
    </div>

    
</template>

<style scoped>

</style>
