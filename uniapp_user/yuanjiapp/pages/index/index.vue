<template>
	<view class="content">
		<image class="logo" src="/static/logo.png"></image>
		<view class="text-area">
			<text class="title">{{title}}</text>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				title: 'Hello'
			}
		},
		onLoad() {

		},
		methods: {

		}
	}
</script>

<style>

	.recommend-products {
		background-color: white;
		margin: 10px 0;
		padding: 15px;
	}

	.product-scroll {
		white-space: nowrap;
		width: 100%;
	}

	.scroll-product-item {
		display: inline-block;
		width: 140px;
		margin-right: 10px;
		vertical-align: top;
	}

	.search-btn {
		padding: 0 10px;
		color: #1890ff;
		font-size: 14px;
	}

	.content {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.logo {
		height: 200rpx;
		width: 200rpx;
		margin-top: 200rpx;
		margin-left: auto;
		margin-right: auto;
		margin-bottom: 50rpx;
	}

	.text-area {
		display: flex;
		justify-content: center;
	}

	.title {
		font-size: 36rpx;
		color: #8f8f94;
	}
</style>
