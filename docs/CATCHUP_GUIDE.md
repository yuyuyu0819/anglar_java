# キャッチアップ学習ガイド

このサンプルを使って Angular + Java（Spring Boot）案件に入るための手引きです。
**上から順に読む必要はありません。** まず「1. まず最初にやること」だけ実施して、
あとは必要なところを辞書的に引いてください。

---

## 目次

1. [まず最初にやること](#1-まず最初にやること)
2. [1リクエストを端から端まで追う](#2-1リクエストを端から端まで追う)
3. [Spring Boot 編](#3-spring-boot-編)
4. [Angular 編](#4-angular-編)
5. [官公庁案件で特に問われる観点](#5-官公庁案件で特に問われる観点)
6. [演習課題](#6-演習課題)
7. [つまずきポイント集](#7-つまずきポイント集)
8. [実案件が別構成だった場合の読み替え](#8-実案件が別構成だった場合の読み替え)

---

## 1. まず最初にやること

### ステップ1: 動かす（15分）

README の手順どおりに起動します。初回はこの3つです。

```powershell
cd C:\develop\angular_java
.\start-database.ps1         # PostgreSQL (Docker) を起動
.\start-backend.ps1 -Init    # テーブル作成＋初期データ投入してから起動（別ターミナル）
.\start-frontend.ps1         # Angular 開発サーバ（さらに別ターミナル）
```

ブラウザで http://localhost:4200/ を開き、一通り操作します。

- 一覧でキーワード検索・ステータス絞り込み・ページ送り
- 新規登録 → 詳細画面で「審査中」にする → 「承認」にする
- 承認済みの申請で「編集」ボタンが消えていることを確認
- **2つのブラウザタブで同じ申請の編集画面を開き、両方で保存** → 2つ目が
  「他のユーザーが更新したため…」になる（楽観ロックの体験）

この最後の操作が一番重要です。**業務システムで必ず議論になる排他制御**を
実物で体験しておくと、設計レビューで話についていけます。

### ステップ2: ログを見ながら操作する（15分）

バックエンドのコンソールを見ながら、ブラウザで検索操作をしてください。

```
INFO  [a1b2c3d4] ... GET /api/applications?keyword=山田 -> 200 (23ms)
DEBUG [a1b2c3d4] ... ApplicationMapper.search : ==>  Preparing: SELECT ... WHERE ( a.application_no LIKE ... )
DEBUG [a1b2c3d4] ... ApplicationMapper.search : ==> Parameters: 山田(String), 山田(String), 10(Integer), 0(Integer)
```

- `[a1b2c3d4]` が **リクエストID**。同じIDの行だけ拾えば1リクエスト分の処理を追えます
- `Preparing:` が**実際に発行されたSQL**、`Parameters:` が**バインドされた値**

> **これが実務で最も使うデバッグ手段です。**
> 「検索結果がおかしい」と言われたとき、まずこのログでSQLと値を確認します。
> 設定は `application.yml` の `logging.level.jp.go.sample.backend.domain.repository: TRACE`。

### ステップ3: pgAdmin でDBを覗く（15分）

ログに出たSQLを、pgAdmin で自分で実行してみると理解が一気に進みます。

**接続設定**（pgAdmin で Servers を右クリック → Register → Server）

| タブ | 項目 | 値 |
|---|---|---|
| General | Name | `shinsei (docker)` ※任意の表示名 |
| Connection | Host name/address | `localhost` |
| Connection | Port | `5432` |
| Connection | Maintenance database | `postgres` |
| Connection | Username | `shinsei_app` |
| Connection | Password | `shinsei_pass`（Save password にチェック） |

接続できたら `Databases > shinsei_db > Schemas > public > Tables` を開きます。

**まず見てほしいもの**

| 見る場所 | 何が分かるか |
|---|---|
| Tables > application > Columns | 列の型。`id` が bigint の IDENTITY、`status` が varchar、`applied_on` が date |
| Tables > application > Constraints | 主キー・外部キー・UNIQUE制約。`uk_application_no` が申請番号の重複を防いでいる |
| Tables > application > Indexes | 検索条件の列に張ったインデックス3本 |
| 右クリック > View/Edit Data > All Rows | 実データ。画面に出ている内容と一致する |

**動かしながら確認すると効果的な操作**

```sql
-- 1. 画面の一覧検索と同じSQLを自分で実行してみる
--    （ログの Preparing: に出ていたものを貼り付け、? に値を入れる）
SELECT a.id, a.application_no, a.applicant_name, c.name AS category_name, a.status, a.version
  FROM application a
  INNER JOIN application_category c ON c.code = a.category_code
 WHERE a.status = 'REVIEWING'
 ORDER BY a.applied_on DESC, a.id DESC
 LIMIT 10 OFFSET 0;

-- 2. 楽観ロックの仕組みを目で見る
--    画面で申請を1件更新したあと、version が増えていることを確認する
SELECT id, application_no, version, updated_at FROM application ORDER BY updated_at DESC LIMIT 5;

-- 3. アプリが実際に投げている UPDATE を再現する
--    version が一致しないと「UPDATE 0」になり、アプリは409を返す
UPDATE application
   SET applicant_name = 'pgAdminから更新', version = version + 1, updated_at = now()
 WHERE id = 1 AND version = 999;     -- → UPDATE 0 （競合と判定される）
```

> **pgAdmin で直接 UPDATE した後は、画面を再読み込みすること。**
> 画面が古い `version` を持ったままだと更新が409になります。
> これは不具合ではなく、楽観ロックが正しく働いている証拠です。

コンテナ内の `psql` でも同じことができます（CLIのほうが速い場面も多い）。

```powershell
.\start-database.ps1 -Psql
# \dt          テーブル一覧
# \d application   テーブル定義
# \q           終了
```

### ステップ4: 1本のAPIをコードで追う

次の章で、一覧表示を画面からDBまで順にたどります。

---

## 2. 1リクエストを端から端まで追う

「一覧画面でキーワード『山田』を入れて検索ボタンを押す」ときの流れです。
**この1本を完全に理解すれば、他の機能は同じ型の繰り返しです。**

```
【フロントエンド】
 1. application-list.html
      <form [formGroup]="searchForm" (ngSubmit)="onSearch()">
      → 検索ボタン(type="submit")押下で onSearch() が呼ばれる

 2. application-list.ts : onSearch()
      → applyQueryParams() でURLを書き換える
        （/applications?keyword=山田）

 3. application-list.ts : ngOnInit() 内の route.queryParamMap 購読が発火
      → fetch({ keyword: '山田', page: 0 })

 4. core/services/application.service.ts : search()
      → HttpParams を組み立てて http.get<PageResponse<Application>>()

 5. core/interceptors/loading.interceptor.ts
      → LoadingService.start()（画面に「通信中...」が出る）

 6. core/interceptors/error.interceptor.ts
      → catchError を仕掛けて後続へ

          ─── HTTP GET /api/applications?keyword=山田&page=0&size=10 ───
              （Angular開発サーバの proxy.conf.json が 8080 へ転送）

【バックエンド】
 7. config/RequestLoggingFilter
      → リクエストIDをMDCに入れ、アクセスログを出す

 8. web/controller/ApplicationController#search
      @GetMapping + @Valid @ModelAttribute ApplicationSearchCriteria
      → クエリ文字列が criteria オブジェクトに束縛され、Bean Validation が走る
      → NGなら MethodArgumentNotValidException → GlobalExceptionHandler → 400

 9. service/impl/ApplicationServiceImpl#search
      @Transactional(readOnly = true)
      → まず countBySearchCriteria() で件数、0件ならSQLを1本節約して即return
      → search() で明細取得 → ApplicationResponse.from() でDTOに変換

10. domain/repository/ApplicationMapper#search（インターフェースのみ）
      → MyBatis が resources/mapper/ApplicationMapper.xml の
        <select id="search"> と突き合わせて実装を生成

11. ApplicationMapper.xml
      <where> と <if> による動的SQL組み立て
      → keyword があるときだけ LIKE 条件が付く
      → LIMIT #{criteria.size} OFFSET #{criteria.offset}

12. H2 データベース
      → 結果を resultMap で Application オブジェクトにマッピング

          ─── レスポンス（PageResponse<ApplicationResponse> の JSON） ───

【フロントエンド】
13. error.interceptor → 正常なので何もしない
14. loading.interceptor → finalize で LoadingService.stop()
15. application-list.ts : fetch() の subscribe({ next })
      → this.page.set(result)
16. シグナル page() が変わったので、テンプレートの @for が再描画される
```

### 追いかけるときのコツ

| 探したいもの | 探し方 |
|---|---|
| ボタンを押したら何が起きるか | テンプレート(`.html`)の `(click)` / `(ngSubmit)` を見て、同名メソッドを `.ts` で探す |
| このAPIはどのJavaメソッドか | URLで全文検索 → `@GetMapping("...")` が付いたメソッドが入口 |
| このメソッドが発行するSQL | Mapper インターフェースのメソッド名で `.xml` を検索（`<select id="メソッド名">`） |
| 画面に出ているこの文言はどこ | 文言で全文検索。`.html` / `.ts` / Java の例外メッセージのどれか |
| エラーがどこで4xxに変換されたか | `GlobalExceptionHandler` の `@ExceptionHandler` を上から見る |

---

## 3. Spring Boot 編

### 3-1. DI（依存性の注入）だけは最初に理解する

Spring のすべての土台です。

```java
@Service                       // ← 「このクラスをSpringが管理する部品にする」宣言
@RequiredArgsConstructor       // ← Lombok。finalフィールドを引数に取るコンストラクタを生成
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationMapper applicationMapper;   // ← new しない
}
```

**自分で `new` しない**のがポイントです。
`@Service` / `@RestController` / `@Component` などが付いたクラスは起動時にSpringが
インスタンス化し（= Bean）、コンストラクタ引数に必要なBeanを自動で渡してくれます。

| 書き方 | 評価 |
|---|---|
| コンストラクタインジェクション（本サンプル） | **推奨。** `final` にできる、テストで差し替えやすい |
| `@Autowired` フィールドインジェクション | 古い案件に多い。`final` にできず、テストしづらい |
| `new` する | DIが効かず `@Transactional` も動かない。**やってはいけない** |

> **よくあるエラー**
> `NoSuchBeanDefinitionException` / `required a bean of type '...' that could not be found`
> → 対象クラスに `@Service` 等が付いていない、または
>   `@SpringBootApplication` のパッケージ配下に置かれていない（コンポーネントスキャンの対象外）。

### 3-2. アノテーション早見表

このサンプルで使っているものだけです。これだけ分かれば大半のコードは読めます。

| アノテーション | 意味 | 本サンプルの該当箇所 |
|---|---|---|
| `@SpringBootApplication` | 起動クラス。これより下の階層をスキャンする | `BackendApplication` |
| `@RestController` | REST APIのコントローラ。戻り値が自動でJSONになる | `ApplicationController` |
| `@RequestMapping("/api/...")` | クラス単位の共通URL | 同上 |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping` / `@DeleteMapping` | HTTPメソッドとパスの対応 | 同上 |
| `@PathVariable` | URLの `{id}` を受け取る | 同上 |
| `@RequestBody` | リクエストボディのJSONをオブジェクトに変換 | 同上 |
| `@ModelAttribute` | クエリパラメータをオブジェクトに束縛 | `search()` |
| `@Valid` | Bean Validation を発火させる | 同上 |
| `@Service` | 業務ロジックの部品 | `ApplicationServiceImpl` |
| `@Transactional` | トランザクション境界 | 同上 |
| `@Configuration` | 設定クラス | `WebMvcConfig`, `MyBatisConfig` |
| `@Component` | 汎用の部品 | `RequestLoggingFilter` |
| `@RestControllerAdvice` | 全コントローラ共通の例外処理 | `GlobalExceptionHandler` |
| `@ExceptionHandler(XxxException.class)` | その例外を処理するメソッド | 同上 |
| `@Value("${キー}")` | application.yml の値を注入 | `WebMvcConfig` |
| `@MapperScan` | MyBatis の Mapper を一括登録 | `MyBatisConfig` |
| `@Select` | MyBatis。SQLを直接書く | `ApplicationCategoryMapper` |
| `@Param` | MyBatis。SQL内で参照する引数名を指定 | `ApplicationMapper` |

### 3-3. トランザクション（`@Transactional`）

```java
@Transactional(readOnly = true)   // クラス全体の既定 = 参照のみ
public class ApplicationServiceImpl {

    @Transactional                // 更新系だけ個別に上書き
    public ApplicationResponse create(...) { ... }
}
```

**押さえるべき3点**

1. **トランザクション境界はサービス層に置く。**
   コントローラに置くとHTTPの都合とDBの都合が混ざり、Mapperに置くと
   「2テーブル更新して途中で失敗」のときにロールバックできません。
2. **既定でロールバックされるのは非検査例外（RuntimeException系）のみ。**
   検査例外（`throws` が必要なもの）ではロールバックされません。
   だから本サンプルの例外は全部 `RuntimeException` を継承しています。
3. **同一クラス内のメソッド呼び出しには効かない。**
   `this.otherMethod()` で呼ぶと `@Transactional` が無視されます
   （Springがプロキシ経由で介入できないため）。実務で踏みやすい罠です。

### 3-4. MyBatis

SQLを自分で書くタイプのO/Rマッパーです。日本のSI・官公庁案件では圧倒的に多く使われます。

**インターフェースとXMLの対応**

```java
// domain/repository/ApplicationMapper.java（実装クラスは書かない）
public interface ApplicationMapper {
    Optional<Application> findById(@Param("id") Long id);
}
```

```xml
<!-- resources/mapper/ApplicationMapper.xml -->
<mapper namespace="jp.go.sample.backend.domain.repository.ApplicationMapper">
                 <!-- ↑ インターフェースの完全修飾名と一致させる -->
  <select id="findById" resultMap="applicationResultMap">
       <!-- ↑ メソッド名と一致させる -->
    SELECT ... WHERE a.id = #{id}
  </select>
</mapper>
```

**`#{}` と `${}` の違い（最重要）**

| 記法 | 展開のされ方 | 用途 |
|---|---|---|
| `#{value}` | PreparedStatement の `?` になり、値は安全にバインドされる | **原則これを使う** |
| `${value}` | SQL文字列にそのまま結合される | カラム名、ASC/DESC など「値でないもの」限定 |

`${}` は SQLインジェクションの入口になります。本サンプルでは ORDER BY のカラム名に
だけ使い、`ApplicationSearchCriteria#getSortColumn()` の `switch` 文で
**許可リスト方式**にして安全性を担保しています。コードレビューで必ず見られる箇所です。

**動的SQL**（MyBatis を使う最大の理由）

```xml
<where>                                 <!-- 中身が空ならWHERE句自体を出力しない -->
  <if test="criteria.keyword != null and criteria.keyword != ''">
    AND (a.application_no LIKE CONCAT('%', #{criteria.keyword}, '%') OR ...)
  </if>
  <if test="criteria.status != null">
    AND a.status = #{criteria.status}
  </if>
</where>
```

`<where>` は先頭の余計な `AND` を自動で取り除いてくれます。
他に `<foreach>`（IN句の展開）、`<choose>/<when>/<otherwise>`（switch相当）、
`<trim>`、`<set>`（UPDATE の SET 句）があります。

**よく使うその他の記法**

| 記法 | 用途 |
|---|---|
| `<sql id="..."> / <include refid="...">` | SQL断片の再利用。一覧用と件数用でWHERE句を共有できる |
| `<resultMap>` | 列 → プロパティの対応を明示 |
| `useGeneratedKeys="true" keyProperty="id"` | INSERT後の自動採番値を引数オブジェクトに書き戻す |

### 3-5. Bean Validation（入力検証）

```java
public class ApplicationCreateRequest {
    @NotBlank(message = "申請者氏名は必須です")
    @Size(max = 100, message = "申請者氏名は100文字以内で入力してください")
    private String applicantName;
}
```

```java
// コントローラ側で @Valid を付けて初めて発火する
public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationCreateRequest request)
```

| アノテーション | 検証内容 |
|---|---|
| `@NotNull` | null でない |
| `@NotBlank` | 文字列が null・空・空白のみでない（**文字列には基本こちら**） |
| `@NotEmpty` | コレクション/文字列が空でない（空白のみは通ってしまう） |
| `@Size(max=, min=)` | 文字数・要素数 |
| `@Email` | メール形式 |
| `@Min` / `@Max` | 数値範囲 |
| `@PastOrPresent` / `@Future` | 日付 |
| `@Pattern(regexp=)` | 正規表現 |

違反時は `MethodArgumentNotValidException` が投げられ、`GlobalExceptionHandler` が
項目別メッセージ付きの 400 レスポンスに変換します。

> **画面側のバリデーションだけでは絶対に不十分です。**
> ブラウザの開発ツールやAPIの直叩きで簡単に回避できるため、**サーバ側の検証が本番**です。
> 画面側は「利用者に早くフィードバックする」ためのもの、と理解してください。

### 3-6. 例外処理の設計

本サンプルの考え方です。実案件でもこの形が多いはずです。

```
業務ロジックの中で例外を投げる
   ↓
RuntimeException を継承した自作例外（BusinessException / ResourceNotFoundException ...）
   ↓
@RestControllerAdvice の @ExceptionHandler が捕まえる
   ↓
HTTPステータス + 共通フォーマットのエラーJSON に変換
```

**設計上のポイント**

- **コントローラに try-catch を書かない。** 例外は投げっぱなしにして一箇所で処理する
- 業務エラー（4xx）は `warn` ログ ＋ 利用者向けメッセージをそのまま返す
- 想定外エラー（5xx）は `error` ログにスタックトレースを出し、
  **レスポンスには内部情報を出さない**（情報漏えい対策。`application.yml` の
  `server.error.include-stacktrace: never` も同じ意図）
- `ErrorCode` enum でエラーコードを一元管理する。
  官公庁案件では「エラーコード一覧」が設計書として存在し、
  画面表示・ログ・問い合わせ対応がこのコードで紐づきます

### 3-7. テストの3パターン

```java
@SpringBootTest     // アプリ全体を起動。DB込みで検証（SQLの確認に最適）
@Transactional      // テストごとにロールバックされるのでデータが汚れない
class ApplicationServiceIntegrationTest { ... }

@WebMvcTest(ApplicationController.class)   // Web層だけ起動。高速
class ApplicationControllerTest {
    @MockitoBean private ApplicationService applicationService;   // サービスをモック化
    @Autowired private MockMvc mockMvc;                           // HTTPを模擬実行
}
```

| 種類 | 範囲 | 速度 | 向いている検証 |
|---|---|---|---|
| 素のJUnit（`new` して呼ぶ） | クラス単体 | 最速 | 計算ロジック、enumの遷移ルール |
| `@WebMvcTest` | Web層のみ | 速い | HTTPステータス、JSON構造、バリデーション |
| `@SpringBootTest` | 全体 | 遅い | SQL、トランザクション、業務フロー全体 |

> `@MockBean` は Spring Boot 3.4 で非推奨になり `@MockitoBean` に変わりました。
> 既存案件のコードには `@MockBean` が大量にあるはずですが、どちらも役割は同じです。

---

## 4. Angular 編

### 4-1. 用語の対応表（Java経験者向け）

| Angular | 役割 | Spring の近いもの |
|---|---|---|
| Component | 画面・部品（クラス + HTML + CSS の3点セット） | （JSPやThymeleafのテンプレート + Controller） |
| Service | ロジック・API通信。`@Injectable` でDI | `@Service` |
| `inject()` / コンストラクタ注入 | 依存性の注入 | コンストラクタインジェクション |
| HTTP Interceptor | 全通信の共通処理 | `Filter` / `HandlerInterceptor` |
| Route Guard | 画面遷移前のチェック | Spring Security のフィルタ |
| Pipe | 表示用の値変換（`\| date`） | JSPのフォーマッタ |
| Directive | DOMに振る舞いを付与（`routerLink` 等） | （カスタムタグ） |
| Module（旧方式） | 部品のまとまり | （パッケージ） |

### 4-2. 起動の流れ

```
index.html の <app-root></app-root>
   ↑ ここに差し込まれる
main.ts : bootstrapApplication(App, appConfig)
   │
   ├── appConfig (app.config.ts) … DIプロバイダの登録
   │      provideRouter(routes, withComponentInputBinding())
   │      provideHttpClient(withInterceptors([...]))
   │
   └── App (app.ts / app.html) … 外枠。ヘッダー・ナビ・<router-outlet />
          │
          └── <router-outlet /> に、URLに対応する画面が差し込まれる
                 app.routes.ts の定義に従う
```

### 4-3. Component の3点セット

```
application-list.ts    … クラス（状態とロジック）
application-list.html  … テンプレート（見た目）
application-list.scss  … スタイル（このコンポーネント内だけに効く）
```

```typescript
@Component({
  selector: 'app-application-list',           // HTMLタグ名
  imports: [ReactiveFormsModule, RouterLink], // テンプレートで使う部品を列挙（standalone）
  templateUrl: './application-list.html',
  styleUrl: './application-list.scss',
})
export class ApplicationList { ... }
```

**スタイルのスコープ**: コンポーネントの `.scss` に書いたCSSは、ビルド時に
`_ngcontent-xxx` のような属性が付与され、**そのコンポーネントの中にしか効きません**。
他画面を壊す心配がなく、クラス名の衝突も起きにくいのが利点です。
全画面共通のものだけ `src/styles.scss` に書きます。

### 4-4. テンプレート構文（これだけ覚えれば読める）

| 構文 | 意味 | 例 |
|---|---|---|
| `{{ expr }}` | 値を埋め込む | `{{ application.applicantName }}` |
| `[prop]="expr"` | プロパティに値を渡す（親→子、DOM） | `[status]="application.status"` |
| `(event)="handler()"` | イベントを受け取る（DOM→クラス） | `(click)="onDelete(app)"` |
| `[(ngModel)]="x"` | 双方向バインド（Template-driven Formsで使用） | — |
| `\| pipe` | 表示形式を変換 | `{{ appliedOn \| date: 'yyyy/MM/dd' }}` |
| `@if (cond) { } @else { }` | 条件分岐 | |
| `@for (x of list; track x.id) { }` | 繰り返し（`track` 必須） | |
| `@if (expr; as alias) { }` | 値が真なら別名で使う（null回避に便利） | `@if (application(); as app)` |
| `routerLink` | 画面遷移（ページ再読み込みなし） | `[routerLink]="['/applications', app.id]"` |

**旧構文との対応**（既存案件では旧構文が大半です）

| 新（Angular 17+） | 旧 | 備考 |
|---|---|---|
| `@if` / `@else if` / `@else` | `*ngIf` / `ng-template` + `[ngIfElse]` | 旧は else が書きづらかった |
| `@for (x of list; track x.id)` | `*ngFor="let x of list; trackBy: fn"` | 新は `track` が必須 |
| `@switch` / `@case` | `[ngSwitch]` / `*ngSwitchCase` | |
| `imports: [...]`（standalone） | `@NgModule({ declarations, imports })` | |
| 不要 | `CommonModule` を import | `@if`/`@for` は import 不要 |

**旧構文も引き続き動作します。** 読めるようにしておけば十分です。

### 4-5. RxJS / Observable（ここが最大の壁）

```typescript
// これだけでは「通信は発生しない」
const request = this.applicationService.search(params);

// subscribe した瞬間に初めてリクエストが飛ぶ
request.subscribe({
  next: (result) => this.page.set(result),    // 成功時
  error: (err) => { ... },                    // 失敗時
});
```

**Promise との違い**

| | Promise | Observable |
|---|---|---|
| 実行タイミング | 作った瞬間に実行される | **subscribe するまで実行されない** |
| 値の数 | 1回だけ | 何回でも流れる（WebSocket等） |
| キャンセル | できない | `unsubscribe()` でできる |

**このサンプルで使っているRxJSの演算子**

| 演算子 | 用途 | 使用箇所 |
|---|---|---|
| `takeUntilDestroyed(destroyRef)` | コンポーネント破棄時に自動で購読解除 | 全コンポーネント |
| `finalize(fn)` | 成功・失敗・解除いずれでも必ず実行 | `loading.interceptor` |
| `catchError(fn)` | エラーを捕まえる | `error.interceptor` |
| `throwError(() => err)` | エラーを再送出する | 同上 |
| `shareReplay({bufferSize:1})` | 結果をキャッシュして通信を1回に抑える | `master.service`（マスタ取得） |

> **メモリリークに注意。**
> `subscribe()` を放置すると、画面を閉じた後も処理が走り続けます。
> 本サンプルは全ての購読に `takeUntilDestroyed()` を付けています。
> 古い案件では `ngOnDestroy` で `subscription.unsubscribe()` する書き方か、
> テンプレートで `| async` パイプを使う書き方になっているはずです。

### 4-6. Signal（シグナル）

Angular 16 で導入された、新しい状態管理の仕組みです。

```typescript
// 作る
protected readonly page = signal<PageResponse<Application>>(emptyPage());

// 読む（関数呼び出しの形になる）
this.page().content

// 書く
this.page.set(newValue);
this.page.update(current => [...current, item]);   // 現在値から新しい値を作る

// 導出する（依存する値が変わったときだけ再計算される）
protected readonly rangeTo = computed(() => Math.min((this.page() + 1) * this.size(), ...));
```

テンプレートで `page()` を参照しておくと、値が変わった箇所だけAngularが再描画します。

| | Signal | Observable |
|---|---|---|
| 向いているもの | 「現在の値」を持つ画面状態 | 時間とともに流れてくるイベント・HTTP通信 |
| 本サンプルでの使い分け | 一覧データ、プルダウン、ローディング状態 | HTTP通信、ルートパラメータの監視 |

**input / output もシグナル化されています**

```typescript
readonly status = input.required<ApplicationStatusCode>();  // 親→子（旧 @Input()）
readonly pageChange = output<number>();                     // 子→親（旧 @Output() EventEmitter）
```

旧構文（`@Input() status!: string;`）も動きますが、新しい `input()` は
「未設定を型で防げる（`required`）」「`computed` と自然に組み合わせられる」のが利点です。

### 4-7. Reactive Forms

業務システムではほぼこちらが使われます（Template-driven Forms より型が効くため）。

```typescript
protected readonly form = this.formBuilder.group({
  applicantName: ['', [Validators.required, Validators.maxLength(100)]],
  applicantEmail: ['', [Validators.required, Validators.email]],
  appliedOn: ['', [Validators.required, notFutureDate()]],   // 自作バリデータ
});
```

```html
<form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
  <input formControlName="applicantName" />
</form>
```

**よく使うAPI**

| API | 用途 |
|---|---|
| `form.get('name')` | 個別のコントロールを取得 |
| `form.value` / `form.getRawValue()` | 値を取得（`getRawValue` は disabled 項目も含む） |
| `form.patchValue({...})` | 一部の項目だけ設定（**編集画面での読み込みはこれ**） |
| `form.setValue({...})` | 全項目を設定（1つ足りないとエラー） |
| `form.valid` / `form.invalid` | 検証結果 |
| `form.markAllAsTouched()` | 全項目を「触った」扱いにしてエラーを表示させる |
| `form.reset()` | 初期化 |

**Angular が自動で付けるCSSクラス**（`styles.scss` で活用しています）

| クラス | 意味 |
|---|---|
| `ng-pristine` / `ng-dirty` | 値が未変更 / 変更済み |
| `ng-untouched` / `ng-touched` | フォーカスを当てていない / 一度外した |
| `ng-valid` / `ng-invalid` | 検証OK / NG |

`.ng-touched.ng-invalid` に赤枠を当てることで「触ったのに不正な項目」だけを
強調できます。画面を開いた瞬間に全項目が赤くなるのを防ぐ定石です。

### 4-8. ルーティング

```typescript
export const routes: Routes = [
  { path: '', redirectTo: 'applications', pathMatch: 'full' },
  { path: 'applications', loadComponent: () => import('...').then(m => m.ApplicationList) },
  { path: 'applications/new',  loadComponent: ... },   // ← :id より前に書く
  { path: 'applications/:id',  loadComponent: ... },
  { path: '**', loadComponent: ... },                  // ← 必ず最後
];
```

**押さえるべき3点**

1. **定義順が意味を持つ。** 上から照合して最初に一致したものが使われるので、
   `applications/new` を `applications/:id` より前に置かないと
   `:id` に `"new"` が入ってしまいます
2. **`loadComponent` による遅延読み込み。** 初回アクセス時に初めてダウンロードされます。
   画面数の多い大規模システムでは必須（`ng build` の出力で画面ごとにチャンクが
   分かれていることを確認できます）
3. **`'**'` は必ず最後。** どのルートにも一致しなかった場合の受け皿です

**ルートパラメータ（`:id`）の受け取り方は2通り**

```typescript
// 新しい方式（app.config.ts で withComponentInputBinding() が必要）
readonly id = input.required<string>();

// 従来の方式（既存案件はほぼこちら）
private readonly route = inject(ActivatedRoute);
const id = this.route.snapshot.paramMap.get('id');
```

### 4-9. HTTPインターセプター

全通信が必ず通る共通処理です。Spring の `Filter` と同じ位置づけです。

```typescript
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);
  loadingService.start();
  return next(req).pipe(finalize(() => loadingService.stop()));
};
```

```typescript
// app.config.ts  ※ 配列の順番どおりにリクエストが通る
provideHttpClient(withInterceptors([loadingInterceptor, errorInterceptor]))
```

実案件では主にこの用途で使われます。

- 認証トークン（JWT等）をヘッダーに付与する
- 401が返ったらログイン画面へ飛ばす
- エラーの共通通知
- ローディング表示
- リクエスト/レスポンスのログ出力

> **「共通で出すエラー」と「画面ごとに出すエラー」の線引きを最初に決めること。**
> 本サンプルでは 400（バリデーション）だけをインターセプターの対象外にし、
> 項目別メッセージを各入力欄の下に出す役割をフォームコンポーネントに持たせています。
> ここを決めずに作ると、同じエラーが二重表示される実装が各所に生まれます。

---

## 5. 官公庁案件で特に問われる観点

実装スキルとは別に、**この種の案件で必ず論点になる**ことです。
設計レビューや仕様確認の場で話についていくために押さえておいてください。

### 5-1. 排他制御

- **楽観ロック**（本サンプルの実装）… `version` 列をWHERE条件に入れ、0件更新を競合と判定
- **悲観ロック** … `SELECT ... FOR UPDATE` で行をロックしてから更新
- 「同じ申請を2人の職員が同時に開いたらどうなるか」は必ず確認されます

### 5-2. 監査証跡（ログ）

- 「いつ・誰が・どのデータを・どう変えたか」を後から追跡できること
- 本サンプルの `RequestLoggingFilter` + MDC によるリクエストIDはその最小形
- 実案件では**更新履歴テーブル**を持ち、変更前後の値を残すことが多い
- **物理削除ではなく論理削除**（削除フラグ）が選ばれるのも、記録を残すためです

### 5-3. ウェブアクセシビリティ（JIS X 8341-3）

官公庁のシステムは準拠が求められることが多く、**実装に直接影響します**。

本サンプルで入れている配慮:

| 実装 | 目的 |
|---|---|
| `<html lang="ja">` | スクリーンリーダーが日本語として読み上げる |
| 「本文へスキップ」リンク | キーボード利用者がナビを読み飛ばせる |
| `<label for="...">` と `id` の対応 | 入力欄とラベルの関連付け |
| `:focus-visible` で明確なフォーカス表示 | `outline: none` だけの実装は不具合扱いになる |
| `role="alert"` / `aria-live` | エラーや通知を動的に読み上げさせる |
| `<table>` の `<caption>` と `scope="col"` | 表の構造を伝える |
| 色だけに頼らない表現（ステータスは文字も出す） | 色覚特性への配慮 |
| `aria-current="page"` | 現在のページを示す |

### 5-4. 性能

- データ量が多くなりがち（数十万〜数百万件）
- **一覧検索は必ずページングする。** 全件取得は本番で破綻します
- 検索条件になる列にインデックスを張る（`schema.sql` 参照）
- `COUNT(*)` と明細取得でSQLが2本になるため、0件なら明細を取りに行かない等の工夫

### 5-5. 多階層の体制

- 元請け → 1次 → 2次という体制で、**規約・設計書が厳密**なことが多い
- 「コードの書き方」よりも**既存コードの流儀に合わせること**が重視される
- 新しい書き方を知っていても、まず既存のやり方に合わせるのが安全
- 設計書（画面定義書・項目定義書・エラーコード一覧・テーブル定義書）と
  コードの対応を意識すると早く馴染めます

### 5-6. その他よく出る要件

| 項目 | 内容 |
|---|---|
| 年度管理 | 年度切替で区分やマスタが変わる。「年度」を持つ設計が多い |
| 元号表示 | 「令和8年」のような和暦表示が要求されることがある |
| 文字コード | 外字・旧字体（𠮟・髙など）の扱い。氏名項目で問題になりやすい |
| 帳票 | PDF / Excel 出力。レイアウトが厳密に決まっている |
| ブラウザ対応 | 指定ブラウザ・バージョンが固定されていることがある |
| 画面サイズ | 1366×768 など、庁内端末の解像度に合わせる指定 |

---

## 6. 演習課題

手を動かすのが一番早いので、易しい順に並べました。
**課題1〜3をやれば、このサンプルの構造は体に入ります。**

### 課題1（易）: 一覧に「登録日時」列を追加する

- 触る場所: `application-list.html` のみ
- `createdAt` は既にAPIから返ってきています
- `| date: 'yyyy/MM/dd HH:mm'` で整形してください

### 課題2（易）: 1ページあたりの件数を画面から選べるようにする

- 触る場所: `application-list.ts` / `.html`
- 10 / 20 / 50 のプルダウンを検索条件に追加
- URLのクエリパラメータにも載せて、ブラウザバックで復元されるようにする
- サーバ側は `@Max(100)` があるので、それを超えないこと

### 課題3（中）: 申請に「電話番号」項目を追加する ★一番おすすめ

**端から端まで**を一通り触る課題です。修正箇所は以下の順になります。

```
1. schema.sql           … phone_number 列を追加
2. data.sql             … 初期データに値を入れる
3. Application.java     … フィールドを追加
4. ApplicationMapper.xml… resultMap / selectColumns / insert / update を修正
5. ApplicationCreateRequest / ApplicationUpdateRequest … @Pattern でバリデーション追加
6. ApplicationResponse  … record に項目を追加（from メソッドも）
7. ApplicationServiceImpl … create / update で値を詰める
8. application.model.ts … TypeScript の型に追加
9. application-form.html / .ts … 入力欄とバリデータを追加
10. application-detail.html    … 詳細表示に追加
11. テストを修正して通す
```

「1項目追加するだけで11ファイル触る」というのが業務システムの現実です。
この感覚を掴んでおくと、工数見積りの話が理解できるようになります。

> **PostgreSQL では `schema.sql` を変えても自動では反映されません。**
> `.\start-backend.ps1 -Init` で起動し直すと、テーブルが作り直されて列が追加されます
> （既存データは消えます）。
> 実案件では `ALTER TABLE application ADD COLUMN phone_number VARCHAR(20);` という
> マイグレーションSQLを1本追加する形になります（既存データを壊さないため）。
> このあたりを自動化するのが Flyway / Liquibase です。

### 課題4（中）: 「審査担当者」でも絞り込めるようにする

- 新しい検索条件をサーバ・クライアント両方に追加する
- `ApplicationMapper.xml` の `<if>` を1つ増やすだけでSQLが変わる動的SQLの威力を確認

### 課題5（中）: 一覧の列ヘッダークリックでソートする

- サーバ側は `sort` / `direction` パラメータに既に対応しています
- `ApplicationSearchCriteria#getSortColumn()` の許可リストを見て、
  **なぜ `${}` に許可リストが必要なのか**を理解してください

### 課題6（やや難）: ステータス変更時にコメントを必須にする

- `PATCH /api/applications/{id}/status` に `comment` を追加
- 「却下」に変更する場合のみ必須（条件付きバリデーション）
- 条件付き検証はアノテーションだけでは書けないため、
  サービス層で明示的に検証するか、クラスレベルのカスタムバリデータを作る
- **実案件で最も揉めるタイプの要件**です

### 課題7（難）: 申請履歴テーブルを追加する

- `application_history` テーブルを作り、ステータス変更のたびに1行追加する
- 詳細画面に履歴を時系列表示する
- 監査証跡の最小実装になります

---

## 7. つまずきポイント集

### データベース / Docker

| 症状 | 原因・対処 |
|---|---|
| `Connection to localhost:5432 refused` | コンテナが起動していない。`.\start-database.ps1` で起動する。Docker Desktop 自体が起動しているかも確認 |
| `relation "application" does not exist` | テーブルが未作成。`.\start-backend.ps1 -Init` で作成する |
| `password authentication failed for user "shinsei_app"` | ロールが作られていない。初期化SQLは**ボリュームが空のときだけ**実行されるため、途中で失敗していると残らない。`.\start-database.ps1 -Reset` で作り直す |
| `permission denied for schema public` | PostgreSQL 15 以降、`public` スキーマへの CREATE 権限が PUBLIC から外れた。`db\init\02_grant_schema.sql` の `GRANT USAGE, CREATE ON SCHEMA public` が効いているか確認 |
| `encoding "UTF8" does not match locale "Japanese_Japan.932"` | `CREATE DATABASE` が template1 のロケールを引き継いだため。`TEMPLATE template0` と `LC_COLLATE='C'` を明示する（`db\init\01_*.sql` 参照） |
| `db\init\*.sql` を直したのに反映されない | 初期化SQLは初回起動時のみ実行される。`docker compose down -v`（= `-Reset`）でボリュームごと作り直す |
| PostgreSQL 18 のコンテナが `unhealthy` で起動しない | 18 からデータのマウント先が `/var/lib/postgresql/data` → `/var/lib/postgresql` に変わった。`docker-compose.yml` のボリューム指定を確認 |
| ポート 5432 が使用中 | Windows にインストールした PostgreSQL サービスが起動している。停止するか、`$env:POSTGRES_PORT='5433'` + `$env:DB_PORT='5433'` で併用する |
| pgAdmin で日本語が文字化けする | DBのエンコーディングを確認（`SELECT pg_encoding_to_char(encoding) FROM pg_database WHERE datname='shinsei_db'` → UTF8 であること） |
| 日本語の ORDER BY が五十音順にならない | 照合順序 `C` はバイト順。漢字の読み順はどのDBでも不可能なので、実案件では「氏名カナ」列を別に持ってソートする |
| テストがDBに依存して落ちる | テストは `@ActiveProfiles("h2")` でH2を使う。本番構成のままテストするとDB未起動でCIが回らず、開発用DBのデータも壊す |

### バックエンド

| 症状 | 原因・対処 |
|---|---|
| `NoSuchBeanDefinitionException` | `@Service` 等が付いていない、またはスキャン対象外のパッケージにある |
| `Invalid bound statement (not found)` | MyBatis の XML と Mapper インターフェースの対応ずれ。`namespace` がFQCNと一致しているか、`<select id="">` がメソッド名と一致しているか、`mybatis.mapper-locations` にXMLが含まれているかを確認 |
| 値が null でマッピングされない | 列名とフィールド名の対応。`map-underscore-to-camel-case` が有効か、`AS` で別名を付けた場合はその名前と一致しているか |
| `@Transactional` が効かない | 同一クラス内で `this.method()` 経由で呼んでいる／`private` メソッドに付けている／検査例外を投げている |
| ロールバックされない | 検査例外（`Exception` 系）を投げている。`@Transactional(rollbackFor = Exception.class)` か非検査例外にする |
| 日本語が文字化けする | ソースのエンコーディング（`project.build.sourceEncoding`）／`spring.sql.init.encoding`／コンソールのコードページ（`chcp 65001`） |
| 起動時に `Table not found` | `schema.sql` の実行順。`spring.sql.init.mode` の設定を確認 |
| `@WebMvcTest` で DataSource エラー | `@MapperScan` を起動クラスに付けている。本サンプルのように専用の `@Configuration` に分離する |
| `Missing non-null annotation: inherited method from ... specifies this parameter as @NonNull` | IDEのnull解析。Springのメソッドをオーバーライドした引数に `@NonNull` を付ける（下記参照） |
| `Null type safety: ... needs unchecked conversion to conform to '@NonNull X'` | 同上。null許容性が未宣言の値を非null引数に渡している（下記参照） |

#### null解析（@NonNull / @Nullable）の警告について

Spring のパッケージには `@NonNullApi`（引数・戻り値は既定で非null）と
`@NonNullFields`（フィールドは既定で非null）が宣言されています。
VS Code の Java 拡張は `java.compile.nullAnalysis.mode` が `automatic` のとき
これを検出して **Eclipse JDT の null 解析** を自動で有効化します。
そのため **Maven ビルドでは警告ゼロなのに IDE だけ警告が出る**、という状態になります。

出る警告は3種類で、対処が異なります。

**1. `Missing non-null annotation`（親の契約を引き継いでいない）**

Spring のメソッドをオーバーライドしたとき、引数に `@NonNull` を付けていないと出ます。
「親は非nullを保証しているが、お前は緩めるのか明示せよ」という指摘です。付けて解決します。

```java
@Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,      // org.springframework.lang.NonNull
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
```

本サンプルでは `RequestLoggingFilter`（4箇所）と `WebMvcConfig`（1箇所）が該当しました。

> Spring Framework 7（Spring Boot 4）以降は独自アノテーションをやめ、
> 業界標準の **JSpecify**（`org.jspecify.annotations.NonNull` と
> パッケージ単位の `@NullMarked`）に移行しています。
> バージョンアップ時は import の差し替えが必要になります。

**2. `needs unchecked conversion to conform to '@NonNull X'`（null許容性が未宣言の値を渡した）**

自分のフィールドや変数に `@NonNull` が付いていない状態で、
Spring の非null引数に渡すと出ます。自分のコードが原因なら宣言すれば解決します。

```java
@NonNull private final String[] allowedOrigins;   // フィールドにも

public WebMvcConfig(@Value("${app.cors.allowed-origins}") @NonNull String[] allowedOrigins) {
    this.allowedOrigins = allowedOrigins;         // 引数にも付けないと代入側で警告が出る
}
```

一方、**JDKやサードパーティ（Jackson等）が返す値**は null許容性が未宣言なので、
自分では直せません。この場合は次のいずれかで対処します。

- 非nullが保証される書き方に変える（本サンプルで採った方法）
  - `URI.create("..." + id)` → `UriComponentsBuilder.fromPath("/api/applications/{id}")...toUri()`
    （`org.springframework.web.util` は `@NonNullApi` なので戻り値が非nullと分かる）
  - `MediaType.APPLICATION_JSON` → `MediaType.APPLICATION_JSON_VALUE`
    （`String` のコンパイル時定数なので常に非nullと判定される）
  - `objectMapper.writeValueAsString(dto)` → テキストブロックでJSONを直接書く
- `@SuppressWarnings("null")` でその箇所だけ抑止する
- IDE側の警告レベルを下げる／`java.compile.nullAnalysis.mode` を `disabled` にする

**3. `parameter 'this' ... needs unchecked conversion`（メソッド参照）**

`Enum::name` や `ApplicationResponse::status` のようなメソッド参照は、
レシーバのnull許容性が未宣言のため警告が出ます。**ラムダに書き換えると解消します**
（ラムダの引数型は関数型インターフェース側から確定するため）。

```java
.map(Enum::name)            // 警告が出る
.map(next -> next.name())   // 出ない
```

**どこまで対応すべきか**

1 は**契約の明示なので直す価値があります**（実案件でも規約に入っていることが多い）。
3 も書き換えコストが小さいので直してよいでしょう。
2 は「自分のコードが原因のもの」だけ直し、ライブラリ起因の誤検知は
抑止するか警告レベルを下げるのが現実的です。
コードベース全体を null 安全にするなら、自分のパッケージにも
`package-info.java` で `@NonNullApi` / `@NonNullFields` を宣言する方法がありますが、
null を許容する項目（本サンプルなら `note` など）すべてに `@Nullable` を
付ける作業が発生するため、途中から導入するのは現実的ではありません。

本サンプルは **IDE警告14件 → 0件** になるところまで対応済みです。
警告自体が不要なら `.vscode\settings.json` の
`java.compile.nullAnalysis.mode` を `"disabled"` にしてください。

### フロントエンド

| 症状 | 原因・対処 |
|---|---|
| `NullInjectorError: No provider for HttpClient` | `app.config.ts` に `provideHttpClient()` が無い。テストでは `provideHttpClient()` + `provideHttpClientTesting()` |
| テンプレートで `@if`/`@for` が効かない | Angular 16以前。`*ngIf`/`*ngFor` を使う |
| `*ngIf` が効かない | standalone で `CommonModule` を import していない |
| 子コンポーネントが表示されない | `@Component` の `imports` にそのコンポーネントを入れていない |
| APIを呼んでいるのに通信が飛ばない | `subscribe()` していない。Observable は購読するまで実行されない |
| CORS エラーが出る | プロキシ設定が効いていない。`angular.json` の `serve.options.proxyConfig` と、APIのURLが相対パス（`/api/...`）になっているかを確認 |
| 画面を閉じたのにエラーが出続ける | 購読解除漏れ。`takeUntilDestroyed()` を付ける |
| `form.setValue` でエラー | 全項目を渡していない。一部だけなら `patchValue` |
| `/applications/new` で詳細画面が出る | ルートの定義順。`:id` より前に書く |
| 本番ビルドだけ動かない | `environment.ts`（prod）の設定漏れ。`fileReplacements` の挙動を確認 |
| 画面をリロードすると404 | SPAのディープリンク。本番配信時はサーバ側で全リクエストを `index.html` に転送する設定が必要 |

---

## 8. 実案件が別構成だった場合の読み替え

### MyBatis ではなく Spring Data JPA だった場合

| MyBatis（本サンプル） | Spring Data JPA |
|---|---|
| `Application`（ただのクラス） | `@Entity @Table(name="application")` + `@Id` `@Column` |
| `ApplicationMapper` + XML | `interface ApplicationRepository extends JpaRepository<Application, Long>` |
| XMLにSQLを書く | メソッド名から自動生成（`findByApplicantNameContaining`）／`@Query` |
| 複雑な検索は動的SQL（`<if>`） | Specification / Criteria API / QueryDSL |
| `version` 列を手動でWHEREに入れる | `@Version` を付けるだけで自動（`OptimisticLockException`） |
| 更新は明示的に `update()` を呼ぶ | トランザクション内でエンティティを変更すれば自動反映（ダーティチェック） |

JPAは楽な一方、**発行されるSQLが見えにくい**（N+1問題、意図しないUPDATE）ため、
`spring.jpa.show-sql` や `hibernate.format_sql` でSQLを確認する習慣が重要になります。

### Maven ではなく Gradle だった場合

| Maven | Gradle |
|---|---|
| `pom.xml` | `build.gradle` / `build.gradle.kts` |
| `mvnw.cmd clean package` | `gradlew.bat clean build` |
| `mvnw.cmd spring-boot:run` | `gradlew.bat bootRun` |
| `mvnw.cmd test` | `gradlew.bat test` |
| `<dependency>` のXML | `implementation 'group:artifact:version'` |
| `target/` | `build/` |

考え方は同じです（依存関係の宣言 → ビルド → テスト → パッケージ）。

### Standalone ではなく NgModule ベースだった場合

Angular 14 以前の案件、または 15 以降でも従来構成を維持している案件です。

```typescript
// NgModule 方式
@NgModule({
  declarations: [AppComponent, ApplicationListComponent],  // 自分のコンポーネント
  imports: [BrowserModule, HttpClientModule, ReactiveFormsModule, AppRoutingModule],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }],
  bootstrap: [AppComponent],
})
export class AppModule {}
```

| Standalone（本サンプル） | NgModule |
|---|---|
| `@Component({ imports: [...] })` | `@NgModule({ declarations, imports })` |
| `bootstrapApplication(App, appConfig)` | `platformBrowserDynamic().bootstrapModule(AppModule)` |
| `provideHttpClient()` | `HttpClientModule` を imports |
| `provideRouter(routes)` | `RouterModule.forRoot(routes)`（`AppRoutingModule`） |
| `withInterceptors([fn])` | `HTTP_INTERCEPTORS` プロバイダ + クラス実装 |
| `loadComponent` | `loadChildren`（モジュール単位の遅延読み込み） |
| ファイル名 `application-list.ts` | `application-list.component.ts`（`.component` を付ける慣習） |
| クラス名 `ApplicationList` | `ApplicationListComponent` |

> ファイル名・クラス名の慣習は Angular 20 の CLI から変わりました
> （`.component` サフィックスが付かなくなった）。
> **既存案件では旧来の `*.component.ts` / `XxxComponent` が標準なので、
> そちらに合わせてください。**

### Angular のバージョンが 20 より古い場合

| 機能 | 導入バージョン |
|---|---|
| Standalone Components（安定版） | 15 |
| 関数型 HTTP インターセプター | 15 |
| Signals | 16 |
| `@if` / `@for` / `@switch` | 17 |
| `inject()` 関数 | 14 |
| `takeUntilDestroyed()` | 16 |
| signal input / output（`input()` / `output()`） | 17.1 / 17.3 |
| `withComponentInputBinding()` | 16 |

実案件のバージョンを確認して（`frontend/package.json` の `@angular/core`）、
使えない機能があれば上の対応表で旧い書き方に読み替えてください。
