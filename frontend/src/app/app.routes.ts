import { Routes } from '@angular/router';

/**
 * 画面とURLの対応表（ルーティング定義）。
 *
 * <h3>loadComponent による遅延読み込み（Lazy Loading）</h3>
 * `loadComponent: () => import('...')` と書くと、
 * そのコンポーネントは初回アクセス時に初めてダウンロードされる。
 * 画面数が多い大規模システムでは、これを使わないと初回表示が極端に遅くなる。
 *
 * `component:` で直接指定すると最初のバンドルに含まれる（即時読み込み）。
 * NgModule 時代は `loadChildren` でモジュール単位に分割していた。
 *
 * <h3>定義順が意味を持つ</h3>
 * ルーターは上から順に照合し、最初に一致したものを使う。
 * `applications/new` を `applications/:id` より<b>前</b>に書かないと、
 * `:id` に "new" という文字列が入ったものとして扱われてしまう。
 *
 * <h3>title</h3>
 * 指定しておくと遷移時にブラウザのタブ（document.title）が自動で切り替わる。
 * 利用者がどの画面にいるか分かりやすくなるため、実案件でも設定しておきたい。
 */
export const routes: Routes = [
  {
    // ルートURL（/）にアクセスされたら一覧へ転送する。
    // pathMatch: 'full' は「パス全体が空のときだけ一致」の指定
    path: '',
    redirectTo: 'applications',
    pathMatch: 'full',
  },
  {
    path: 'applications',
    title: '申請一覧 | 申請管理システム',
    loadComponent: () =>
      import('./features/applications/application-list/application-list').then(
        (m) => m.ApplicationList,
      ),
  },
  {
    // ':id' より前に置く必要がある
    path: 'applications/new',
    title: '申請登録 | 申請管理システム',
    loadComponent: () =>
      import('./features/applications/application-form/application-form').then(
        (m) => m.ApplicationForm,
      ),
  },
  {
    path: 'applications/:id',
    title: '申請詳細 | 申請管理システム',
    loadComponent: () =>
      import('./features/applications/application-detail/application-detail').then(
        (m) => m.ApplicationDetail,
      ),
  },
  {
    path: 'applications/:id/edit',
    title: '申請編集 | 申請管理システム',
    loadComponent: () =>
      import('./features/applications/application-form/application-form').then(
        (m) => m.ApplicationForm,
      ),
  },
  {
    // '**' はワイルドカード。どのルートにも一致しなかった場合に使われるので必ず最後に置く
    path: '**',
    title: 'ページが見つかりません | 申請管理システム',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
