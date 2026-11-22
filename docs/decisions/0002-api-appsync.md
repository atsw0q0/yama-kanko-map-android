# 0002: API に AWS AppSync（GraphQL）を採用する

- 日付：2026-11-19
- ステータス：承認

## 背景

端末内の活動履歴・プロフィール・GPX をクラウドと同期するための API が必要。
主な処理は、ユーザー単位の CRUD（活動・プロフィール）、日別の集計（Insights／カレンダー画面）、GPX ファイルの保存。
運用負荷を抑えるため、サーバーを持たない構成にしたい。

## 選択肢

1. **AWS AppSync（GraphQL）**
   - メリット：DynamoDB へのリゾルバー直結で CRUD に Lambda が不要。スキーマから型付きのクライアントコードを生成できる。Subscription でリアルタイム機能を追加できる。Cognito 認可がネイティブで使える。
   - デメリット：GraphQL とリゾルバー（APPSYNC_JS）の学習が必要。ファイル転送には使えない。リゾルバーのデバッグがやや難しい。
2. **API Gateway（HTTP API）+ Lambda（REST）**
   - メリット：構成がシンプルで、知見も多い。JWT オーソライザーで Cognito と連携できる。
   - デメリット：すべてのエンドポイントに Lambda のコードが必要で、保守対象が増える。型付きのクライアントコードは OpenAPI などで別途整備する必要がある。

## 決定

**AWS AppSync を採用する。**

- このアプリの API は大部分が「自分のデータの CRUD」なので、JS リゾルバーで DynamoDB に直結すればバックエンドのコード量を最小にできる。
- 将来、ルート共有などのリアルタイム機能を追加する余地を残せる。

### 実装方針
- 認可モード：`AMAZON_COGNITO_USER_POOLS`（→ [0001](0001-auth-cognito.md)）。
- **パーティションキーには必ず `ctx.identity.sub` を使う**。クライアントから userId を受け取らない。
- データストア：DynamoDB シングルテーブル（`PK=USER#{sub}`, `SK=PROFILE | ACT#... | DAY#...`）、オンデマンド課金。
- Lambda リゾルバーは次に限定する：
  - `getTrackUploadUrl` / `getTrackDownloadUrl`：S3 の署名付きURLを発行する（有効期限5分、`users/{sub}/` 配下のキーのみ）
  - `deleteAccount`：DynamoDB・S3・Cognito のユーザーを削除する
- 集計：DynamoDB Streams → Lambda で `DAY#` サマリーを更新する。
- GPX は GraphQL に載せず、署名付きURLで端末から S3 に直接 PUT / GET する。
- 同期の競合は `updatedAt` による last-write-wins + 論理削除（`deleted`）で解決する。
- Android クライアント：**Apollo Kotlin + AWS AppSync Apollo Extensions**。Amplify DataStore は採用しない（メンテナンスモード寄りのため）。オフライン対応は Room + WorkManager で自前実装する。
- スキーマ・リゾルバー・Lambda はすべて Terraform で管理する。

## 影響

- GraphQL スキーマ（`schema.graphql`）がアプリとバックエンドの契約になる。スキーマの変更は PR でレビューし、後方互換を保つ（フィールドは追加のみ、削除は段階的に行う）。
- AppSync の無料枠は新規アカウント向けのクレジット方式に変わっているため、従量課金を前提にコストを見積もる。
- prod では WAF をつけてレート制限を行う。
- 複雑な検索や集計が必要になった場合は、Lambda リゾルバーや別のデータストア（OpenSearch 等）の追加を検討する。
