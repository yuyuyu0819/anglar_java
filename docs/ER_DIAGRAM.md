# ER図（申請管理システム）

`backend/src/main/resources/schema.sql` から起こした ER 図です。

```mermaid
erDiagram
    application_category ||--o{ application : "区分として使われる"
    application ||--o{ application_history : "ステータス変更履歴を持つ"

    application_category {
        VARCHAR(10)  code PK "区分コード（BLD, RSD, TAX ...）"
        VARCHAR(100) name "区分名"
        INT          display_order "表示順"
    }

    application {
        BIGINT        id PK "自動採番"
        VARCHAR(20)   application_no UK "申請番号（APP-2026-00001）"
        VARCHAR(100)  applicant_name "申請者氏名"
        VARCHAR(255)  applicant_email "申請者メール"
        VARCHAR(10)   category_code FK "→ application_category.code"
        VARCHAR(20)   status "ApplicationStatus（enum名）"
        DATE          applied_on "申請日"
        VARCHAR(1000) note "備考（NULL可）"
        INT           version "楽観ロック用"
        TIMESTAMP     created_at "登録日時"
        TIMESTAMP     updated_at "更新日時"
    }

    application_history {
        BIGINT      id PK "自動採番"
        BIGINT      application_id FK "→ application.id"
        VARCHAR(20) from_status "変更前（登録時はNULL）"
        VARCHAR(20) to_status "変更後"
        TIMESTAMP   changed_at "変更日時"
    }
```

## 補足

| 項目 | 内容 |
|---|---|
| カーディナリティ | 区分 1 : N 申請、申請 1 : N 履歴 |
| `status` | テーブルではなく Java の enum `ApplicationStatus` で管理（RECEIVED / REVIEWING / APPROVED / REJECTED / WITHDRAWN） |
| インデックス | `application` の `status`、`applied_on`、`applicant_name` |
| 一意制約 | `application.application_no` |

## ステータス遷移（ApplicationStatus.allowedNextStatuses）

```mermaid
stateDiagram-v2
    [*] --> RECEIVED: 申請登録
    RECEIVED --> REVIEWING: 審査開始
    RECEIVED --> WITHDRAWN: 取下げ
    REVIEWING --> APPROVED: 承認
    REVIEWING --> REJECTED: 却下
    REVIEWING --> WITHDRAWN: 取下げ
    APPROVED --> [*]
    REJECTED --> [*]
    WITHDRAWN --> [*]
```
