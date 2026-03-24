package com.example.pdtranslator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoudaoWebCodecTest {

  @Test
  fun `decrypts captured webtranslate payload`() {
    val decrypted = YoudaoWebCodec.decryptPayload(
      payload = "Z21kD9ZK1ke6ugku2ccWu-MeDWh3z252xRTQv-wZ6jddVo3tJLe7gIXz4PyxGl73nSfLAADyElSjjvrYdCvEP4pfohVVEX1DxoI0yhm36ytQNvu-WLU94qULZQ72aml6MdaC9LzSO4qdlPmtuyg_YvDQQMdLTVnTMYtInG0ZBrNPOPNzjjiq-jHBIXclo3bdhEzfuCQJrHD9t_lAkSsXJJMWMnE2HZm_g86NPlGORn2w21mrfqMP_Mc96pue4l-upHy_Dlw8NKfIiqjkfVSG6kxyE3oj-56b5O7rjrKZ1ddUeivB627dvhzgf1Q5iugT-mU2NUPmcPsbp6iVXQ24ol07NwF1jJZmICT4Jc4qv4dt5oxft01Fv6RKqUSspdQjl1ePM9YiH_1iKjo-LN5UnVOp3wvbZSKvNIBKC-Th1NLSPkSEYMTl3AfBb7TKDSw3HDpsfZSAs-I78hplaQdsUjfG9YiyYER_pWstmNdsnMzzUV7GSp6vp5Jh8BkYA6hjQr93DYTafwAeXml0PoFr3hM1GKC2HSoN-k40A06hKJ0P6I-dFnKyvn0MyEcoUh2ggzQLyd4FukFaKxQzQfaA5Z7ZByF-7jA5HCbwQeeJgUV4IY9iGyZ5xkYG_AlegcuXVGroLs21GWfY-SBaom7DUzkBTC38fotkVCfzd5gPsZofTr_ZmWmM_CIa4WLulYT5xsvKVUFpguspyPrcEXs6koZhny5ZCzbrIxhMJX7uyEF9rxwnIoYfoH4KTiZIUoSUju7N8Vo-APiKOoBkeCrSwfyDXNRKxDEN4Yg8afiW-QMP5LhdOWtLPgZZJDXpgU7jXV5wLLk8UA_eTKifbRI3OzxinwsV3l3xGZnnbS6NGZKfJrLGkdGjZVB9LZhECOMKD8OM-bnJRKKpI6-5juw1lDeTLyxewCx8aAF9BAW9JqZIVtMLw7wMKwVip27BbLmCUKT0oaWDK8DTos-_gIwkF5tCzphT8FQL2ysIO-Q92IZ8oRPYQzm9QlUNN_6vC-Y-HQN0-S8WPAHVri2tdmTy8_uZm5bzZ-oLQXNSZWItt6-f3NpKQvPOidJTTq8ody2eBXJa8ImoF1I4ZeX0quLMme7T4SsuMMI0pKCXETNU5ae30Vm818QSbt2yqZAZe7_OLE-gUgBtX0KgwTxMlaswrRkq4q76NvXY3Pz0uwzW24aTx_iH3gcXu6Nywdl5Jk20jDDYlozu74RyrJZYj3Xol1roY3LD8HqyqZUqbssKYHeFI7VNqi8dQ9SboWSC43J_Uv0exurLIXtEYFWuKp-TgKdXdYn1lPXIINwsPx2Ohpc=",
      aesKeySeed = "ydsecret://query/key/B*RGygVywfNBwpmBaZg*WT7SIOUP2T0C9WHMZN39j^DAdaZhAnxvGcCY6VYFwnHl",
      aesIvSeed = "ydsecret://query/iv/C@lZe2YzHtZ2CYgaXKSVfsb7Y4QWHjITPPZ0nQp87fBeJ!Iv6v^6fvi2WN@bYpJ4"
    )

    assertTrue(decrypted.contains("\"src\":\"hello\""))
    assertEquals("你好", YoudaoWebCodec.extractTranslation(decrypted))
  }

  @Test
  fun `extracts translation from webtranslate json`() {
    val json = """
      {
        "code": 0,
        "translateResult": [
          [
            { "src": "hello", "tgt": "你好" }
          ],
          [
            { "src": "world", "tgt": "世界" }
          ]
        ]
      }
    """.trimIndent()

    assertEquals("你好世界", YoudaoWebCodec.extractTranslation(json))
  }

  @Test
  fun `prefers ec dictionary gloss and normalizes first sense`() {
    val json = """
      {
        "ec": {
          "word": {
            "trs": [
              { "tran": "猫，猫科动物" }
            ]
          }
        },
        "individual": {
          "trs": [
            { "tran": "喵星人" }
          ]
        }
      }
    """.trimIndent()

    assertEquals("猫", YoudaoWebCodec.extractDictionaryTranslation(json))
  }

  @Test
  fun `falls back to individual dictionary gloss`() {
    val json = """
      {
        "individual": {
          "trs": [
            { "tran": "世界；世间；领域" }
          ]
        }
      }
    """.trimIndent()

    assertEquals("世界", YoudaoWebCodec.extractDictionaryTranslation(json))
  }
}
