import { describe, expect, it } from 'vitest';
import { buildMenuInfoRequest, extractMenuRows, formatMenuRows } from './jump-menu.mjs';

describe('jump menu dump helpers', () => {
  it('extracts unique menu rows from nested menu payloads', () => {
    const rows = extractMenuRows({
      dsAllMenu: [
        {
          MENU_ID: 'M001',
          MENU_KEY: 'app/sucr/SucrLessnSbjctInq',
          MENU_NM: '개설강좌조회',
          PGM_ID: 'SucrLessnSbjctInq',
          TASK_AUTHRT_ID: 'AUTH_001',
          APP_CONTEXT: '/view',
          CALL_PAGE: 'app/sucr/SucrLessnSbjctInq.clx',
        },
        {
          MENU_ID: 'M002',
          MENU_KEY: 'app/sucr/SucrStdntAppcsLgn',
          MENU_NM: '수강신청',
          PGM_ID: 'SucrStdntAppcsLgn',
          TASK_AUTHRT_ID: 'AUTH_002',
          APP_CONTEXT: '/view',
        },
      ],
      dmMenuInfo: {
        MENU_ID: 'M002',
        MENU_KEY: 'app/sucr/SucrStdntAppcsLgn',
        MENU_NM: '수강신청',
        PGM_ID: 'SucrStdntAppcsLgn',
        TASK_AUTHRT_ID: 'AUTH_002',
        APP_CONTEXT: '/view',
      },
    });

    expect(rows).toEqual([
      {
        menuId: 'M001',
        menuKey: 'app/sucr/SucrLessnSbjctInq',
        menuName: '개설강좌조회',
        pageId: 'SucrLessnSbjctInq',
        taskAuthrtId: 'AUTH_001',
        appContext: '/view',
        callPage: 'app/sucr/SucrLessnSbjctInq.clx',
      },
      {
        menuId: 'M002',
        menuKey: 'app/sucr/SucrStdntAppcsLgn',
        menuName: '수강신청',
        pageId: 'SucrStdntAppcsLgn',
        taskAuthrtId: 'AUTH_002',
        appContext: '/view',
      },
    ]);
  });

  it('formats menu rows as a compact report', () => {
    const output = formatMenuRows([
      {
        menuId: 'M001',
        menuKey: 'app/sucr/SucrLessnSbjctInq',
        menuName: '개설강좌조회',
        pageId: 'SucrLessnSbjctInq',
        taskAuthrtId: 'AUTH_001',
        appContext: '/view',
      },
    ]);

    expect(output).toBe('M001 | app/sucr/SucrLessnSbjctInq | 개설강좌조회 | SucrLessnSbjctInq');
  });

  it('builds the getMenuInfo request body from a menu row', () => {
    const body = buildMenuInfoRequest({
      menuId: 'M001',
      menuKey: 'app/sucr/SucrLessnSbjctInq',
      menuName: '개설강좌조회',
      pageId: 'SucrLessnSbjctInq',
      taskAuthrtId: 'AUTH_001',
      appContext: '/view',
    });

    expect(Object.fromEntries(body.entries())).toEqual({
      _AUTH_MENU_KEY: 'app/sucr/SucrLessnSbjctInq',
      _AUTH_TASK_AUTHRT_ID: 'AUTH_001',
      _APP_CONTEXT: '/view',
    });
  });
});
