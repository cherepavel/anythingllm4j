# AnythingLLM4Java

Java SDK для REST API AnythingLLM на Retrofit 3. Требуется Java 17+.
Контракт взят из `.data/api.odt`: реализованы все 63 операции документа,
включая admin, documents, workspaces, system, threads, users, OpenAI,
embed и transcription. Совместимость с работающим сервером зависит от его версии;
есть тесты на MockWebServer и опциональные интеграционные тесты реального сервера.

## Сборка

```sh
mvn verify
```

Maven coordinates проекта (публикация в Maven Central не настроена):

```xml
<dependency>
  <groupId>io.github.cherepavel</groupId>
  <artifactId>anythingllm4j</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Для использования в другом локальном проекте сначала выполните `mvn install`.

## Архитектура

Зависимости направлены к прикладным контрактам:

```text
AnythingLlm (composition root)
  ├─ WorkspaceService / ThreadService → application.port.AnythingLlmGateway
  └─ RetrofitAnythingLlmGateway ────── implements ────────────────┘
       ├─ RequestMapper / ResponseMapper
       └─ Retrofit / OkHttp / Gson
```

| Пакет | Ответственность |
| --- | --- |
| `application.api` | Входные контракты `Workspaces`, `Threads` |
| `application.dto` | Неизменяемые запросы и результаты на типах JDK |
| `application.exception` | Проверяемые ошибки прикладной границы |
| `application.port` | Порт внешнего сервиса, необходимый сценариям |
| `application.service` | Проверка аргументов, точный поиск и правила выбора workspace |
| `infrastructure.retrofit` | HTTP, авторизация, ресурсы, JSON, преобразование DTO |
| Корневой пакет | `AnythingLlm` собирает граф объектов; прежний API сохраняет совместимость |

`application` не импортирует инфраструктуру, Gson, Retrofit или OkHttp.
Сервисы принимают выходной порт через конструктор и тестируются без сети.
Отдельного domain-слоя нет: у этого SDK нет самостоятельной предметной модели,
которой понадобился бы такой слой. Зависимости скомпилированных классов проверяет
`OnionArchitectureTest` через JDK `jdeps`.

Новые синхронные DTO находятся в `io.github.cherepavel.application.dto`, ошибки —
в `io.github.cherepavel.application.exception`. Корневые `ChatRequest` и
`ChatResponse` сохраняют прежний сериализуемый контракт низкоуровневого API;
синхронный клиент использует отдельный `application.dto.ChatRequest` без Gson.
Корневые `AnythingLlmApi` и `AnythingLlmClient` также сохранены для совместимости;
реализация фабрики транспорта вынесена в инфраструктуру.

## Синхронный клиент

Прикладному коду достаточно типов SDK, `URI` и `Duration`:

```java
import io.github.cherepavel.AnythingLlm;
import io.github.cherepavel.application.dto.ChatRequest;
import java.net.URI;
import java.time.Duration;

try (var client = AnythingLlm.builder()
        .serverUri(URI.create("http://127.0.0.1:3001"))
        .apiKey(System.getenv("ANYTHINGLLM_API_KEY"))
        .connectTimeout(Duration.ofSeconds(10))
        .requestTimeout(Duration.ofMinutes(10))
        .build()) {
    var workspace = client.workspaces().findByName("СанПиН — вода");
    var thread = client.threads().create(workspace.slug(), "Проверка анализа");
    var response = client.threads().chat(workspace.slug(), thread.slug(),
            new ChatRequest("Найди норматив для фторидов", ChatRequest.Mode.QUERY));
    System.out.println(response.textResponse());
}
```

`serverUri` задаёт HTTP(S)-адрес **без `/api/`**. Допускается префикс reverse proxy,
например `https://example.org/anythingllm`: SDK добавляет `/api/`, затем пути `v1/`.
Credentials, query и fragment запрещены. Ключ передаётся без `Bearer`, пробелов
и управляющих символов. Настройки проверяются в `build()`; допустимые таймауты —
от 1 мс до 2147483647 мс. По умолчанию connect timeout — 10 секунд,
request timeout — 5 минут. Request timeout ограничивает весь HTTP-вызов, включая
чтение тела, а не только ожидание очередного фрагмента ответа.

Доступны `workspaces().list()`, `workspaces().get(slug)`,
`workspaces().findByName(name)`, `workspaces().chat(slug, request)`,
`threads().create(workspaceSlug, name)` и
`threads().chat(workspaceSlug, threadSlug, request)`.
Поиск имени учитывает регистр и не выбирает первое совпадение при дубликатах.
Результаты — `Workspace`, `ThreadInfo`, `ChatReply` и списки DTO.
`ChatReply.sources()` содержит `ChatSource` с title, text, id, documentId,
chunkSource и url. Все исходные поля ответа, включая дополнительные идентификаторы,
ссылки и metadata, доступны через `fields()` как глубоко неизменяемые
`Map<String, Object>` / `List<?>` со строками, boolean, null и числами `BigDecimal`.
Отсутствующие или null sources превращаются в пустой список.

Все операции объявляют проверяемую `AnythingLlmException`:

| Исключение | Причина |
| --- | --- |
| `TransportException` | Соединение, таймаут, ошибка чтения; исходная причина в `getCause()` |
| `HttpException` | Неуспешный HTTP-код; `statusCode()` и безопасное имя `operation()` |
| `ApiException` | Заполненный `error`, `type=abort` или `success=false` при успешном HTTP |
| `InvalidResponseException` | Некорректный JSON, пустое тело, отсутствующие или неверные поля |
| `WorkspaceNotFoundException` | Нет точного совпадения имени или пустой результат получения по slug |
| `AmbiguousWorkspaceException` | Несколько точных совпадений имени |

SDK не включает серверные сообщения ошибок, авторизацию и пользовательские запросы
в собственные сообщения исключений. HTTP 404 остаётся `HttpException`.
Текст ответа чата обязателен и не должен быть пустым. Транспортные ошибки и ошибки
разбора сохраняют причину. Клиент закрывает тела ответов самостоятельно.

Один клиент переиспользуется для нескольких операций. Он владеет только своим
созданным внутри транспортом: `close()` отменяет активные вызовы, освобождает pool
соединений и executor. Закрывайте клиент после окончания работы всех вызывающих
потоков. После закрытия операции отклоняются с `IllegalStateException`.
Синхронный клиент не принимает внешний транспорт и не затрагивает ресурсы
отдельного низкоуровневого клиента. Автоматические повторы POST и перенаправления
отключены, включая повторы после `503 Retry-After: 0`.
Потоковые операции остаются отдельным низкоуровневым контрактом; SDK не собирает
SSE в `ChatReply`.

## Низкоуровневое подключение и чат

Передавайте URL **до `/api/` включительно**; завершающий слеш необязателен.
Ключ передаётся без префикса `Bearer`. Клиент добавляет его в заголовок
`Authorization` каждого запроса. Перенаправления отключены.

```java
import io.github.cherepavel.AnythingLlmApi;
import io.github.cherepavel.AnythingLlmClient;
import io.github.cherepavel.ChatRequest;
import io.github.cherepavel.ChatResponse;
import retrofit2.Response;

AnythingLlmApi api = AnythingLlmClient.create(
    "http://localhost:3001/api/", System.getenv("ANYTHINGLLM_API_KEY"));
Response<ChatResponse> response = api.chat("my-workspace",
    new ChatRequest("Что содержат документы?", ChatRequest.Mode.QUERY)).execute();
if (response.isSuccessful() && response.body() != null) {
    // Помимо HTTP-кода проверяйте type/error: сервер может вернуть abort с HTTP 200.
    System.out.println(response.body().textResponse());
} else {
    try (var error = response.errorBody()) {
        throw new java.io.IOException("HTTP " + response.code() + ": "
            + (error == null ? "" : error.string()));
    }
}
```

Все методы низкоуровневого `AnythingLlmApi` возвращают Retrofit `Call<T>`: доступны `execute()`, `enqueue()`
и `cancel()`. Вызов `execute()` может выбросить `IOException`. Один `Call`
исполняется один раз; для повторного запроса создайте новый или вызовите `clone()`.
Сервис можно переиспользовать. По умолчанию read timeout составляет 5 минут;
для собственных настроек передайте `OkHttpClient` третьим аргументом фабрики.

## JSON и загрузка документов

Чат использует `ChatRequest` / `ChatResponse`; остальные JSON payload и ответы —
`JsonElement`, чтобы сохранять поля, зависящие от настроек провайдера.
Null-поля запросов пропускаются; для явного JSON null используйте `JsonNull`.

```java
var settings = com.google.gson.JsonParser.parseString("{\"name\":\"My workspace\"}");
var workspace = api.createWorkspace(settings).execute();

var file = okhttp3.MultipartBody.Part.createFormData("file", "notes.txt",
    okhttp3.RequestBody.create(okhttp3.MediaType.get("text/plain"),
        new java.io.File("notes.txt")));
// Необязательные multipart-поля допускают null.
var uploaded = api.uploadDocument(file, null, null).execute();

var documents = api.documents(java.util.Map.of(
    "folder", "custom-documents", "offset", "0", "limit", "all")).execute();
```

`metadata` передавайте как `RequestBody` с `application/json`,
`addToWorkspaces` — как `RequestBody` с `text/plain` и перечнем slug через запятую.
Для transcription имя файловой части — `audio`, для генерации изображений —
`image_references`. Значения `@Path` передавайте без предварительного URL-кодирования.

## Потоковые ответы и файлы

`streamChat`, `streamThreadChat`, `chatCompletions`, `generateImages`,
`generatedFile`, `exportChats` и `dumpEnvironment` возвращают `ResponseBody`.
Закрывайте его после использования. Для OpenAI chat можно передать `stream: true`
или `false`: возвращаемый body позволяет прочитать оба формата.

```java
var response = api.streamChat("my-workspace",
    new ChatRequest("Привет", ChatRequest.Mode.CHAT)).execute();
if (response.isSuccessful() && response.body() != null) {
    try (var body = response.body()) {
        var source = body.source();
        String line;
        while ((line = source.readUtf8Line()) != null) {
            System.out.println(line);
        }
    }
} else {
    try (var error = response.errorBody()) {
        throw new java.io.IOException("HTTP " + response.code());
    }
}
```

Пример показывает чтение строк SSE. Для обработки событий собирайте строки
`data:` до пустой строки; JSON события можно десериализовать в `ChatResponse`.
SDK не объединяет SSE-события и не выполняет автоматические повторы запросов
на уровне приложения. Административные методы требуют соответствующих прав,
а часть методов — включённого multi-user mode на сервере.

## Интеграционные тесты

Без `ANYTHINGLLM_API_KEY` интеграционные тесты пропускаются. Для запуска:

```sh
export ANYTHINGLLM_API_URL=http://localhost:3001/api/
read -s ANYTHINGLLM_API_KEY
export ANYTHINGLLM_API_KEY
mvn verify
unset ANYTHINGLLM_API_KEY
```

После команды `read` введите ключ и нажмите Enter. Ключ не сохраняется в проекте.
Проверяются авторизация, списки документов и рабочих пространств, схемы metadata,
режим multi-user, OpenAI models/vector stores и жизненный цикл отдельного workspace
с веткой чата. Тест создаёт workspace с префиксом `sdk-integration-` и удаляет его
в `finally`. При аварийном завершении процесса может потребоваться ручная очистка.
Тесты не вызывают LLM, генерацию изображений и транскрибацию и не проверяют все
63 операции на сервере. Удаление workspace/thread возвращает `Call<Void>`:
успех проверяйте по HTTP-коду, тело ответа не десериализуется.
