<template>
  <view />
</template>

<script>
export default {
  onLaunch() {
    const isLoggedIn = uni.getStorageSync('isLoggedIn');
    if (!isLoggedIn) {
      uni.reLaunch({
        url: '/pages/login/login'
      });
    }
  },
  onShow() {
    console.log('App Show');
  },
  onHide() {
    console.log('App Hide');
  }
};
</script>

<style lang="scss">
page {
  min-height: 100%;
  background: #f4f7fb;
  color: #18243c;
  font-size: 28rpx;
}

view,
text,
scroll-view,
swiper,
swiper-item,
image,
button,
input,
textarea {
  box-sizing: border-box;
}

button {
  padding: 0;
  margin: 0;
  line-height: 1;
  background: transparent;
  border: 0;
}

button::after {
  border: 0;
}

image {
  display: block;
}

.glass-card {
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(16rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.76);
  box-shadow: 0 16rpx 44rpx rgba(60, 78, 110, 0.08);
}

.text-gradient {
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.reveal-anim {
  animation: reveal-up 0.45s ease;
}

@keyframes reveal-up {
  from {
    opacity: 0;
    transform: translateY(18rpx);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
