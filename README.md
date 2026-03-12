# 🏠 ZipSa (집사)

> 🤖 자연어로 부동산의 모든 것을 해결하는 AI 에이전트

## 💡 컨셉

**"집사"** 는 **집**을 **사**는 과정을 돕는 AI 에이전트입니다.
사용자가 자연어로 말하면, LLM이 의도를 파악하고 적절한 행동을 실행합니다.

### 💬 대화 예시

```
🧑 양재동 11-50 매물 좀 보여줘
🏠 양재동 11-50 주변 매물 3건을 찾았습니다. ...

🧑 이 중에 전용면적 30평 이상인 것만 필터해줘
🏠 조건에 맞는 매물 1건입니다. ...

🧑 이 매물 최근 실거래가 알려줘
🏠 최근 6개월 실거래 내역입니다. ...
```

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| ☕ Language | Java 21 |
| 🍃 Framework | Spring Boot 3.4 |
| ⚡ Reactive | Spring WebFlux |
| 🧠 AI / LLM | Spring AI (OpenAI, Vertex AI Gemini) |
| 📐 Embedding | Vertex AI Embedding |
| 🔍 Search | Elasticsearch |
| ☁️ Storage | Google Cloud Storage |
| 🔐 Security | Jasypt (설정 암호화) |

## 🚀 시작하기

### 📋 사전 요구사항

- ☕ JDK 21+
- 🔍 Elasticsearch
- 🔑 OpenAI API Key 또는 Google Cloud 인증

### ▶️ 실행

```bash
./gradlew bootRun
```

### 📦 빌드

```bash
./gradlew build
```

## 📁 프로젝트 구조

```
src/main/java/com/example/zipsa/
├── ZipSaApplication.java    # 🚪 애플리케이션 진입점
├── config/                  # ⚙️ 설정
├── controller/              # 🌐 API 엔드포인트
├── service/                 # 🧠 비즈니스 로직 / AI 에이전트
├── domain/                  # 📦 도메인 모델
└── infra/                   # 🔌 외부 연동 (Elasticsearch, GCS 등)
```
