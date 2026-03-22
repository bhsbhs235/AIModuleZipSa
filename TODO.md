# 기능 개선 사항
- [ ] 1. 사용자 대화 기억 기능 개선
  - [ ] a. ChatMemory로 세션 내에서 이전 대화 기억 (redis 활용)
  - [ ] b. Elasticsearch RAG 로 장기 기억 구현

- [ ] 2. 민사법 Fine-Tuned 모델 적용
  - [ ] a. 민사법 Instruction Tuning 데이터를 JSONL 형식으로 변환 (Vertex AI SFT 요구 형식)
  - [ ] b. Vertex AI Supervised Fine-Tuning 실행 (training + validation 데이터)
  - [ ] c. ChatClient 이중 구성 (VertexAiConfig)
    - `generalChatClient` — gemini-2.0-flash (일반 부동산)
    - `civilLawChatClient` — tuned model endpoint (민사법 전문)
  - [ ] d. 자동 분류 기반 라우팅 (ChatController)
    - flash 모델로 질문 분류 (Y/N, 1토큰) → 민사법이면 tuned 모델로 라우팅
    - `Mono.fromCallable()` + `boundedElastic`으로 blocking 처리