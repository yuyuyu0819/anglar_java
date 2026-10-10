import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** 存在しないURLにアクセスされたときに表示する画面 */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="page">
      <header class="page__header">
        <h1 class="page__title">ページが見つかりません</h1>
      </header>
      <p class="empty">
        お探しのページは存在しないか、移動した可能性があります。<br />
        <a class="link" routerLink="/applications">申請一覧へ戻る</a>
      </p>
    </section>
  `,
})
export class NotFound {}
