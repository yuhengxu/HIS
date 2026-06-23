<template>
  <view class="page">
    <view class="title">HIS + 康养移动端</view>
    <view class="card">OA 基座、物资查询与企业微信小程序入口骨架</view>
    <view class="grid">
      <view class="cell">用户权限</view>
      <view class="cell">OA</view>
      <view class="cell">物资</view>
      <view class="cell">审计</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 32rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  margin-bottom: 24rpx;
}

.card,
.cell {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  padding: 28rpx;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 18rpx;
}
</style>
