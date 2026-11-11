# Codex 대화 기록

저장소 루트에서 실행함. Python 3.10 이상이 필요하고, 압축 기록은 `zstd`로 읽음. 기본 데이터 경로는 `${CODEX_HOME:-~/.codex}`임.

| 용도 | 명령 |
| --- | --- |
| 현재 실행의 정확한 기록 확인 | `python3 .agents/pstack/transcripts/resolve.py current` |
| 지정한 스레드의 기록 확인 | `python3 .agents/pstack/transcripts/resolve.py current --thread <UUID>` |
| 같은 저장소의 활성 기록 목록 | `python3 .agents/pstack/transcripts/resolve.py list` |
| 대화의 일부 문구 검색 | `python3 .agents/pstack/transcripts/resolve.py search '<문구>' --limit 5` |
| 첫 사용자 메시지에서 평가 과제 찾기 | `python3 .agents/pstack/transcripts/resolve.py search '<평가 과제 문구>' --opening-only` |
| 지정 스레드의 대화 읽기 | `python3 .agents/pstack/transcripts/resolve.py read <UUID>` |
| 도구 호출이나 실제 실행 증거 확인 | `python3 .agents/pstack/transcripts/resolve.py read <UUID> --records` |
| worktree별 최근 활동 시각 확인 | `python3 .agents/pstack/transcripts/resolve.py --include-archived activity` |

`--workspace <경로>`로 같은 저장소의 다른 worktree에서 조회할 수 있음. `--include-archived`는 보관된 대화가 필요한 때만 명령 이름 앞에 추가함. `list`는 스레드 ID, 경로, worktree, 수정 시각, 부모 스레드 ID 등 메타데이터를 보여줌. 평가 과제를 고를 때 부모 스레드와 실제 호출 기록도 대조함.

현재 기록을 고를 때 Codex 상태 DB의 `threads.rollout_path`를 읽기 전용으로 조회하고, 경로와 헤더의 스레드 ID·작업 디렉터리를 확인함. 되돌리기 이전의 파일이 남아도 선택하지 않음. 인덱스가 없으면 단일 원본 후보만 사용함. 동일한 Git worktree 목록과 해당 경로 내부에서 실행한 대화로 범위를 제한함. `--records`는 도구 입출력까지 반환하므로 필요한 타입과 필드를 로컬에서 필터링함.

`activity`는 정규화한 worktree 경로를 Unix 초로 매핑함. 세션 헤더, 턴의 작업 디렉터리, 실행 도구에 명시한 작업 디렉터리를 활동 근거로 사용함. 해석할 수 없는 기록이 결과에 영향을 줄 수 있으면 오류를 반환함.

회귀 검증은 `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s .agents/pstack/transcripts -v`로 실행함.
