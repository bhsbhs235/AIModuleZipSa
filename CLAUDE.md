# 🤖 Claude Code 활용 및 검토 가이드라인

AI 어시스턴트(Claude)를 통해 코드를 생성하거나 리팩토링할 때, 최종 반영 전 아래 체크리스트를 반드시 확인합니다.

---

### 1. 성능 및 최적화 (Performance)
- [ ] **시간 복잡도:** 현재 구현이 최선의 알고리즘인지, 더 효율적인 구조로 바꿀 수 없는지 확인합니다.
- [ ] **메모리 효율:** 불필요한 메모리 낭비나 대용량 데이터 처리 시 병목이 생기지 않는지 검토합니다.
- [ ] **자원 해제:** 데이터베이스 연결, 파일 스트림 등이 올바르게 닫히는지(Close) 확인합니다.

### 2. 코드 품질 및 안정성 (Quality & Stability)
- [ ] **예외 처리:** 예외 상황(Edge Case, Null 값 등)에 대한 방어 코드가 견고하게 작성되었는지 확인합니다.
- [ ] **하드코딩 지양:** 설정 값이나 매직 넘버가 코드에 직접 노출되어 있지 않은지 확인합니다.
- [ ] **가독성:** 변수/함수명이 직관적이며, 복잡한 로직에는 적절한 주석이 달렸는지 확인합니다.

### 3. 최종 확인 (Cross-Check)
- [ ] 클로드가 제안한 외부 라이브러리나 API가 현재 프로젝트 버전과 호환되는지 확인합니다.
- [ ] 작성된 코드가 기존 시스템의 아키텍처 및 코딩 컨벤션을 준수하는지 점검합니다.

---

## 📘 Effective Java 코딩 가이드라인

> **Joshua Bloch의 Effective Java (3rd Edition)** 기반으로, 본 프로젝트(Java 21 / Spring Boot 3.4.11 / WebFlux / Lombok)에 맞게 정리한 코딩 규칙입니다.
> 코드를 생성하거나 리팩토링할 때 반드시 아래 원칙들을 준수합니다.

---

### Chapter 2. 객체 생성과 파괴 (Creating and Destroying Objects)

#### Item 1: 생성자 대신 정적 팩터리 메서드를 고려하라
- 이름을 가질 수 있어 가독성이 높고, 호출할 때마다 새 인스턴스를 만들지 않아도 되며, 반환 타입의 하위 타입 객체를 반환할 수 있다.
- `from`, `of`, `valueOf`, `instance`, `create`, `newInstance` 등 관례적 네이밍을 따른다.

```java
// BAD - 생성자만으로는 의미가 불명확
new ApiResponse(200, "OK", data);

// GOOD - 정적 팩터리 메서드로 의도를 명확히 전달
ApiResponse.success(data);
ApiResponse.error(ErrorCode.NOT_FOUND, "리소스를 찾을 수 없습니다");
```

#### Item 2: 생성자에 매개변수가 많으면 빌더를 고려하라
- 매개변수가 4개 이상이거나, 선택적 매개변수가 많을 때 빌더 패턴을 사용한다.
- Lombok의 `@Builder`를 적극 활용하되, 불변 객체 보장에 유의한다.

```java
// GOOD - Lombok @Builder 활용
@Builder
public class SearchCriteria {
    private final String query;
    private final int page;
    @Builder.Default
    private final int size = 10;
    @Builder.Default
    private final SortOrder sort = SortOrder.RELEVANCE;
}
```

#### Item 3: private 생성자나 열거 타입으로 싱글턴임을 보증하라
- Spring의 `@Component`, `@Service`, `@Configuration` 빈은 기본적으로 싱글턴이므로 별도의 싱글턴 패턴 구현은 불필요하다.
- 유틸리티 클래스는 Spring 빈이 아닌 경우 `private` 생성자로 인스턴스화를 방지한다.

#### Item 4: 인스턴스화를 막으려거든 private 생성자를 사용하라
- 유틸리티 클래스(정적 메서드만 모아놓은 클래스)는 반드시 `private` 생성자를 선언한다.
- Lombok의 `@UtilityClass`를 활용할 수 있다.

```java
// GOOD
@UtilityClass
public class UriUtils {
    public static URI buildUri(String baseUrl, Map<String, String> params) { ... }
}
```

#### Item 5: 자원을 직접 명시하지 말고 의존 객체 주입을 사용하라
- Spring의 생성자 주입(`@RequiredArgsConstructor`)을 기본으로 사용한다.
- `@Autowired` 필드 주입은 지양하고, 생성자 주입을 통해 불변성과 테스트 용이성을 확보한다.

```java
// BAD - 필드 주입
@Autowired
private ChatModel chatModel;

// GOOD - 생성자 주입 (Lombok 활용)
@RequiredArgsConstructor
public class ChatController {
    private final ChatModel chatModel;
    private final ChatClient chatClient;
}
```

#### Item 6: 불필요한 객체 생성을 피하라
- `String`, `Boolean`, `Integer` 등의 박싱 타입은 불필요하게 생성하지 않는다.
- 반복 사용되는 정규표현식은 `Pattern.compile()`로 미리 컴파일하여 상수로 보관한다.
- 오토박싱이 빈번히 발생하는 루프에 주의한다.

```java
// BAD - 매번 Pattern 재컴파일
public boolean isValid(String input) {
    return input.matches("[a-zA-Z0-9]+");
}

// GOOD - Pattern 사전 컴파일
private static final Pattern ALPHANUMERIC = Pattern.compile("[a-zA-Z0-9]+");
public boolean isValid(String input) {
    return ALPHANUMERIC.matcher(input).matches();
}
```

#### Item 7: 다 쓴 객체 참조를 해제하라
- 자기 메모리를 직접 관리하는 클래스(캐시, 콜백, 리스너 등)에서는 메모리 누수에 주의한다.
- WebFlux 환경에서 `Disposable` 반환 구독은 적절히 해제한다.

#### Item 8: finalizer와 cleaner 사용을 피하라
- `finalize()`는 절대 사용하지 않는다 (Java 21에서는 deprecated for removal).
- 자원 해제는 `try-with-resources`를 사용한다.

#### Item 9: try-finally보다는 try-with-resources를 사용하라
- `AutoCloseable`을 구현하는 모든 자원은 반드시 `try-with-resources`로 처리한다.

```java
// GOOD - 본 프로젝트 JasyptConfig처럼 try-with-resources 사용
try (FileInputStream fis = new FileInputStream("zipsa-keystore.p12")) {
    keyStore.load(fis, keystorePassword.toCharArray());
}
```

---

### Chapter 3. 모든 객체의 공통 메서드 (Methods Common to All Objects)

#### Item 10: equals는 일반 규약을 지켜 재정의하라
- `equals`를 재정의할 때는 반사성, 대칭성, 추이성, 일관성, null-아님 규약을 모두 만족시켜야 한다.
- 값 객체(VO)에서만 재정의하고, 엔티티에서는 신중히 판단한다.
- Lombok `@EqualsAndHashCode`를 사용할 때는 포함/제외 필드를 명확히 지정한다.
- Java 16+의 `record`를 사용하면 `equals`, `hashCode`, `toString`이 자동 생성되므로 DTO에 적극 활용한다.

```java
// GOOD - record로 equals/hashCode 자동 생성 (본 프로젝트 ChatRequest처럼)
public record ChatRequest(String message) {}

// GOOD - VO에서 Lombok 활용
@Value
public class Money {
    BigDecimal amount;
    Currency currency;
}
```

#### Item 11: equals를 재정의하려거든 hashCode도 재정의하라
- `equals`를 재정의하면 반드시 `hashCode`도 함께 재정의한다.
- Lombok의 `@EqualsAndHashCode`, `@Value`, `@Data` 또는 `record`를 사용하면 자동으로 보장된다.

#### Item 12: toString을 항상 재정의하라
- 디버깅과 로깅을 위해 주요 정보를 담은 `toString`을 재정의한다.
- Lombok `@ToString`을 활용하되, 민감 정보(비밀번호, API 키 등)는 `@ToString.Exclude`로 제외한다.

```java
@ToString
public class ApiConfig {
    private String baseUrl;
    @ToString.Exclude
    private String apiKey;  // 로그에 노출 방지
}
```

#### Item 13: clone 재정의는 주의해서 진행하라
- `Cloneable` / `clone()`은 사용하지 않는다.
- 복사가 필요하면 **복사 생성자** 또는 **복사 팩터리 메서드**를 사용한다.

```java
// BAD
public SearchCriteria clone() { ... }

// GOOD
public static SearchCriteria copyOf(SearchCriteria original) { ... }
```

#### Item 14: Comparable을 구현할지 고려하라
- 자연적 순서가 있는 값 클래스는 `Comparable`을 구현한다.
- `compareTo` 구현 시 `Comparator.comparing()` 체이닝을 활용한다.

```java
// GOOD - Comparator 빌더 활용
public int compareTo(TradeRecord other) {
    return Comparator.comparing(TradeRecord::getDealDate)
                     .thenComparing(TradeRecord::getPrice)
                     .compare(this, other);
}
```

---

### Chapter 4. 클래스와 인터페이스 (Classes and Interfaces)

#### Item 15: 클래스와 멤버의 접근 권한을 최소화하라
- 모든 클래스와 멤버는 가능한 한 가장 낮은 접근 수준을 부여한다.
- 패키지-프라이빗(default)을 적극 활용하고, `public`은 API로 노출해야 할 때만 사용한다.
- `public` 클래스의 인스턴스 필드는 절대 `public`으로 선언하지 않는다.

#### Item 16: public 클래스에서는 public 필드가 아닌 접근자 메서드를 사용하라
- Lombok `@Getter`를 활용하되, 가변 객체의 `@Setter`는 최소화한다.
- 불변 객체에는 `@Value` 또는 `record`를 사용하여 setter 자체를 제공하지 않는다.

#### Item 17: 변경 가능성을 최소화하라 (불변 클래스를 선호하라)
- 클래스는 가능한 한 불변으로 설계한다.
- 모든 필드를 `final`로 선언하고, setter를 제공하지 않으며, 가변 객체의 방어적 복사를 수행한다.
- DTO는 `record`를 기본으로 사용한다.
- 불변 컬렉션은 `List.of()`, `Map.of()`, `Set.of()`, `Collections.unmodifiable*`을 사용한다.

```java
// GOOD - 불변 DTO
public record AptTradeResponse(
    String apartmentName,
    String dealDate,
    long dealAmount,
    double area,
    List<String> floors
) {
    // 방어적 복사: 가변 컬렉션을 불변으로 변환
    public AptTradeResponse {
        floors = List.copyOf(floors);
    }
}
```

#### Item 18: 상속보다는 컴포지션을 사용하라
- 코드 재사용을 위해 상속 대신 컴포지션과 위임을 사용한다.
- `extends`는 **IS-A** 관계가 명확할 때만 사용하고, **HAS-A** 관계에는 컴포지션을 사용한다.

```java
// BAD - 상속으로 기능 확장
public class InstrumentedSet<E> extends HashSet<E> { ... }

// GOOD - 컴포지션으로 기능 확장
public class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate;
    private int addCount = 0;
    // delegate 메서드들...
}
```

#### Item 19: 상속을 고려해 설계하고 문서화하라. 그러지 않았으면 상속을 금지하라
- 상속용으로 설계하지 않은 클래스는 `final`로 선언하거나, 생성자를 `private`/`package-private`으로 제한한다.

#### Item 20: 추상 클래스보다는 인터페이스를 우선하라
- 타입을 정의할 때는 인터페이스를 우선 사용한다.
- 인터페이스의 `default` 메서드를 활용하여 골격 구현을 제공할 수 있다.

#### Item 21: 인터페이스는 구현하는 쪽을 생각해 설계하라
- 기존 인터페이스에 `default` 메서드를 추가할 때는 기존 구현체와의 호환성을 면밀히 검토한다.

#### Item 22: 인터페이스는 타입을 정의하는 용도로만 사용하라
- 상수만 모아놓은 인터페이스(상수 인터페이스 패턴)는 절대 사용하지 않는다.
- 상수는 관련 클래스나 enum에 정의한다.

```java
// BAD - 상수 인터페이스
public interface ApiConstants {
    String BASE_URL = "http://www.law.go.kr/DRF";
}

// GOOD - 유틸리티 클래스 또는 enum
public final class ApiConstants {
    private ApiConstants() {}
    public static final String LAW_BASE_URL = "http://www.law.go.kr/DRF";
}

// BETTER - application.yml + @Value 또는 @ConfigurationProperties
@ConfigurationProperties(prefix = "law.api")
public record LawApiProperties(String baseUrl, String authKey) {}
```

#### Item 23: 태그 달린 클래스보다는 클래스 계층구조를 활용하라
- 한 클래스 안에서 타입 필드로 동작을 분기하는 "태그 달린 클래스"는 지양한다.
- 대신 sealed 인터페이스/클래스 + record 패턴을 활용한다 (Java 21).

```java
// GOOD - Java 21 sealed + record 패턴
public sealed interface ApiResult<T> {
    record Success<T>(T data) implements ApiResult<T> {}
    record Failure<T>(String errorCode, String message) implements ApiResult<T> {}
}
```

#### Item 24: 멤버 클래스는 되도록 static으로 만들라
- 비정적 멤버 클래스는 바깥 인스턴스에 대한 숨은 참조를 가지므로 메모리 누수의 원인이 된다.
- 바깥 인스턴스에 접근할 필요가 없다면 반드시 `static`으로 선언한다.

#### Item 25: 톱레벨 클래스는 한 파일에 하나만 담으라
- 하나의 `.java` 파일에는 하나의 톱레벨 클래스만 작성한다.
- 단, `record`를 다른 클래스 내부에 정의하는 것은 허용한다 (본 프로젝트의 `ChatRequest` 패턴).

---

### Chapter 5. 제네릭 (Generics)

#### Item 26: 로 타입은 사용하지 말라
- `List`, `Map` 같은 로 타입(raw type)은 절대 사용하지 않는다.
- 타입 안전성을 위해 반드시 제네릭 타입 매개변수를 명시한다.

```java
// BAD
List items = new ArrayList();

// GOOD
List<String> items = new ArrayList<>();
```

#### Item 27: 비검사 경고를 제거하라
- 모든 비검사(unchecked) 경고를 가능한 한 제거한다.
- 경고를 제거할 수 없다면 타입 안전함을 확인한 뒤 `@SuppressWarnings("unchecked")`를 **가장 좁은 범위**에 적용하고 그 이유를 주석으로 남긴다.

#### Item 28: 배열보다는 리스트를 사용하라
- 배열은 공변(covariant)이고 제네릭은 불공변(invariant)이므로, 타입 안전성을 위해 `List<T>`를 사용한다.

#### Item 29-30: 이왕이면 제네릭 타입/메서드로 만들라
- 공통 로직에 타입 매개변수를 적용하여 재사용성과 타입 안전성을 동시에 확보한다.

#### Item 31: 한정적 와일드카드를 사용해 API 유연성을 높여라
- PECS: **P**roducer → `extends`, **C**onsumer → `super`
- 반환 타입에는 와일드카드를 사용하지 않는다.

```java
// GOOD
public <E extends Comparable<? super E>> Optional<E> max(Collection<? extends E> collection) { ... }
```

#### Item 33: 타입 안전 이종 컨테이너를 고려하라
- 여러 타입을 안전하게 담아야 할 때 `Class<T>`를 키로 사용하는 타입 안전 이종 컨테이너 패턴을 고려한다.

---

### Chapter 6. 열거 타입과 애너테이션 (Enums and Annotations)

#### Item 34: int 상수 대신 열거 타입을 사용하라
- 관련 상수 집합은 반드시 `enum`으로 정의한다.
- 문자열 상수를 매개변수로 전달하는 대신 `enum`을 활용하여 타입 안전성을 확보한다.

```java
// BAD - 문자열 상수로 정렬 옵션 전달
public String searchLawList(String sort) {
    // "lasc", "ldesc", "efasc"... 오타 위험
}

// GOOD - enum으로 타입 안전성 확보
public enum LawSortOrder {
    LAW_NAME_ASC("lasc", "법령명 오름차순"),
    LAW_NAME_DESC("ldesc", "법령명 내림차순"),
    EFFECT_DATE_ASC("efasc", "시행일자 오름차순"),
    EFFECT_DATE_DESC("efdesc", "시행일자 내림차순"),
    ANNOUNCE_DATE_ASC("anasc", "공포일자 오름차순"),
    ANNOUNCE_DATE_DESC("andesc", "공포일자 내림차순");

    private final String code;
    private final String description;

    LawSortOrder(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
}
```

#### Item 35: ordinal 메서드 대신 인스턴스 필드를 사용하라
- `enum.ordinal()`은 절대 사용하지 않는다. 순서 변경 시 버그 유발.
- 필요한 값은 인스턴스 필드로 저장한다.

#### Item 36: 비트 필드 대신 EnumSet을 사용하라
- 비트 연산 기반 플래그 대신 `EnumSet`을 사용하여 가독성과 타입 안전성을 확보한다.

#### Item 37: ordinal 인덱싱 대신 EnumMap을 사용하라
- 열거 타입을 키로 사용하는 맵에는 `EnumMap`을 사용한다.

#### Item 38: 확장할 수 있는 열거 타입이 필요하면 인터페이스를 사용하라
- 열거 타입은 확장할 수 없으므로, 확장이 필요한 경우 인터페이스를 구현하게 한다.

#### Item 39-41: 애너테이션 관련
- 명명 패턴보다 애너테이션을 사용한다.
- `@Override`는 상위 타입 메서드를 재정의하는 모든 메서드에 반드시 붙인다.
- 정의하려는 것이 타입이라면 마커 인터페이스를 사용하고, 그 외에는 마커 애너테이션을 사용한다.

---

### Chapter 7. 람다와 스트림 (Lambdas and Streams)

#### Item 42: 익명 클래스보다는 람다를 사용하라
- 함수형 인터페이스의 구현에는 람다를 사용한다.
- 단, `this` 키워드가 필요하거나, 직렬화가 필요한 경우는 예외로 한다.

#### Item 43: 람다보다는 메서드 참조를 사용하라
- 람다가 단순히 메서드를 호출하는 것이라면 메서드 참조를 사용한다.
- 단, 메서드 참조가 오히려 읽기 어려운 경우에는 람다를 유지한다.

```java
// GOOD - 메서드 참조
items.stream().map(Item::getName).collect(Collectors.toList());

// 단, 이런 경우는 람다가 더 읽기 쉬움
items.stream().map(item -> "Item: " + item.getName()).toList();
```

#### Item 44: 표준 함수형 인터페이스를 사용하라
- 직접 함수형 인터페이스를 정의하기보다 `java.util.function` 패키지의 표준 인터페이스를 사용한다.
- 주요: `Function<T,R>`, `Predicate<T>`, `Supplier<T>`, `Consumer<T>`, `UnaryOperator<T>`, `BinaryOperator<T>`

#### Item 45: 스트림은 주의해서 사용하라
- 스트림이 무조건 좋은 것은 아니다. 간단한 반복에는 for-each가 더 적합할 수 있다.
- 스트림 파이프라인이 길어지면 중간 변수나 헬퍼 메서드로 분리한다.
- 스트림 안에서 부작용(side-effect)을 발생시키지 않는다.

#### Item 46: 스트림에서는 부작용 없는 함수를 사용하라
- `forEach`는 스트림 계산 결과를 보고할 때만 사용하고, 계산 자체에는 사용하지 않는다.
- `Collectors`의 `toList()`, `toMap()`, `groupingBy()`, `joining()` 등을 적극 활용한다.

```java
// BAD - forEach로 부작용 발생
Map<String, Long> freq = new HashMap<>();
words.forEach(word -> freq.merge(word, 1L, Long::sum));

// GOOD - Collector 활용
Map<String, Long> freq = words.stream()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
```

#### Item 47: 반환 타입으로는 스트림보다 컬렉션이 낫다
- 원소 시퀀스를 반환할 때, 컬렉션을 반환할 수 있으면 컬렉션을 반환한다.
- 단, WebFlux 환경에서는 `Flux<T>`, `Mono<T>` 반환이 자연스러우므로 Reactive 타입을 우선한다.

#### Item 48: 스트림 병렬화는 주의해서 적용하라
- `parallel()`은 성능 측정 없이 사용하지 않는다.
- 데이터 소스가 `ArrayList`, 배열, `IntRange` 등 분할이 쉬운 경우에만 효과적이다.
- WebFlux 환경에서는 Reactor의 `Schedulers`를 활용한 비동기 처리를 우선한다.

---

### Chapter 8. 메서드 (Methods)

#### Item 49: 매개변수가 유효한지 검사하라
- public/protected 메서드는 매개변수 유효성을 즉시 검사한다.
- `Objects.requireNonNull()`, `checkArgument()` 등을 활용한다.
- Spring의 `Assert` 유틸리티도 적극 활용한다.

```java
// GOOD
public String searchLawList(String query) {
    Objects.requireNonNull(query, "검색 쿼리는 null일 수 없습니다");
    // ...
}
```

#### Item 50: 적시에 방어적 복사본을 만들라
- 가변 객체를 받거나 반환할 때는 방어적 복사를 수행한다.
- `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()`를 활용한다.
- `record`의 compact constructor에서 방어적 복사를 수행한다.

#### Item 51: 메서드 시그니처를 신중히 설계하라
- 메서드 이름은 표준 명명 규칙을 따른다.
- 편의 메서드를 너무 많이 만들지 않는다.
- 매개변수 타입으로는 클래스보다 인터페이스를 사용한다 (`ArrayList` → `List`).
- boolean 매개변수보다는 원소 2개짜리 enum을 사용한다.

#### Item 52: 다중정의는 신중히 사용하라
- 오버로딩(다중정의)은 혼란을 줄 수 있으므로, 매개변수 수가 같은 다중정의는 피한다.
- 대신 메서드 이름을 다르게 짓는 것을 고려한다.

#### Item 54: null이 아닌, 빈 컬렉션이나 배열을 반환하라
- 컬렉션이나 배열을 반환하는 메서드에서 `null`을 반환하지 않는다.
- 빈 컬렉션(`List.of()`, `Collections.emptyList()`)이나 빈 배열을 반환한다.
- WebFlux에서는 `Mono.empty()`, `Flux.empty()`를 사용한다.

```java
// BAD
public List<Trade> findTrades() {
    if (noResult) return null;
}

// GOOD
public List<Trade> findTrades() {
    if (noResult) return List.of();
}

// GOOD - WebFlux
public Flux<Trade> findTrades() {
    return Flux.empty();  // 빈 스트림 반환
}
```

#### Item 55: 옵셔널 반환은 신중히 하라
- 반환값이 없을 수 있고, 클라이언트가 이를 명시적으로 처리해야 할 때 `Optional<T>`를 사용한다.
- 컬렉션, 스트림, 배열, Optional을 `Optional`로 감싸지 않는다.
- `Optional`을 필드, 매개변수, 맵의 값으로 사용하지 않는다.
- `Optional.get()` 직접 호출을 지양하고, `orElse`, `orElseThrow`, `orElseGet`, `map`, `flatMap`을 사용한다.
- 기본 타입에는 `OptionalInt`, `OptionalLong`, `OptionalDouble`을 사용한다.

```java
// BAD
public Optional<List<Trade>> findTrades() { ... }  // 컬렉션을 Optional로 감쌈

// BAD
Optional<Trade> trade = findTrade();
String name = trade.get();  // NoSuchElementException 위험

// GOOD
public Optional<Trade> findTradeById(String id) {
    return repository.findById(id);
}

// GOOD - Optional 활용
String name = findTradeById(id)
    .map(Trade::getName)
    .orElse("알 수 없음");

String name = findTradeById(id)
    .map(Trade::getName)
    .orElseThrow(() -> new TradeNotFoundException(id));
```

#### Item 56: 공개된 API 요소에는 항상 문서화 주석을 작성하라
- `public` / `protected` 메서드에는 Javadoc을 작성한다.
- `@param`, `@return`, `@throws` 태그를 활용한다.
- `@Tool`, `@ToolParam` 어노테이션의 `description`을 Javadoc 대용으로 활용할 수 있다 (본 프로젝트 패턴).

---

### Chapter 9. 일반적인 프로그래밍 원칙 (General Programming)

#### Item 57: 지역변수의 범위를 최소화하라
- 지역변수는 가장 처음 사용되는 시점에 선언한다.
- 거의 모든 지역변수는 선언과 동시에 초기화한다.
- for 루프가 while 루프보다 범위 최소화에 유리하다.

#### Item 58: 전통적인 for 문보다는 for-each 문을 사용하라
- 인덱스가 필요하지 않다면 향상된 for 문(for-each)을 사용한다.

#### Item 59: 라이브러리를 익히고 사용하라
- 직접 구현하기보다 표준 라이브러리(java.util, java.util.concurrent 등)를 우선 사용한다.
- Spring Framework가 제공하는 유틸리티(StringUtils, CollectionUtils, UriComponentsBuilder 등)를 활용한다.

#### Item 60: 정확한 답이 필요하다면 float와 double은 피하라
- 금융 계산(부동산 매매가 등)에는 `BigDecimal`, `int`, `long`을 사용한다.
- `float`/`double`은 근사치 계산에만 사용한다.

```java
// BAD - 부동산 매매가에 double 사용
double dealAmount = 125000.0;

// GOOD - BigDecimal 또는 정수(만원 단위)
BigDecimal dealAmount = new BigDecimal("125000");
long dealAmountInTenThousandWon = 12_5000L;
```

#### Item 61: 박싱된 기본 타입보다는 기본 타입을 사용하라
- `int` > `Integer`, `long` > `Long`, `boolean` > `Boolean`
- 오토박싱/언박싱의 성능 비용에 유의한다.
- 단, 제네릭 타입 매개변수와 컬렉션에서는 박싱 타입을 사용할 수밖에 없다.

#### Item 62: 다른 타입이 적절하다면 문자열 사용을 피하라
- 열거 타입, 혼합 타입, 권한(capability)을 문자열로 표현하지 않는다.
- 문자열 대신 적절한 값 타입(`enum`, `record`, 전용 클래스)을 사용한다.

#### Item 63: 문자열 연결은 느리니 주의하라
- 반복적인 문자열 연결에는 `StringBuilder`를 사용한다.
- 단, 컴파일러 최적화가 적용되는 단순 연결(`+`)이나 `String.format` / text block은 괜찮다.
- Java 21의 `STR` 템플릿 프로세서를 활용할 수 있다.

#### Item 64: 객체는 인터페이스를 사용해 참조하라
- 적합한 인터페이스가 있다면 매개변수, 반환값, 변수, 필드를 모두 인터페이스 타입으로 선언한다.

```java
// BAD
LinkedHashMap<String, String> params = new LinkedHashMap<>();
ArrayList<Trade> trades = new ArrayList<>();

// GOOD
Map<String, String> params = new LinkedHashMap<>();
List<Trade> trades = new ArrayList<>();
```

#### Item 65: 리플렉션보다는 인터페이스를 사용하라
- 리플렉션은 프레임워크 수준에서만 사용하고, 일반 애플리케이션 코드에서는 피한다.
- Spring의 DI 컨테이너에 의존하여 인터페이스 기반 설계를 활용한다.

#### Item 67: 최적화는 신중히 하라
- "빠른 프로그램보다 좋은 프로그램을 작성하라."
- 성능 최적화는 프로파일링으로 병목을 확인한 후에만 수행한다.
- API 설계 시 성능에 영향을 주는 결정(불변 vs 가변, 컴포지션 vs 상속)은 신중히 한다.

#### Item 68: 일반적으로 통용되는 명명 규칙을 따르라

| 구분 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자, 도메인 역순 | `com.example.zipsa.tool` |
| 클래스/인터페이스 | PascalCase | `LawServiceTool`, `ChatRequest` |
| 메서드/필드 | camelCase | `searchLawList`, `callApi` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 타입 매개변수 | 단일 대문자 | `T`, `E`, `K`, `V`, `R` |
| boolean 반환 메서드 | is/has/can/should 접두사 | `isEmpty()`, `hasNext()` |
| 변환 메서드 | to 접두사 | `toString()`, `toList()` |
| 팩터리 메서드 | from/of/valueOf | `List.of()`, `Money.from()` |

---

### Chapter 10. 예외 (Exceptions)

#### Item 69: 예외는 진짜 예외 상황에만 사용하라
- 흐름 제어에 예외를 사용하지 않는다.
- 상태 검사 메서드(`isEmpty()`, `hasNext()`)나 `Optional`로 예외를 예방한다.

#### Item 70: 복구할 수 있는 상황에는 검사 예외를, 프로그래밍 오류에는 런타임 예외를 사용하라
- 호출자가 복구할 수 있는 상황: checked exception (`IOException` 등)
- 프로그래밍 오류: unchecked exception (`IllegalArgumentException`, `IllegalStateException` 등)
- 본 프로젝트에서 외부 API 호출 실패는 런타임 예외로 처리하되, 적절한 폴백(fallback)을 제공한다.

#### Item 71: 필요 없는 검사 예외 사용은 피하라
- checked exception은 호출자가 복구 조치를 취할 수 있을 때만 사용한다.
- 그렇지 않으면 unchecked exception을 사용한다.

#### Item 72: 표준 예외를 사용하라

| 예외 | 사용 상황 |
|------|----------|
| `IllegalArgumentException` | 매개변수 값이 부적절할 때 |
| `IllegalStateException` | 객체 상태가 메서드 호출에 적합하지 않을 때 |
| `NullPointerException` | null이 허용되지 않는 매개변수에 null이 전달될 때 |
| `IndexOutOfBoundsException` | 인덱스가 범위를 벗어날 때 |
| `UnsupportedOperationException` | 호출한 메서드를 지원하지 않을 때 |
| `ConcurrentModificationException` | 동시 수정이 금지된 곳에서 감지될 때 |

#### Item 73: 추상화 수준에 맞는 예외를 던지라
- 저수준 예외를 그대로 전파하지 말고, 현재 추상화 수준에 맞는 예외로 변환하여 던진다.
- 원인 예외(cause)를 보존하여 디버깅을 용이하게 한다.

```java
// GOOD - 추상화 수준에 맞는 예외 변환
public String searchLawList(String query) {
    try {
        return callApi("법령검색", uri);
    } catch (RestClientException e) {
        throw new LawApiException("법령 목록 조회 실패: " + query, e);
    }
}
```

#### Item 74: 각 메서드가 던지는 모든 예외를 문서화하라
- checked exception은 `@throws` Javadoc 태그로 문서화한다.
- unchecked exception도 가능한 한 문서화하여 호출자가 대비할 수 있게 한다.

#### Item 75: 상세 메시지에 실패 관련 정보를 담으라
- 예외 메시지에 실패를 일으킨 매개변수 값과 관련 필드 값을 포함한다.
- 보안에 민감한 정보(비밀번호, API 키)는 절대 예외 메시지에 포함하지 않는다.

```java
// BAD
throw new IllegalArgumentException("잘못된 지역코드");

// GOOD
throw new IllegalArgumentException(
    "잘못된 지역코드입니다. lawdCd=%s (5자리 숫자여야 합니다)".formatted(lawdCd));
```

#### Item 76: 실패 원자성을 위해 노력하라
- 메서드가 실패하더라도 객체는 메서드 호출 전 상태를 유지해야 한다.
- 불변 객체를 사용하면 자연스럽게 실패 원자성이 보장된다.
- 가변 객체라면 작업 수행 전에 매개변수 유효성을 먼저 검사한다.

#### Item 77: 예외를 무시하지 말라
- `catch` 블록을 비워두지 않는다.
- 예외를 무시해야 하는 경우 그 이유를 주석으로 남기고, 변수명을 `ignored`로 짓는다.

```java
// BAD
try { ... } catch (Exception e) { }

// GOOD - 무시 사유를 명시
try { ... } catch (TimeoutException ignored) {
    // 타임아웃은 정상 흐름. 빈 결과 반환으로 처리.
}
```

---

### Chapter 11. 동시성 (Concurrency)

#### Item 78: 공유 중인 가변 데이터는 동기화해서 사용하라
- 여러 스레드가 가변 데이터를 공유한다면 반드시 동기화한다.
- `volatile`, `AtomicReference`, `ConcurrentHashMap` 등을 활용한다.
- WebFlux 환경에서는 가변 공유 상태를 최소화하고, Reactor의 연산자를 통해 데이터를 처리한다.

#### Item 79: 과도한 동기화는 피하라
- 동기화 블록 안에서 외부 메서드를 호출하지 않는다 (교착 상태 위험).
- 동기화 영역은 최소한으로 줄인다.

#### Item 80: 스레드보다는 실행자, 태스크, 스트림을 애용하라
- 직접 `Thread`를 생성하지 말고 `ExecutorService`를 사용한다.
- Java 21의 Virtual Thread를 활용할 수 있다.
- WebFlux 환경에서는 `Schedulers.boundedElastic()`, `Schedulers.parallel()` 등 Reactor Scheduler를 사용한다.

```java
// BAD
new Thread(() -> heavyWork()).start();

// GOOD - Virtual Thread (Java 21)
Thread.startVirtualThread(() -> heavyWork());

// GOOD - WebFlux 환경
Mono.fromCallable(() -> heavyWork())
    .subscribeOn(Schedulers.boundedElastic());
```

#### Item 81: wait와 notify보다는 동시성 유틸리티를 애용하라
- `wait`/`notify` 대신 `java.util.concurrent`의 고수준 유틸리티를 사용한다.
- `CountDownLatch`, `Semaphore`, `CompletableFuture` 등을 활용한다.
- WebFlux에서는 Reactor의 `Mono.zip()`, `Flux.merge()` 등으로 동시성을 제어한다.

#### Item 83: 지연 초기화는 신중히 사용하라
- 대부분의 경우 일반적인 초기화가 지연 초기화보다 낫다.
- 성능 문제가 확인된 경우에만 사용하되, 멀티스레드 환경에서는 `holder` 클래스 관용구를 사용한다.

#### Item 84: 스레드 스케줄러에 기대지 말라
- `Thread.yield()`나 스레드 우선순위에 의존하지 않는다.
- 프로그램의 정확성이 스레드 스케줄링에 의존하면 안 된다.

---

### Chapter 12. 직렬화 (Serialization)

#### Item 85-90: 직렬화 관련 주의사항
- Java 직렬화(`Serializable`)는 가능한 한 사용하지 않는다.
- 크로스 플랫폼 데이터 교환에는 **JSON**을 사용한다 (본 프로젝트에서 이미 적용 중).
- `Serializable`을 구현해야 한다면 `serialVersionUID`를 명시적으로 선언한다.
- 역직렬화 시 신뢰할 수 없는 데이터의 보안 위험에 주의한다.

---

### 프로젝트 특화 규칙 (본 프로젝트에 맞는 추가 권장사항)

#### WebFlux / Reactive 프로그래밍
- 블로킹 호출(`Thread.sleep()`, 동기 I/O)을 Reactor 파이프라인 안에서 사용하지 않는다.
- 블로킹이 불가피하면 `Schedulers.boundedElastic()`으로 감싼다.
- `Mono`/`Flux`를 구독(`.subscribe()`)하지 말고, 체이닝으로 처리한다. 구독은 프레임워크에 맡긴다.
- `onErrorResume`, `onErrorReturn`, `retry` 등 Reactor 에러 처리 연산자를 적절히 활용한다.

#### Spring AI Tool 작성 시
- `@Tool`의 `description`은 LLM이 이해할 수 있도록 명확하고 구체적으로 작성한다.
- `@ToolParam`의 `description`에 유효한 값의 예시와 형식을 포함한다.
- `required = false`인 파라미터는 `null` 및 빈 문자열 모두 처리한다.
- 외부 API 호출 실패 시 에러 메시지를 문자열로 반환하여 LLM이 사용자에게 안내할 수 있도록 한다.

#### DTO / 데이터 전달 객체
- 불변 DTO에는 `record`를 기본으로 사용한다.
- 가변 상태가 필요한 경우에만 Lombok `@Builder` + `@Value`를 사용한다.
- API 응답 DTO에는 `record` + Jackson 어노테이션을 조합한다.

#### 설정 관리
- 외부 설정값은 `application.yml` + `@ConfigurationProperties`로 관리한다.
- 민감 정보는 Jasypt 암호화를 유지한다.
- 하드코딩된 URL, 키, 매직 넘버는 반드시 설정 파일로 추출한다.