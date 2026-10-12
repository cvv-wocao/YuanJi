<template>
  <view class="address-edit-container">
    <view class="form-section">
      <view class="form-item">
        <text class="label">收货人</text>
        <input type="text" v-model="form.receiverName" placeholder="请输入收货人姓名" class="input" />
      </view>
      <view class="form-item">
        <text class="label">手机号</text>
        <input type="number" v-model="form.receiverPhone" placeholder="请输入手机号" class="input" />
      </view>
      <view class="form-item">
        <text class="label">所在地区</text>
        <view class="region-picker" @click="showRegionPicker">
          <text class="region-text">{{ form.province }}{{ form.city }}{{ form.district }}</text>
          <image src="https://via.placeholder.com/16x16?text=→" class="arrow-icon" />
        </view>
      </view>
      <view class="form-item">
        <text class="label">详细地址</text>
        <input type="text" v-model="form.detailAddress" placeholder="请输入详细地址" class="input" style="height: 80px;" />
      </view>
      <!-- <view class="form-item checkbox">
        <checkbox v-model="form.isDefault" />
        <text>设为默认地址</text>
      </view> -->
	  <view class="form-item checkbox">
	    <checkbox-group @change="toggleDefault">
			<checkbox :checked="form.isDefault" />
	    </checkbox-group>
	    <text>设为默认地址</text>
	    </view>
    </view>

    <view class="submit-btn" @click="saveAddress">保存</view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      form: {
        id: '',
        receiverName: '',
        receiverPhone: '',
        province: '北京市',
        city: '北京市',
        district: '海淀区',
        detailAddress: '',
        isDefault: false
      },
      token: ''
    };
  },
  onLoad(options) {
    this.getToken();
    if (options.id) {
      this.form.id = options.id;
      this.loadAddress();
    }
  },
  methods: {
	toggleDefault(e) {
	  this.form.isDefault = e.detail.value.length > 0;
	},
    getToken() {
      try {
        this.token = uni.getStorageSync('token') || '';
      } catch (e) {
        console.error('获取token失败', e);
      }
    },
    loadAddress() {
      uni.request({
        url: `http://localhost:8080/address/detail/${this.form.id}`,
        method: 'GET',
        header: {
          'Authorization': 'Bearer ' + this.token
        },
        success: (res) => {
          if (res.data.success) {
            const address = res.data.data;
            this.form = {
              id: address.id,
              receiverName: address.receiverName,
              receiverPhone: address.receiverPhone,
              province: address.province,
              city: address.city,
              district: address.district,
              detailAddress: address.detailAddress,
              isDefault: address.isDefault
            };
          } else {
            uni.showToast({
              title: res.data.message || '获取地址详情失败',
              icon: 'none'
            });
          }
        },
        fail: (err) => {
          console.error('加载地址详情失败', err);
          uni.showToast({
            title: '网络错误，请稍后重试',
            icon: 'none'
          });
        }
      });
    },
    showRegionPicker() {
      uni.showActionSheet({
        title: '选择省份',
        itemList: [
          '北京市', '天津市', '河北省', '山西省', '内蒙古自治区',
          '辽宁省', '吉林省', '黑龙江省', '上海市', '江苏省',
          '浙江省', '安徽省', '福建省', '江西省', '山东省',
          '河南省', '湖北省', '湖南省', '广东省', '广西壮族自治区',
          '海南省', '重庆市', '四川省', '贵州省', '云南省',
          '西藏自治区', '陕西省', '甘肃省', '青海省', '宁夏回族自治区',
          '新疆维吾尔自治区', '台湾省', '香港特别行政区', '澳门特别行政区'
        ],
        success: (res) => {
          const provinces = [
            '北京市', '天津市', '河北省', '山西省', '内蒙古自治区',
            '辽宁省', '吉林省', '黑龙江省', '上海市', '江苏省',
            '浙江省', '安徽省', '福建省', '江西省', '山东省',
            '河南省', '湖北省', '湖南省', '广东省', '广西壮族自治区',
            '海南省', '重庆市', '四川省', '贵州省', '云南省',
            '西藏自治区', '陕西省', '甘肃省', '青海省', '宁夏回族自治区',
            '新疆维吾尔自治区', '台湾省', '香港特别行政区', '澳门特别行政区'
          ];
          this.form.province = provinces[res.tapIndex];
          this.showCityPicker();
        }
      });
    },
    showCityPicker() {
      const cityMap = {
        '北京市': ['北京市'],
        '天津市': ['天津市'],
        '河北省': ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'],
        '山西省': ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市'],
        '内蒙古自治区': ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'],
        '辽宁省': ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'],
        '吉林省': ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州'],
        '黑龙江省': ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市', '大兴安岭地区'],
        '上海市': ['上海市'],
        '江苏省': ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'],
        '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市'],
        '安徽省': ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'],
        '福建省': ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市'],
        '江西省': ['南昌市', '景德镇市', '萍乡市', '九江市', '新余市', '鹰潭市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市'],
        '山东省': ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '莱芜市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市'],
        '河南省': ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市'],
        '湖北省': ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州'],
        '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州'],
        '广东省': ['广州市', '韶关市', '深圳市', '珠海市', '汕头市', '佛山市', '江门市', '湛江市', '茂名市', '肇庆市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市'],
        '广西壮族自治区': ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'],
        '海南省': ['海口市', '三亚市', '三沙市', '儋州市'],
        '重庆市': ['重庆市'],
        '四川省': ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州'],
        '贵州省': ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州'],
        '云南省': ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州'],
        '西藏自治区': ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区'],
        '陕西省': ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'],
        '甘肃省': ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州'],
        '青海省': ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州'],
        '宁夏回族自治区': ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'],
        '新疆维吾尔自治区': ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区'],
        '台湾省': ['台北市', '新北市', '桃园市', '台中市', '台南市', '高雄市', '基隆市', '新竹市', '嘉义市'],
        '香港特别行政区': ['香港特别行政区'],
        '澳门特别行政区': ['澳门特别行政区']
      };
      const cities = cityMap[this.form.province] || ['市区'];
      uni.showActionSheet({
        title: '选择城市',
        itemList: cities,
        success: (res) => {
          this.form.city = cities[res.tapIndex];
          this.showDistrictPicker();
        }
      });
    },
    showDistrictPicker() {
      const districtMap = {
        '北京市': ['东城区', '西城区', '朝阳区', '丰台区', '石景山区', '海淀区', '门头沟区', '房山区', '通州区', '顺义区', '昌平区', '大兴区', '怀柔区', '平谷区', '密云区', '延庆区'],
        '天津市': ['和平区', '河东区', '河西区', '南开区', '河北区', '红桥区', '东丽区', '西青区', '津南区', '北辰区', '武清区', '宝坻区', '滨海新区', '宁河区', '静海区', '蓟州区'],
        '石家庄市': ['长安区', '桥西区', '新华区', '井陉矿区', '裕华区', '藁城区', '鹿泉区', '栾城区', '井陉县', '正定县', '行唐县', '灵寿县', '高邑县', '深泽县', '赞皇县', '无极县', '平山县', '元氏县', '赵县', '晋州市', '新乐市'],
        '太原市': ['小店区', '迎泽区', '杏花岭区', '尖草坪区', '万柏林区', '晋源区', '清徐县', '阳曲县', '娄烦县', '古交市'],
        '呼和浩特市': ['新城区', '回民区', '玉泉区', '赛罕区', '土默特左旗', '托克托县', '和林格尔县', '清水河县', '武川县'],
        '沈阳市': ['和平区', '沈河区', '大东区', '皇姑区', '铁西区', '苏家屯区', '浑南区', '沈北新区', '于洪区', '辽中区', '康平县', '法库县', '新民市'],
        '长春市': ['南关区', '宽城区', '朝阳区', '二道区', '绿园区', '双阳区', '九台区', '农安县', '榆树市', '德惠市'],
        '哈尔滨市': ['道里区', '南岗区', '道外区', '平房区', '松北区', '香坊区', '呼兰区', '阿城区', '双城区', '依兰县', '方正县', '宾县', '巴彦县', '木兰县', '通河县', '延寿县', '尚志市', '五常市'],
        '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区', '虹口区', '杨浦区', '闵行区', '宝山区', '嘉定区', '浦东新区', '金山区', '松江区', '青浦区', '奉贤区', '崇明区'],
        '南京市': ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区', '溧水区', '高淳区'],
        '杭州市': ['上城区', '下城区', '江干区', '拱墅区', '西湖区', '滨江区', '萧山区', '余杭区', '富阳区', '临安区', '桐庐县', '淳安县', '建德市'],
        '合肥市': ['瑶海区', '庐阳区', '蜀山区', '包河区', '长丰县', '肥东县', '肥西县', '庐江县', '巢湖市'],
        '福州市': ['鼓楼区', '台江区', '仓山区', '马尾区', '晋安区', '长乐区', '闽侯县', '连江县', '罗源县', '闽清县', '永泰县', '平潭县', '福清市'],
        '南昌市': ['东湖区', '西湖区', '青云谱区', '湾里区', '青山湖区', '新建区', '南昌县', '安义县', '进贤县'],
        '济南市': ['历下区', '市中区', '槐荫区', '天桥区', '历城区', '长清区', '章丘区', '济阳区', '莱芜区', '钢城区', '平阴县', '商河县'],
        '郑州市': ['中原区', '二七区', '管城回族区', '金水区', '上街区', '惠济区', '中牟县', '巩义市', '荥阳市', '新密市', '新郑市', '登封市'],
        '武汉市': ['江岸区', '江汉区', '硚口区', '汉阳区', '武昌区', '青山区', '洪山区', '东西湖区', '汉南区', '蔡甸区', '江夏区', '黄陂区', '新洲区'],
        '长沙市': ['芙蓉区', '天心区', '岳麓区', '开福区', '雨花区', '望城区', '长沙县', '浏阳市', '宁乡市'],
        '广州市': ['荔湾区', '越秀区', '海珠区', '天河区', '白云区', '黄埔区', '番禺区', '花都区', '南沙区', '从化区', '增城区'],
        '深圳市': ['罗湖区', '福田区', '南山区', '宝安区', '龙岗区', '盐田区', '龙华区', '坪山区', '光明区'],
        '南宁市': ['兴宁区', '青秀区', '江南区', '西乡塘区', '良庆区', '邕宁区', '武鸣区', '隆安县', '马山县', '上林县', '宾阳县', '横县'],
        '海口市': ['秀英区', '龙华区', '琼山区', '美兰区'],
        '重庆市': ['万州区', '涪陵区', '渝中区', '大渡口区', '江北区', '沙坪坝区', '九龙坡区', '南岸区', '北碚区', '綦江区', '大足区', '渝北区', '巴南区', '黔江区', '长寿区', '江津区', '合川区', '永川区', '南川区', '璧山区', '铜梁区', '潼南区', '荣昌区', '开州区', '梁平区', '武隆区'],
        '成都市': ['锦江区', '青羊区', '金牛区', '武侯区', '成华区', '龙泉驿区', '青白江区', '新都区', '温江区', '双流区', '郫都区', '金堂县', '大邑县', '蒲江县', '新津县', '都江堰市', '彭州市', '邛崃市', '崇州市', '简阳市'],
        '贵阳市': ['南明区', '云岩区', '花溪区', '乌当区', '白云区', '观山湖区', '开阳县', '息烽县', '修文县', '清镇市'],
        '昆明市': ['五华区', '盘龙区', '官渡区', '西山区', '东川区', '呈贡区', '晋宁区', '富民县', '宜良县', '石林彝族自治县', '嵩明县', '禄劝彝族苗族自治县', '寻甸回族彝族自治县', '安宁市'],
        '拉萨市': ['城关区', '堆龙德庆区', '达孜区', '林周县', '当雄县', '尼木县', '曲水县', '墨竹工卡县'],
        '西安市': ['新城区', '碑林区', '莲湖区', '灞桥区', '未央区', '雁塔区', '阎良区', '临潼区', '长安区', '高陵区', '鄠邑区', '蓝田县', '周至县'],
        '兰州市': ['城关区', '七里河区', '西固区', '安宁区', '红古区', '永登县', '皋兰县', '榆中县'],
        '西宁市': ['城东区', '城中区', '城西区', '城北区', '大通回族土族自治县', '湟中县', '湟源县'],
        '银川市': ['兴庆区', '西夏区', '金凤区', '永宁县', '贺兰县', '灵武市'],
        '乌鲁木齐市': ['天山区', '沙依巴克区', '新市区', '水磨沟区', '头屯河区', '达坂城区', '米东区', '乌鲁木齐县'],
        '台北市': ['中正区', '大同区', '中山区', '松山区', '大安区', '万华区', '信义区', '士林区', '北投区', '内湖区', '南港区', '文山区'],
        '香港特别行政区': ['中西区', '湾仔区', '东区', '南区', '油尖旺区', '深水埗区', '九龙城区', '黄大仙区', '观塘区', '荃湾区', '屯门区', '元朗区', '北区', '大埔区', '西贡区', '沙田区', '葵青区', '离岛区'],
        '澳门特别行政区': ['花地玛堂区', '圣安多尼堂区', '大堂区', '望德堂区', '风顺堂区', '嘉模堂区', '圣方济各堂区']
      };
      const districts = districtMap[this.form.city] || ['城区'];
      uni.showActionSheet({
        title: '选择区县',
        itemList: districts,
        success: (res) => {
          this.form.district = districts[res.tapIndex];
        }
      });
    },
    saveAddress() {
      if (!this.form.receiverName) {
        uni.showToast({ title: '请输入收货人姓名', icon: 'none' });
        return;
      }
      if (!this.form.receiverPhone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' });
        return;
      }
      if (!this.form.detailAddress) {
        uni.showToast({ title: '请输入详细地址', icon: 'none' });
        return;
      }

      // 判断是新增还是编辑
      const url = this.form.id ? 'http://localhost:8080/address/update' : 'http://localhost:8080/address/add';

      // 根据是新增还是编辑，准备不同的请求数据
      const requestData = {
        receiverName: this.form.receiverName,
        receiverPhone: this.form.receiverPhone,
        province: this.form.province,
        city: this.form.city,
        district: this.form.district,
        detailAddress: this.form.detailAddress,
        isDefault: this.form.isDefault ? 1 : 0  // 将布尔值转换为数字
      };

      // 如果是编辑，添加id字段
      if (this.form.id) {
        requestData.id = this.form.id;
      }

      uni.request({
        url: url,
        method: 'POST',
        data: requestData,
        header: {
          'Authorization': 'Bearer ' + this.token,
          'Content-Type': 'application/json'
        },
        success: (res) => {
          if (res.data.success) {
            uni.showToast({
              title: '保存成功',
              icon: 'success'
            });
            setTimeout(() => {
              uni.navigateBack({ delta: 1 });
            }, 1500);
          } else {
            uni.showToast({
              title: res.data.message || '保存失败',
              icon: 'none'
            });
          }
        },
        fail: (err) => {
          console.error('保存地址失败', err);
          uni.showToast({
            title: '网络错误，请稍后重试',
            icon: 'none'
          });
        }
      });
    }
  }
};
</script>

<style scoped>
.address-edit-container {
  padding: 15px;
  padding-bottom: calc(96px + env(safe-area-inset-bottom));
}

.form-section {
  background-color: white;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.form-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.label {
  width: 80px;
  font-size: 14px;
  color: #333;
}

.input {
  flex: 1;
  font-size: 14px;
  color: #333;
  padding: 5px;
}

.region-picker {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px;
}

.region-text {
  font-size: 14px;
  color: #333;
}

.arrow-icon {
  width: 16px;
  height: 16px;
}

.checkbox {
  justify-content: flex-start;
}

.checkbox text {
  margin-left: 10px;
  font-size: 14px;
  color: #333;
}

.submit-btn {
  width: 100%;
  height: 45px;
  line-height: 45px;
  text-align: center;
  background-color: #1890ff;
  color: white;
  border-radius: 22px;
  font-size: 16px;
  margin-top: 20px;
}
</style>
