# spark_client.py
import _thread as thread
import base64
import hashlib
import hmac
import json
import ssl
from datetime import datetime
from time import mktime
from urllib.parse import urlencode, urlparse
from wsgiref.handlers import format_date_time
import websocket

class SparkClient:
    def __init__(self, appid, api_key, api_secret, spark_url, domain):
        self.appid = appid
        self.api_key = api_key
        self.api_secret = api_secret
        self.spark_url = spark_url
        self.domain = domain
        self.answer = ""
        self.is_first_content = False

    class WsParam:
        def __init__(self, appid, api_key, api_secret, spark_url):
            self.appid = appid
            self.api_key = api_key
            self.api_secret = api_secret
            self.host = urlparse(spark_url).netloc
            self.path = urlparse(spark_url).path
            self.spark_url = spark_url

        def create_url(self):
            now = datetime.now()
            date = format_date_time(mktime(now.timetuple()))
            signature_origin = f"host: {self.host}\ndate: {date}\nGET {self.path} HTTP/1.1"
            signature_sha = hmac.new(self.api_secret.encode('utf-8'),
                                     signature_origin.encode('utf-8'),
                                     digestmod=hashlib.sha256).digest()
            signature_sha_base64 = base64.b64encode(signature_sha).decode('utf-8')
            authorization_origin = f'api_key="{self.api_key}", algorithm="hmac-sha256", headers="host date request-line", signature="{signature_sha_base64}"'
            authorization = base64.b64encode(authorization_origin.encode('utf-8')).decode('utf-8')
            v = {"authorization": authorization, "date": date, "host": self.host}
            return self.spark_url + '?' + urlencode(v)

    def _on_message(self, ws, message):
        data = json.loads(message)
        code = data['header']['code']
        if code != 0:
            print(f"星火错误: {code}, {data}")
            ws.close()
            return
        choices = data["payload"]["choices"]
        status = choices["status"]
        text = choices['text'][0]
        if 'reasoning_content' in text and text['reasoning_content']:
            # 这里可以收集思维链，但为了简化，先跳过
            pass
        if 'content' in text and text['content']:
            self.answer += text["content"]
        if status == 2:
            ws.close()

    def _on_error(self, ws, error):
        print(f"WebSocket错误: {error}")

    def _on_close(self, ws, one, two):
        pass

    def _on_open(self, ws):
        def run(*args):
            data = json.dumps({
                "header": {"app_id": self.appid, "uid": "1234"},
                "parameter": {"chat": {"domain": self.domain, "temperature": 0.7, "max_tokens": 2048}},
                "payload": {"message": {"text": [{"role": "user", "content": ws.question}]}}
            })
            ws.send(data)
        thread.start_new_thread(run, ())

    def ask(self, question):
        """发送问题并返回完整回答"""
        self.answer = ""
        self.is_first_content = False

        ws_param = self.WsParam(self.appid, self.api_key, self.api_secret, self.spark_url)
        ws_url = ws_param.create_url()

        ws = websocket.WebSocketApp(ws_url,
                                    on_message=self._on_message,
                                    on_error=self._on_error,
                                    on_close=self._on_close,
                                    on_open=self._on_open)
        ws.question = question
        ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

        return self.answer.strip()