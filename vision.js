// vision.js — 让纯文本模型也能"看图"：调用 glm-5.3-flash 把图片转成文字描述
// 用法: node vision.js <图片路径> [自定义提示语] [输出文件路径]
//       第3个参数给了就把结果以 UTF-8 直接写入该文件（推荐，避免终端重定向乱码）；不给则打印到屏幕
// 依赖: Node 18+（自带 fetch），无需安装任何包
// API Key 配置：优先读环境变量 ZAI_API_KEY；没设置就用下面引号里的 Key
const API_KEY = process.env.ZAI_API_KEY || "77d841901b34493d911bb312e20da393.oL7Jb5N8e77XTdWN";

const fs = require("fs");
const path = require("path");

const imgPath = process.argv[2];
const prompt = process.argv[3] || "请详细描述这张图片的全部内容";
const outFile = process.argv[4];
if (!imgPath) {
  console.error("用法: node vision.js <图片路径> [自定义提示语] [输出文件路径]");
  process.exit(1);
}

const ext = imgPath.split(".").pop().toLowerCase();
const mime =
  ext === "png" ? "image/png" :
  ext === "webp" ? "image/webp" :
  ext === "gif" ? "image/gif" : "image/jpeg";

const b64 = fs.readFileSync(imgPath).toString("base64");

// 端点说明: 走的是 GLM Coding Plan 专用端点（套餐额度）；若你是按量付费账户，
// 把下面地址改成 https://open.bigmodel.cn/api/paas/v4/chat/completions
fetch("https://open.bigmodel.cn/api/coding/paas/v4/chat/completions", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${API_KEY}`,
  },
  body: JSON.stringify({
    model: "glm-5.3-flash",
    messages: [
      {
        role: "user",
        content: [
          { type: "image_url", image_url: { url: `data:${mime};base64,${b64}` } },
          { type: "text", text: prompt },
        ],
      },
    ],
  }),
})
  .then((r) => r.json())
  .then((d) => {
    if (d.error) {
      console.error("API 错误:", JSON.stringify(d.error));
      process.exit(1);
    }
    const content = d.choices?.[0]?.message?.content ?? JSON.stringify(d);
    if (outFile) {
      fs.mkdirSync(path.dirname(path.resolve(outFile)), { recursive: true });
      fs.writeFileSync(outFile, content, "utf8");
      console.log(`已保存到 ${outFile}（UTF-8 编码，共 ${content.length} 字符）`);
    } else {
      console.log(content);
    }
  })
  .catch((e) => {
    console.error("请求失败:", e.message);
    process.exit(1);
  });
