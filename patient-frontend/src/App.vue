<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  console.log("云脑诊疗患者端启动");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style>
page {
  min-height: 100%;
  color: #16324a;
  background: #f4f8fb;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif;
}
button::after { border: 0; }
.safe-bottom { padding-bottom: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
