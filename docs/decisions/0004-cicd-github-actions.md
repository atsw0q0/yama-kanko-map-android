# 0004: CI/CD に GitHub Actions + OIDC + Terraform を採用する

- 日付：2026-11-19
- ステータス：提案

## 背景

Android アプリのビルド・テスト・配布と、AWS インフラ（Cognito / AppSync / DynamoDB / S3 / Lambda）のデプロイを自動化したい。
リポジトリは GitHub で管理している。

## 選択肢

1. **GitHub Actions**
   - メリット：リポジトリと統合されている。PR へのコメントや Environments による承認フローが標準で使える。Android 用のアクションが豊富。OIDC で AWS に、アクセスキーなしで認証できる。
   - デメリット：プライベートリポジトリでは無料の実行時間に上限がある。Android ビルドは時間がかかるので、キャッシュの工夫が必要。
2. **AWS CodePipeline / CodeBuild**
   - メリット：AWS 内で完結する。IAM と統合されている。
   - デメリット：GitHub との連携や PR へのフィードバックの設定に手間がかかる。Android SDK の環境を自分で用意する必要がある。
3. **Bitrise などのモバイル特化 CI**
   - メリット：Android・iOS のビルドや署名に強い。
   - デメリット：インフラのデプロイには別の仕組みが要る。無料枠が小さい。

IaC は Terraform を使う（全 PJ 共通で `terraform/` にモジュールを置く方針のため）。

## 決定

**GitHub Actions を採用し、AWS への認証は OIDC、インフラは Terraform で管理する。**

| トリガー | Android | インフラ |
|----------|---------|----------|
| PR | lint / unit test / debug ビルド | `terraform fmt`・`validate`・`plan` → 結果を PR にコメント |
| main へのマージ | dev flavor をビルドし、Firebase App Distribution で配布 | dev アカウントに `apply` |
| `v*` タグ | release AAB に署名し、Play 内部テストトラックへアップロード | prod アカウントに `apply`（GitHub Environments `prod` の承認が必要） |

- **アクセスキーは発行しない**。各アカウントに IAM OIDC プロバイダとデプロイ用ロールを作る。信頼ポリシーは `repo:<owner>/<repo>:ref:refs/heads/main` や `environment:prod` などに絞る。
- Android の署名は Play App Signing を使う。アップロード鍵は GitHub Secrets（Base64）に置く。
- 環境ごとの設定値（AppSync エンドポイント、User Pool ID、Client ID）は、`terraform output` から CI で生成して BuildConfig に注入する。
- Mapbox のトークンなどのシークレットは GitHub Secrets と SSM Parameter Store で管理する（参考：`docs/dev/android-secret-management.md`）。
- Gradle キャッシュ（`gradle/actions/setup-gradle`）でビルド時間を短縮する。

## 影響

- Android 側で productFlavors（`dev` / `prod`、dev は `applicationIdSuffix = ".dev"`）を追加し、`buildConfig = true` にする必要がある。
- prod へのデプロイには必ず人の承認が入る。承認者を決めておく。
- GitHub Actions の実行時間を監視し、無料枠を超えそうなら実行条件（paths フィルタなど）を絞る。
- Play Console への初回アップロードは手動で行う必要がある（API でのアップロードは 2 回目以降）。
