---
name: mapbox-android-sdk-guidelines
description: Mapbox Maps SDK for Android (v11) の実装ガイドラインおよび規則。WebView等の代替手段を一切排除し、Mapbox公式ネイティブAndroid SDK（`com.mapbox.extension:maps-compose` および `com.mapbox.maps:android`）を厳格に使用してJetpack Compose上にマップ・位置情報・アノテーション・スタイルを正しく実装する。ドキュメント参照先: https://docs.mapbox.com/android/ja/maps/guides/
---

# Mapbox Maps SDK for Android (v11) 実装スキル

このスキルは、Androidアプリにおける Mapbox Maps SDK for Android (v11) の公式かつ標準的な実装基準・ガイドラインを定めます。

## 核心原則（絶対に守るべき規則）

1. **完全公式ネイティブSDKの強制**:
   - WebViewやLeafletなどの代替手段・Web技術は**絶対に禁止**。
   - 必ずMapbox公式 Native Android SDK (`com.mapbox.maps:android`) および Jetpack Compose 拡張 (`com.mapbox.extension:maps-compose`) を使用する。

2. **リポジトリおよび依存関係の定義**:
   - `settings.gradle.kts`: `https://api.mapbox.com/downloads/v2/releases/maven` リポジトリを `BasicAuthentication` および `mapbox` ユーザー名、シークレットトークン (`sk...`) の認証情報付きで追加する。
   - `app/build.gradle.kts`:
     ```kotlin
     implementation("com.mapbox.maps:android:11.18.2")
     implementation("com.mapbox.extension:maps-compose:11.18.2")
     ```

3. **アクセストークンの管理**:
   - トークンはソースに直書きしない。`local.properties`（gitignore対象）に `MAPBOX_PUBLIC_TOKEN=pk...`（描画用）と `MAPBOX_DOWNLOADS_TOKEN=sk...`（SDKダウンロード用）を記載する（CIでは同名の環境変数）。
   - パブリックトークンは `app/build.gradle.kts` の `resValue("string", "mapbox_access_token", ...)` でビルド時に注入する。`res/values/mapbox_access_token.xml` にトークンを書いてはならない（gitignore対象）。

4. **Jetpack Composeでの標準実装パターン**:
   - 画面には Compose コンポーネント `MapboxMap` を直接使用する。
   - カメラ制御には `rememberMapViewportState` および `mapViewportState.transitionToFollowPuckState()` を使用する。
   - 位置情報表示・追従（Puck）には `MapEffect` を用いて以下のように設定する：
     ```kotlin
     MapEffect(Unit) { mapView ->
       mapView.location.updateSettings {
         enabled = true
         locationPuck = createDefault2DPuck(withBearing = true)
         puckBearingEnabled = true
         puckBearing = PuckBearing.COURSE
       }
       mapView.location.addOnIndicatorPositionChangedListener { point ->
         // 位置情報ハンドリング
       }
       mapViewportState.transitionToFollowPuckState()
     }
     ```

5. **更新頻度・表示精度の制御**:
   - 位置情報表示テキストを更新する際は、高頻度な画面ちらつきを防ぐため更新間隔（例: 10秒）のチェックを行う。
   - 緯度経度の数値は必要に応じて適切な精度（例: 小数点以下3位 `%.3f`）にフォーマットする。

## 参照ドキュメント
- [Mapbox Maps SDK for Android 公式ガイド (日本語)](https://docs.mapbox.com/android/ja/maps/guides/)
- [Mapbox Compose 拡張ドキュメント](https://docs.mapbox.com/android/maps/guides/styles/compose/)
