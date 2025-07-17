# HTTP Cache 実装仕様

## 目的

HTTP/1.1の`Cache-Control`,`Expires`,`ETag`,`Last-Modified`などに準拠したクライアント側のキャッシュ機構を実装する。

## キャッシュ設計

- 保存先：ディスク
- 保存形式：
    - `.body`: HTTPレスポンスのボディ
- 保存キー：URLを`SHA256`でハッシュ化