# 0001: 認証基盤に Amazon Cognito を採用する

- 日付：2026-11-19
- ステータス：承認

## 背景

現在、アプリのデータ（活動履歴・GPX・プロフィール）はすべて端末内に保存している。機種変更時の引き継ぎやクラウド同期を実現するには、ユーザー認証が必要になる。
要件は次のとおり。

- 小中規模（〜数千MAU）であり、**無料枠の範囲で運用したい**
- バックエンドは AWS（AppSync / DynamoDB / S3）で構築する予定（→ [0002](0002-api-appsync.md)）
- 体重・位置情報などのセンシティブなデータを扱う

## 選択肢

1. **Amazon Cognito（User Pool, Lite / Essentials）**
   - メリット：月1万MAUまで無料。超過分は MAU 単位の従量課金で、コストが緩やかに増える。AppSync・IAM・API Gateway とネイティブに統合できる。他の AWS リソースと同じ Terraform で管理できる。Google 連携（フェデレーション）にも対応。
   - デメリット：Managed Login の見た目のカスタマイズに限界がある。料金体系が改定されることがある。
2. **Auth0（Free プラン）**
   - メリット：無料枠が大きい（2.5万MAU程度）。Universal Login の完成度が高い。Android SDK が使いやすい。
   - デメリット：AWS の外にもう1社依存先が増える。無料プランには機能制限がある。有料プランへの移行時に、月額固定で費用が一気に上がる。端末から AWS リソースへ直接アクセスする場合は Identity Pool の併用が必要になる。
3. **Firebase Authentication**
   - メリット：基本機能は事実上無料で、実装が最も簡単。Crashlytics / FCM と同じ Firebase に相乗りできる。
   - デメリット：GCP への依存が増える。Terraform で管理しにくい。AWS 側では OIDC として個別に連携を設定する必要がある。

※ 料金・無料枠は 2026-11 時点の記憶に基づく。採用前に各社の公式ページで確認すること。

## 決定

**Amazon Cognito User Pool（Essentials ティア）を採用する。**

- 想定規模では無料枠に収まり、超過しても従量課金で緩やかに増えるだけ。
- AppSync の `AMAZON_COGNITO_USER_POOLS` 認可モードをそのまま使え、リゾルバーで `ctx.identity.sub` を扱える。
- インフラ全体を AWS + Terraform に統一し、運用対象を増やさない。

### 実装方針
- 認証フロー：Managed Login + Authorization Code + **PKCE**（Android 側は AppAuth を使う）。カスタムスキームでリダイレクトする（例：`yamakanko://callback`）。
- サインイン方式：まずメール + パスワード。Google ログインは Cognito のフェデレーションで後から追加できる（初期リリースに入れるかは未決）。
- 環境ごとに User Pool を分ける（`yama-dev` / `yama-prod`）。prod では削除保護を有効にする。
- トークンは Android Keystore で保護して保存する（DataStore + Tink 等）。
- PostConfirmation トリガー（Lambda）で初期プロフィールを作成する。

## 影響

- Android 側に認証画面・トークン管理・同期処理が加わる。未ログインでも使えるオフライン利用をどう扱うか（ゲストモード）を別途決める必要がある。
- アカウント削除（Google Play ポリシー）は、Cognito のユーザーと関連データをまとめて削除する仕組みで対応する（→ [0002](0002-api-appsync.md) の `deleteAccount`）。
- 月1万MAU を超えると課金が発生する。AWS Budgets でアラートを設定する。
- 将来、別の IdP へ移行する場合は、パスワードハッシュを引き継げないため再登録や移行用 Lambda が必要になる（ロックインのコスト）。
