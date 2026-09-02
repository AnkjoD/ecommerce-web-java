# Homura Shop — AI Chatbot Sidecar

Service FastAPI độc lập xử lý ngôn ngữ tự nhiên, phân loại ý định và tìm kiếm sản phẩm cho Homura Shop bằng Agentic RAG.

## Tech Stack

| Thành phần       | Công nghệ                                       |
| ---------------- | ----------------------------------------------- |
| Framework        | FastAPI + Uvicorn + Python 3.13                 |
| LLM Router       | Google Gemini 2.0 Flash API                     |
| LLM Generate     | Ollama qwen2.5:7b (local)                       |
| Embedding        | `BAAI/bge-m3` (HuggingFace, local)              |
| Reranker         | `BAAI/bge-reranker-v2-m3` (CrossEncoder, local) |
| Vector Search    | PostgreSQL + pgvector (cosine similarity)       |
| Full-text Search | PostgreSQL FTS (plainto_tsquery)                |
| ORM              | SQLAlchemy → PostgreSQL                         |
| Cache            | Redis                                           |

## Kiến trúc Pipeline

```
User Query (từ NestJS, kèm userId đã verify JWT)
        │
        ▼
┌─────────────────────────────────┐
│  LLM Router — Gemini Flash      │  ~300-500ms
│  - Classify intent (6 loại)     │
│  - Extract filters              │
│    (brand, color, size, price)  │
│  - Extract order_product_hint   │
│  - Rewrite sai chính tả         │
│    (chỉ khi cần)                │
└─────────────────────────────────┘
        │
        ├── ORDER_INQUIRY
        │       └── SQL query orders WHERE user_id = :userId
        │           Tự phân biệt theo nội dung:
        │           - Có số đơn → WHERE id = :order_id
        │           - Có tên SP → JOIN order_items, ưu tiên đơn active
        │           - Không có gì → 5 đơn gần nhất
        │           → Template format response (~50ms, không LLM)
        │
        ├── COUPON_INQUIRY
        │       └── SQL query coupons
        │           → Template format response (~50ms, không LLM)
        │
        ├── CHITCHAT
        │       └── Template response thân thiện (~5ms, không LLM)
        │
        ├── OFFTOPIC
        │       └── Template response redirect về sản phẩm (~5ms, không LLM)
        │
        ├── NEED_CLARIFICATION
        │       └── Hỏi lại user (~5ms, không LLM)
        │
        └── PRODUCT_SEARCH
                │
                ▼
        ┌───────────────────────────┐
        │  Hybrid Search            │  ~200-500ms
        │  vector_search()          │
        │  + fts_search()           │
        │  Filters → SQL WHERE      │
        │  RRF merge                │
        └───────────────────────────┘
                │
                ▼
        ┌───────────────────────────┐
        │  CrossEncoder Reranker    │  ~100-300ms
        │  BAAI/bge-reranker-v2-m3  │
        └───────────────────────────┘
                │
                ▼
        ┌───────────────────────────┐
        │  LLM Generate             │  ~1-3s
        │  Ollama qwen2.5:7b        │
        │  Tư vấn dựa trên context  │
        └───────────────────────────┘
                │
                ▼
        Response to User
```

## Tại sao tách Router và Generate

Router dùng Gemini Flash vì đây là điểm fail đầu tiên của pipeline — nếu classify sai intent hoặc extract sai brand/size thì toàn bộ retrieval sai theo. Gemini Flash chính xác tiếng Việt tốt hơn, chi phí ~$1/tháng, không đáng lo.

Generate dùng qwen2.5:7b local vì lúc này context sản phẩm đã được retrieval + reranker lọc sạch — LLM chỉ cần đọc và viết câu trả lời tự nhiên, 7b là đủ.

## Intents được hỗ trợ (6 intents)

| Intent             | Ví dụ                                                                          | Xử lý                               |
| ------------------ | ------------------------------------------------------------------------------ | ----------------------------------- |
| PRODUCT_SEARCH     | "tìm giày chạy bộ", "Nike AF1 còn size 42 không", "Adidas Samba giá bao nhiêu" | Hybrid search + LLM generate        |
| ORDER_INQUIRY      | "đơn 1234 giao chưa", "cặp hàn quốc đến đâu rồi", "lịch sử mua hàng"           | SQL query + Template (không LLM)    |
| COUPON_INQUIRY     | "có mã giảm giá không", "voucher nào dùng được"                                | SQL query + Template (không LLM)    |
| CHITCHAT           | "xin chào shop", "cảm ơn bạn"                                                  | Template tĩnh, tone thân thiện      |
| OFFTOPIC           | "thời tiết hôm nay", "viết code giúp t"                                        | Template tĩnh, redirect về sản phẩm |
| NEED_CLARIFICATION | "mua gì đó đẹp", "cho t cái gì hay hay"                                        | Hỏi lại user                        |

**Lý do gộp so với thiết kế cũ:**

- STOCK_CHECK + PRICE_CHECK → PRODUCT_SEARCH: cả 3 đều đi qua hybrid_search(), LLM tự điều chỉnh câu trả lời theo context filters
- ORDER_LOOKUP + ORDER_HISTORY → ORDER_INQUIRY: cùng 1 function xử lý, SQL tự phân biệt:
  - Có số đơn → `WHERE id = :order_id`
  - Có tên sản phẩm → `JOIN order_items` + ưu tiên đơn đang active (shipping/processing) trước delivered
  - Không có gì → lấy 5 đơn gần nhất theo `created_at DESC`
- CHITCHAT và OFFTOPIC **không gộp** vì tone response khác nhau — một cái mời tiếp tục, một cái redirect

## Router Output

```json
{
  "intent": "PRODUCT_SEARCH | ORDER_INQUIRY | COUPON_INQUIRY | CHITCHAT | OFFTOPIC | NEED_CLARIFICATION",
  "query_rewritten": "câu hỏi đã sửa chính tả, tối đa 10 từ (giữ nguyên nếu đã ổn)",
  "filters": {
    "brand": null,
    "color": null,
    "size": null,
    "max_price": null,
    "sort": null
  },
  "order_id": null,
  "order_product_hint": null,
  "requires_auth": false,
  "confidence": 0.0
}
```

## Cài đặt

### 1. Môi trường

```bash
conda create -n aiEnv python=3.13
conda activate aiEnv
pip install -r requirements.txt
```

### 2. Biến môi trường

File `.env` đặt ở thư mục cha (`Web-ban-hang/.env`):

```env
# Bắt buộc
DATABASE_URL=postgresql+psycopg://postgres:password@127.0.0.1:5432/web-ban-hang
GEMINI_API_KEY=...          # cho LLM Router

# Ollama (generate)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b

# Tuỳ chọn
REDIS_URL=redis://localhost:6379
WEBHOOK_SECRET=...
RAG_TOP_K=5
AI_CACHE_TTL=600
```

### 3. Khởi chạy Ollama

```bash
ollama pull qwen2.5:7b
ollama serve
```

### 4. Sync sản phẩm vào Vector DB

Chạy lần đầu hoặc khi có sản phẩm mới:

```bash
python sync_products.py
```

### 5. Khởi chạy server

Development:

```bash
python -m fastapi dev main.py
```

Production:

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

> Không dùng `--reload` trong production vì mỗi lần reload sẽ load lại model HuggingFace (~60s).

## Bảo mật

`user_id` chỉ được truyền từ NestJS sau khi đã verify JWT — FastAPI không verify JWT trực tiếp. Mọi query liên quan đến đơn hàng đều có `WHERE user_id = :userId`, không thể lấy đơn của người khác.

FastAPI chỉ expose port nội bộ, không ra internet trực tiếp.

## Lưu ý

- Lần chạy đầu tiên FastAPI tự download model HuggingFace (`bge-m3`, `bge-reranker-v2-m3`) — mất 5-10 phút tuỳ tốc độ mạng.
- NestJS Proxy timeout đặt là **120s** để cover thời gian khởi động model lần đầu.
- Cache Redis tự động bỏ qua cho request có `user_id` (tránh cache lệch dữ liệu giữa các user).
