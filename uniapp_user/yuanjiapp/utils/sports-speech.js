// 语音播报相关功能
import { ref } from 'vue';

const isSpeechEnabled = ref(true);
const lastSpokenText = ref('');

/**
 * 使用 Web Speech API 播报文本
 * @param {string} text - 需要播报的文本内容
 * @returns {Promise<void>}
 */
const speakText = (text) => {
  return new Promise((resolve, reject) => {
    if (!isSpeechEnabled.value || !text || text === '检测中...') {
      reject('语音播报未开启或文本无效');
      return;
    }
    // 浏览器兼容性检查
    if (!window.speechSynthesis) {
      console.warn('当前浏览器不支持语音播报');
      reject('Browser does not support speech synthesis');
      return;
    }
    // 避免重复播报相同内容
    if (lastSpokenText.value === text) {
      console.log('相同文本，跳过播报');
      return;
    }
    lastSpokenText.value = text;
    // 取消任何正在进行的播报
    if (window.speechSynthesis.speaking) {
      window.speechSynthesis.cancel();
    }
    // 创建语音实例
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'zh-CN';
    utterance.rate = 0.9;
    utterance.pitch = 1.0;
    utterance.volume = 1.0;
    // 尝试设置一个好听的中文语音
    const setVoice = () => {
      const voices = window.speechSynthesis.getVoices();
      const chineseVoice = voices.find(voice => voice.lang === 'zh-CN' && (voice.name.includes('Google') || voice.name.includes('Ting-Ting')));
      if (chineseVoice) utterance.voice = chineseVoice;
    };
    if (window.speechSynthesis.getVoices().length) {
      setVoice();
    } else {
      window.speechSynthesis.onvoiceschanged = setVoice;
    }
    // 事件处理
    utterance.onend = () => {
      console.log('播报完成');
      resolve();
    };
    
    utterance.onerror = (event) => {
      console.error('播报错误:', event);
      reject(event.error);
    };
    // 开始播报
    window.speechSynthesis.speak(utterance);
  });
};

export { speakText, isSpeechEnabled, lastSpokenText };